import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.*;

/** Read-only, conservative symbol inventory. Compile with JDK 25; no third-party parser needed. */
public final class CodeLifecycleAudit {
    private static final class Declaration {
        Element element;
        Tree tree;
        String file, symbol, owner, kind, scope = "";
        long start, end, line;
        boolean test, entry, override, deprecated;
        int production, tests, reads, writes, otherFiles;
        final Set<String> referringFiles = new TreeSet<>();
        final List<String> writeLocations = new ArrayList<>();
    }

    private final Path root, output;
    private final Trees trees;
    private final Types types;
    private final Elements elements;
    private final Map<Element, Declaration> declarations = new LinkedHashMap<>();
    private final Map<String, Set<String>> typeEdges = new TreeMap<>();
    private final Set<String> entryTypes = new TreeSet<>();
    private final Set<String> reachable = new HashSet<>();
    private CompilationUnitTree unit;

    private CodeLifecycleAudit(Path root, Path output, JavacTask task) {
        this.root = root;
        this.output = output;
        trees = Trees.instance(task);
        types = task.getTypes();
        elements = task.getElements();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) throw new IllegalArgumentException("Usage: CodeLifecycleAudit projectRoot outputDirectory externalRootsFile [dependencyProjectRoot]");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(output);
        List<Path> sources;
        try (var files = Files.walk(root.resolve("src"))) {
            sources = files.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        // javac's API needs explicit JAR paths, unlike the command-line wildcard expansion.
        String classpath;
        Path dependencyRoot = args.length == 4 ? Path.of(args[3]).toAbsolutePath().normalize() : root;
        try (var jars = Files.list(dependencyRoot.resolve("dependency-egps"))) {
            classpath = jars.filter(p -> p.toString().endsWith(".jar")).sorted()
                    .map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK is required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (var manager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "-encoding", "UTF-8", "-classpath", classpath), null,
                    manager.getJavaFileObjectsFromPaths(sources));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            task.analyze();
            Files.writeString(output.resolve("compiler-diagnostics.txt"), diagnostics.getDiagnostics().stream()
                    .map(Object::toString).collect(Collectors.joining("\n")), StandardCharsets.UTF_8);
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IllegalStateException("Source attribution failed; see compiler-diagnostics.txt");
            }
            CodeLifecycleAudit audit = new CodeLifecycleAudit(root, output, task);
            for (CompilationUnitTree unit : units) { audit.unit = unit; audit.collectDeclarations(); }
            for (CompilationUnitTree unit : units) { audit.unit = unit; audit.collectReferences(); }
            for (String token : Files.readAllLines(Path.of(args[2]), StandardCharsets.UTF_8)) {
                for (String type : audit.typeEdges.keySet()) {
                    if (token.equals(type) || token.startsWith(type + ".")) audit.entryTypes.add(type);
                }
            }
            Deque<String> queue = new ArrayDeque<>(audit.entryTypes);
            while (!queue.isEmpty()) {
                String type = queue.removeFirst();
                if (audit.reachable.add(type)) queue.addAll(audit.typeEdges.getOrDefault(type, Set.of()));
            }
            audit.writeReports(sources.size());
            List<String> hashes = new ArrayList<>(List.of("file\tsha256"));
            for (Path source : sources) hashes.add(root.relativize(source).toString().replace('\\', '/') + "\t"
                    + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source))));
            Files.write(output.resolve("source-hashes.tsv"), hashes, StandardCharsets.UTF_8);
        }
    }

    private String file() { return root.relativize(Path.of(unit.getSourceFile().toUri())).toString().replace('\\', '/'); }
    private static TypeElement ownerType(Element element) {
        for (Element current = element; current != null; current = current.getEnclosingElement()) {
            if (current instanceof TypeElement type) return type;
        }
        return null;
    }
    private String typeName(Element element) {
        TypeElement type = ownerType(element);
        if (type == null) return "";
        return type.getNestingKind() == NestingKind.ANONYMOUS || type.getNestingKind() == NestingKind.LOCAL
                ? elements.getBinaryName(type).toString() : type.getQualifiedName().toString();
    }
    private boolean subtype(TypeElement type, String contract) {
        TypeElement target = elements.getTypeElement(contract);
        return target != null && types.isSubtype(types.erasure(type.asType()), types.erasure(target.asType()));
    }
    private boolean moduleEntry(TypeElement type) {
        return !type.getModifiers().contains(Modifier.ABSTRACT) && subtype(type, "egps2.modulei.IModuleLoader")
                && !subtype(type, "egps2.builtin.modules.voice.fastmodvoice.DockableTabModuleFaceOfVoice")
                && !subtype(type, "egps2.plugin.fastmodtem.FastBaseTemplate");
    }
    private boolean overrides(ExecutableElement method) {
        if (method.getKind() != ElementKind.METHOD || method.getModifiers().contains(Modifier.PRIVATE)) return false;
        if (method.getAnnotation(Override.class) != null) return true;
        TypeElement owner = ownerType(method);
        for (var supertype : types.directSupertypes(owner.asType())) {
            Element parent = types.asElement(supertype);
            if (parent instanceof TypeElement type) {
                for (Element member : elements.getAllMembers(type)) {
                    if (member instanceof ExecutableElement candidate && elements.overrides(method, candidate, owner)) return true;
                }
            }
        }
        return false;
    }

    private void collectDeclarations() {
        new TreePathScanner<Void, Void>() {
            private void add(Tree tree) {
                Element element = trees.getElement(getCurrentPath());
                if (element == null) return;
                long start = trees.getSourcePositions().getStartPosition(unit, tree);
                long end = trees.getSourcePositions().getEndPosition(unit, tree);
                if (start < 0 || end < 0) return; // javac's generated constructors and synthetic nodes
                Declaration declaration = new Declaration();
                declaration.element = element; declaration.tree = tree;
                declaration.start = start; declaration.end = end;
                declaration.line = unit.getLineMap().getLineNumber(start);
                declaration.file = file(); declaration.test = declaration.file.startsWith("src/test/");
                declaration.kind = element.getKind().name(); declaration.owner = typeName(element);
                declaration.symbol = declaration.owner + "#" + element;
                if (element instanceof VariableElement) {
                    for (Element enclosing = element.getEnclosingElement(); enclosing != null; enclosing = enclosing.getEnclosingElement()) {
                        if (enclosing instanceof ExecutableElement method) {
                            declaration.scope = typeName(method) + "#" + method;
                            break;
                        }
                        if (enclosing instanceof TypeElement) break;
                    }
                }
                declaration.deprecated = elements.isDeprecated(element);
                if (element instanceof TypeElement type) {
                    typeEdges.computeIfAbsent(declaration.owner, ignored -> new TreeSet<>());
                    declaration.entry = !declaration.test && (moduleEntry(type) || declaration.file.startsWith("src/api/"));
                }
                if (element instanceof ExecutableElement method) {
                    declaration.override = overrides(method);
                    declaration.entry = !declaration.test && method.getSimpleName().contentEquals("main")
                            && method.getModifiers().contains(Modifier.STATIC);
                }
                if (declaration.entry) entryTypes.add(declaration.owner);
                declarations.put(element, declaration);
            }
            @Override public Void visitClass(ClassTree tree, Void unused) { add(tree); return super.visitClass(tree, unused); }
            @Override public Void visitMethod(MethodTree tree, Void unused) { add(tree); return super.visitMethod(tree, unused); }
            @Override public Void visitVariable(VariableTree tree, Void unused) { add(tree); return super.visitVariable(tree, unused); }
        }.scan(unit, null);
    }

    private void collectReferences() {
        String referringFile = file();
        boolean test = referringFile.startsWith("src/test/");
        new TreePathScanner<Void, Void>() {
            private void reference(Tree tree) {
                Element element = trees.getElement(getCurrentPath());
                Declaration declaration = declarations.get(element);
                if (declaration != null) {
                    if (test) declaration.tests++; else declaration.production++;
                    declaration.referringFiles.add(referringFile);
                    if (!referringFile.equals(declaration.file)) declaration.otherFiles++;
                    Tree parent = getCurrentPath().getParentPath().getLeaf();
                    boolean write = parent instanceof AssignmentTree assignment && assignment.getVariable() == tree;
                    boolean update = parent instanceof CompoundAssignmentTree assignment && assignment.getVariable() == tree;
                    if (parent instanceof UnaryTree unary) {
                        update = switch (unary.getKind()) {
                            case PREFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_INCREMENT, POSTFIX_DECREMENT -> true;
                            default -> update;
                        };
                    }
                    if (!write || update) declaration.reads++;
                    if (write || update) {
                        declaration.writes++;
                        long start = trees.getSourcePositions().getStartPosition(unit, parent);
                        long end = trees.getSourcePositions().getEndPosition(unit, parent);
                        declaration.writeLocations.add(referringFile + ":" + unit.getLineMap().getLineNumber(start) + ":" + start + ":" + end);
                    }
                }
                if (!test && element != null) {
                    String target = typeName(element);
                    for (TreePath path = getCurrentPath(); path != null; path = path.getParentPath()) {
                        if (path.getLeaf() instanceof ClassTree) {
                            String source = typeName(trees.getElement(path));
                            if (!source.isEmpty() && !target.isEmpty() && !source.equals(target)) {
                                typeEdges.computeIfAbsent(source, ignored -> new TreeSet<>()).add(target);
                            }
                            break;
                        }
                    }
                }
            }
            @Override public Void visitIdentifier(IdentifierTree tree, Void unused) { reference(tree); return super.visitIdentifier(tree, unused); }
            @Override public Void visitMemberSelect(MemberSelectTree tree, Void unused) { reference(tree); return super.visitMemberSelect(tree, unused); }
            @Override public Void visitMemberReference(MemberReferenceTree tree, Void unused) { reference(tree); return super.visitMemberReference(tree, unused); }
            @Override public Void visitNewClass(NewClassTree tree, Void unused) { reference(tree); return super.visitNewClass(tree, unused); }
        }.scan(unit, null);
    }

    private String reason(Declaration declaration) {
        if (declaration.test) return "";
        if (declaration.deprecated) return "deprecated";
        if (declaration.element.getKind() == ElementKind.FIELD
                && ownerType(declaration.element).getKind() == ElementKind.RECORD) return "record-storage";
        if (declaration.element instanceof TypeElement) {
            if (declaration.entry) return "entry";
            if (declaration.otherFiles == 0 || !reachable.contains(declaration.owner)) return "type-review";
        }
        if (declaration.element.getKind() == ElementKind.METHOD && declaration.reads == 0
                && declaration.element.getModifiers().contains(Modifier.PRIVATE)) return "private-method";
        if (declaration.element.getKind() == ElementKind.FIELD && declaration.reads == 0
                && !declaration.element.getSimpleName().contentEquals("serialVersionUID")
                && declaration.element.getModifiers().contains(Modifier.PRIVATE)) return "private-field";
        if ((declaration.element.getKind() == ElementKind.LOCAL_VARIABLE || declaration.element.getKind() == ElementKind.RESOURCE_VARIABLE)
                && declaration.reads == 0) return "unread-local";
        if ((declaration.element.getKind() == ElementKind.PARAMETER || declaration.element.getKind() == ElementKind.EXCEPTION_PARAMETER)
                && declaration.reads == 0) return "unused-parameter-review";
        return "";
    }

    private static String cell(Object value) { return Objects.toString(value, "").replace('\t', ' ').replace('\r', ' ').replace('\n', ' '); }
    private void writeReports(int sourceCount) throws Exception {
        String header = "file\tline\tstart\tend\tkind\tsymbol\tmodifiers\tproduction_refs\ttest_refs\treads\twrites\tother_file_refs\treachable\toverride\treason\tinitializer\twrite_locations\treferring_files\ttype\tscope";
        List<String> inventory = new ArrayList<>(List.of(header));
        List<String> candidates = new ArrayList<>(List.of(header));
        List<String> modules = new ArrayList<>();
        Map<String, Long> counts = new TreeMap<>();
        for (Declaration declaration : declarations.values()) {
            String initializer = declaration.tree instanceof VariableTree variable && variable.getInitializer() != null
                    ? variable.getInitializer().toString() : "";
            String reason = reason(declaration);
            String row = Stream.of(declaration.file, declaration.line, declaration.start, declaration.end, declaration.kind,
                    declaration.symbol, declaration.element.getModifiers(), declaration.production, declaration.tests,
                    declaration.reads, declaration.writes, declaration.otherFiles, reachable.contains(declaration.owner),
                    declaration.override, reason, initializer, String.join(";", declaration.writeLocations),
                    String.join(";", declaration.referringFiles), declaration.element.asType(), declaration.scope).map(CodeLifecycleAudit::cell).collect(Collectors.joining("\t"));
            inventory.add(row);
            if (!reason.isEmpty()) { candidates.add(row); counts.merge(reason, 1L, Long::sum); }
            if (!declaration.test && declaration.element instanceof TypeElement type && moduleEntry(type)) modules.add(declaration.owner);
        }
        Files.write(output.resolve("declarations.tsv"), inventory, StandardCharsets.UTF_8);
        Files.write(output.resolve("candidates.tsv"), candidates, StandardCharsets.UTF_8);
        Files.write(output.resolve("module-entries.txt"), modules.stream().sorted().toList(), StandardCharsets.UTF_8);
        Files.write(output.resolve("entry-types.txt"), entryTypes, StandardCharsets.UTF_8);
        List<String> edges = new ArrayList<>(List.of("source\ttarget\treachable"));
        typeEdges.forEach((source, targets) -> targets.stream().filter(typeEdges::containsKey).forEach(target ->
                edges.add(source + "\t" + target + "\t" + reachable.contains(source))));
        Files.write(output.resolve("type-edges.tsv"), edges, StandardCharsets.UTF_8);
        String summary = "Java sources: " + sourceCount + "\nDeclarations: " + declarations.size()
                + "\nModule entries: " + modules.size() + "\nCandidates: " + counts + "\n";
        Files.writeString(output.resolve("summary.txt"), summary, StandardCharsets.UTF_8);
        System.out.print(summary);
    }
}
