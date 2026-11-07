package module.evolview.pathwaybrowser.io;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Holds parsed gene/component copy counts, missing-data states, and optional category groupings across species.
 * Supports both table orientations:
 * <ul>
 *   <li><b>Species-as-rows</b>: Header contains {@code Name} (tree node name), and other columns are gene/component IDs.</li>
 *   <li><b>Genes-as-rows</b> (e.g. {@code components.info.tsv}): First column is {@code name} (gene ID),
 *       optional second column is {@code category} (e.g. {@code cat1..cat9}), and remaining columns are species names.</li>
 * </ul>
 */
public final class PathwayComponentCountData {

    public enum CountState {
        PRESENT,
        ABSENT,
        MISSING
    }

    public record CountEntry(CountState state, int count, String rawDisplay) {
        public static final CountEntry ZERO = new CountEntry(CountState.ABSENT, 0, "0");

        public static CountEntry present(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("Present count must be positive: " + count);
            }
            return new CountEntry(CountState.PRESENT, count, Integer.toString(count));
        }

        public static CountEntry missing(String rawDisplay) {
            return new CountEntry(CountState.MISSING, 0, rawDisplay == null ? "" : rawDisplay.trim());
        }

        public boolean isKnown() {
            return state != CountState.MISSING;
        }

        public boolean isPresent() {
            return state == CountState.PRESENT && count > 0;
        }

        public boolean isAbsent() {
            return state == CountState.ABSENT;
        }
    }

    public record GeneMatch(
            boolean mappedInTable,
            CountState state,
            int count,
            Set<String> matchedGenes) {
        public static final GeneMatch UNMAPPED = new GeneMatch(false, CountState.MISSING, 0, Set.of());
    }

    public record CategoryMatch(
            boolean mappedInTable,
            CountState state,
            int count,
            int presentGenes,
            int knownGenes,
            int totalGenes,
            Set<String> memberGenes) {
        public static final CategoryMatch UNMAPPED = new CategoryMatch(false, CountState.MISSING, 0, 0, 0, 0, Set.of());
    }

    public enum SpeciesMatchStatus {
        MATCHED,
        NOT_FOUND,
        AMBIGUOUS
    }

    public record SpeciesMatchResult<T>(
            SpeciesMatchStatus status,
            String matchedKey,
            T value,
            List<String> candidates) {
        public boolean isMatched() {
            return status == SpeciesMatchStatus.MATCHED;
        }

        public boolean isAmbiguous() {
            return status == SpeciesMatchStatus.AMBIGUOUS;
        }
    }

    private static final List<Set<String>> SPECIES_ALIAS_GROUPS = List.of(
            Set.of("human", "homo sapiens"),
            Set.of("mouse", "mus musculus"),
            Set.of("rat", "rattus norvegicus"),
            Set.of("chimp", "chimpanzee", "pan troglodytes"),
            Set.of("dog", "canis lupus familiaris", "canis familiaris"),
            Set.of("cow", "cattle", "bos taurus"),
            Set.of("chicken", "gallus gallus"),
            Set.of("zebrafish", "danio rerio"),
            Set.of("frog", "xenopus", "xenopus tropicalis", "xenopus laevis"),
            Set.of("fruitfly", "fruit fly", "drosophila", "drosophila melanogaster"),
            Set.of("worm", "nematode", "caenorhabditis elegans", "c. elegans", "c elegans"),
            Set.of("yeast", "budding yeast", "saccharomyces cerevisiae", "s. cerevisiae", "s cerevisiae"),
            Set.of("arabidopsis", "thale cress", "arabidopsis thaliana"),
            Set.of("rice", "oryza sativa")
    );

    private static final Map<String, Integer> TOKEN_TO_ALIAS_GROUP;
    static {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < SPECIES_ALIAS_GROUPS.size(); i++) {
            for (String alias : SPECIES_ALIAS_GROUPS.get(i)) {
                map.put(alias, i);
            }
        }
        TOKEN_TO_ALIAS_GROUP = Collections.unmodifiableMap(map);
    }

    private static final Set<String> MISSING_VALUE_TOKENS = Set.of(
            "", "na", "n/a", "?", ".", "-"
    );

    private final List<String> speciesNames;
    private final List<String> geneNames;
    private final Map<String, String> gene2Category;
    private final Map<String, List<String>> category2Genes;
    private final Map<String, Map<String, CountEntry>> species2GeneEntries;
    private final Map<String, Map<String, Integer>> species2GeneCounts;
    private final Map<String, Map<String, Integer>> species2CategoryCopySum;
    private final Map<String, Map<String, Integer>> species2CategoryPresentGenes;
    private final Map<String, Map<String, Integer>> species2CategoryKnownGenes;
    private final Map<String, Integer> category2TotalGenes;
    private final List<String> normalizedHeaders;
    private final List<List<String>> normalizedRows;
    private final String structureSignature;

    private PathwayComponentCountData(
            List<String> speciesNames,
            List<String> geneNames,
            Map<String, String> gene2Category,
            Map<String, List<String>> category2Genes,
            Map<String, Map<String, CountEntry>> species2GeneEntries,
            Map<String, Map<String, Integer>> species2GeneCounts,
            Map<String, Map<String, Integer>> species2CategoryCopySum,
            Map<String, Map<String, Integer>> species2CategoryPresentGenes,
            Map<String, Map<String, Integer>> species2CategoryKnownGenes,
            Map<String, Integer> category2TotalGenes,
            List<String> normalizedHeaders,
            List<List<String>> normalizedRows) {
        this.speciesNames = Collections.unmodifiableList(speciesNames);
        this.geneNames = Collections.unmodifiableList(geneNames);
        this.gene2Category = Collections.unmodifiableMap(gene2Category);
        this.category2Genes = Collections.unmodifiableMap(category2Genes);
        this.species2GeneEntries = Collections.unmodifiableMap(species2GeneEntries);
        this.species2GeneCounts = Collections.unmodifiableMap(species2GeneCounts);
        this.species2CategoryCopySum = Collections.unmodifiableMap(species2CategoryCopySum);
        this.species2CategoryPresentGenes = Collections.unmodifiableMap(species2CategoryPresentGenes);
        this.species2CategoryKnownGenes = Collections.unmodifiableMap(species2CategoryKnownGenes);
        this.category2TotalGenes = Collections.unmodifiableMap(category2TotalGenes);
        this.normalizedHeaders = Collections.unmodifiableList(normalizedHeaders);
        this.normalizedRows = Collections.unmodifiableList(normalizedRows);
        this.structureSignature = buildStructureSignature(geneNames, gene2Category, category2TotalGenes);
    }

    private static String buildStructureSignature(
            List<String> genes,
            Map<String, String> gene2Cat,
            Map<String, Integer> catTotals) {
        if (genes.isEmpty() && catTotals.isEmpty()) {
            return "EMPTY";
        }
        StringBuilder sb = new StringBuilder();
        for (String g : genes) {
            sb.append(g).append(':').append(gene2Cat.getOrDefault(g, "")).append('|');
        }
        sb.append('#');
        for (Map.Entry<String, Integer> e : catTotals.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('|');
        }
        return sb.toString();
    }

    public static PathwayComponentCountData load(File file) throws IOException {
        if (file == null || !file.isFile()) {
            return empty();
        }
        RawTable raw = readRawTable(file);
        return fromRawTable(raw.headers(), raw.rows());
    }

    public static PathwayComponentCountData empty() {
        return new PathwayComponentCountData(
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of()
        );
    }

    public static PathwayComponentCountData fromRawTable(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty() || rows == null) {
            return empty();
        }

        int exactNameIdx = -1;
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h != null && h.trim().equals("Name")) {
                exactNameIdx = i;
                break;
            }
        }
        int geneRowNameIdx = -1;
        int categoryIdx = -1;
        if (exactNameIdx < 0) {
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i) == null ? "" : headers.get(i).trim();
                if (geneRowNameIdx < 0 && (h.equalsIgnoreCase("name") || h.equalsIgnoreCase("gene") || h.equalsIgnoreCase("component"))) {
                    geneRowNameIdx = i;
                } else if (categoryIdx < 0 && (h.equalsIgnoreCase("category") || h.equalsIgnoreCase("stage") || h.equalsIgnoreCase("group"))) {
                    categoryIdx = i;
                }
            }
        }

        if (exactNameIdx < 0 && geneRowNameIdx >= 0) {
            return parseGenesAsRows(headers, rows, geneRowNameIdx, categoryIdx);
        }
        if (exactNameIdx < 0) {
            throw new IllegalArgumentException(
                    "Missing required identifier column (expected 'Name', 'name', 'gene', or 'component')"
            );
        }
        return parseSpeciesAsRows(headers, rows, exactNameIdx);
    }

    private static PathwayComponentCountData parseGenesAsRows(
            List<String> headers,
            List<List<String>> rows,
            int geneColIdx,
            int catColIdx) {
        List<Integer> speciesCols = new ArrayList<>();
        List<String> speciesNames = new ArrayList<>();
        Set<String> seenSpeciesNorm = new HashSet<>();
        for (int c = 0; c < headers.size(); c++) {
            if (c == geneColIdx || c == catColIdx) {
                continue;
            }
            String sp = headers.get(c) == null ? "" : headers.get(c).trim();
            if (!sp.isEmpty()) {
                String spKey = sp.toLowerCase(Locale.ROOT);
                if (!seenSpeciesNorm.add(spKey)) {
                    throw new IllegalArgumentException("Duplicate species column '" + sp + "' at column " + (c + 1));
                }
                speciesCols.add(c);
                speciesNames.add(sp);
            }
        }

        List<String> geneNames = new ArrayList<>();
        Map<String, Integer> seenGeneUpper2Row = new HashMap<>();
        Map<String, String> gene2Category = new LinkedHashMap<>();
        Map<String, List<String>> category2Genes = new LinkedHashMap<>();
        Map<String, Integer> category2TotalGenes = new LinkedHashMap<>();
        Map<String, Map<String, CountEntry>> species2GeneEntries = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2GeneCounts = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2CategoryCopySum = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2CategoryPresentGenes = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2CategoryKnownGenes = new LinkedHashMap<>();

        for (String sp : speciesNames) {
            species2GeneEntries.put(sp, new LinkedHashMap<>());
            species2GeneCounts.put(sp, new LinkedHashMap<>());
            species2CategoryCopySum.put(sp, new LinkedHashMap<>());
            species2CategoryPresentGenes.put(sp, new LinkedHashMap<>());
            species2CategoryKnownGenes.put(sp, new LinkedHashMap<>());
        }

        for (int rIdx = 0; rIdx < rows.size(); rIdx++) {
            List<String> row = rows.get(rIdx);
            int fileRowNumber = rIdx + 2; // 1-based including header row
            if (row == null || geneColIdx >= row.size()) {
                continue;
            }
            String gene = row.get(geneColIdx) == null ? "" : row.get(geneColIdx).trim();
            if (gene.isEmpty()) {
                continue;
            }
            String upperGene = gene.toUpperCase(Locale.ROOT);
            Integer prevRow = seenGeneUpper2Row.putIfAbsent(upperGene, fileRowNumber);
            if (prevRow != null) {
                throw new IllegalArgumentException(
                        "Duplicate gene ID '" + gene + "' at row " + fileRowNumber + " (already defined at row " + prevRow + ")"
                );
            }
            geneNames.add(gene);

            String rawCat = (catColIdx >= 0 && catColIdx < row.size() && row.get(catColIdx) != null)
                    ? row.get(catColIdx).trim()
                    : "";
            String normCat = rawCat.toLowerCase(Locale.ROOT);
            if (!normCat.isEmpty()) {
                gene2Category.put(gene, normCat);
                category2Genes.computeIfAbsent(normCat, k -> new ArrayList<>()).add(gene);
                category2TotalGenes.merge(normCat, 1, Math::addExact);
            }

            for (int i = 0; i < speciesCols.size(); i++) {
                int col = speciesCols.get(i);
                String sp = speciesNames.get(i);
                String cell = col < row.size() ? row.get(col) : "";
                CountEntry entry = parseCountEntry(cell, fileRowNumber, sp);
                species2GeneEntries.get(sp).put(gene, entry);
                if (entry.isKnown()) {
                    species2GeneCounts.get(sp).put(gene, entry.count());
                }
                if (!normCat.isEmpty()) {
                    species2CategoryCopySum.get(sp).putIfAbsent(normCat, 0);
                    species2CategoryPresentGenes.get(sp).putIfAbsent(normCat, 0);
                    species2CategoryKnownGenes.get(sp).putIfAbsent(normCat, 0);
                    if (entry.isKnown()) {
                        try {
                            int prevSum = species2CategoryCopySum.get(sp).get(normCat);
                            species2CategoryCopySum.get(sp).put(normCat, Math.addExact(prevSum, entry.count()));
                        } catch (ArithmeticException ex) {
                            throw new IllegalArgumentException(
                                    "Category copy sum overflow for category '" + rawCat + "', species '" + sp + "' at row " + fileRowNumber,
                                    ex
                            );
                        }
                        species2CategoryKnownGenes.get(sp).merge(normCat, 1, Math::addExact);
                        if (entry.isPresent()) {
                            species2CategoryPresentGenes.get(sp).merge(normCat, 1, Math::addExact);
                        }
                    }
                }
            }
        }

        Map<String, List<String>> immutableCat2Genes = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : category2Genes.entrySet()) {
            immutableCat2Genes.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }

        // Build transposed species-as-rows table for PathwayComponentPanel, preserving missing raw display
        List<String> normHeaders = new ArrayList<>(geneNames.size() + 1);
        normHeaders.add("Name");
        normHeaders.addAll(geneNames);

        List<List<String>> normRows = new ArrayList<>(speciesNames.size());
        for (String sp : speciesNames) {
            List<String> r = new ArrayList<>(normHeaders.size());
            r.add(sp);
            Map<String, CountEntry> gMap = species2GeneEntries.getOrDefault(sp, Map.of());
            for (String g : geneNames) {
                CountEntry ce = gMap.get(g);
                r.add(ce != null ? ce.rawDisplay() : "");
            }
            normRows.add(Collections.unmodifiableList(r));
        }

        return new PathwayComponentCountData(
                speciesNames,
                geneNames,
                gene2Category,
                immutableCat2Genes,
                 wrapNestedMap(species2GeneEntries),
                wrapNestedMap(species2GeneCounts),
                wrapNestedMap(species2CategoryCopySum),
                wrapNestedMap(species2CategoryPresentGenes),
                wrapNestedMap(species2CategoryKnownGenes),
                category2TotalGenes,
                normHeaders,
                normRows
        );
    }

    private static PathwayComponentCountData parseSpeciesAsRows(
            List<String> headers,
            List<List<String>> rows,
            int nameColIdx) {
        if (nameColIdx < 0) {
            return new PathwayComponentCountData(
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    headers,
                    rows
            );
        }

        List<Integer> geneCols = new ArrayList<>();
        List<String> geneNames = new ArrayList<>();
        Map<String, Integer> seenGeneUpper2Col = new HashMap<>();
        for (int c = 0; c < headers.size(); c++) {
            if (c == nameColIdx) {
                continue;
            }
            String g = headers.get(c) == null ? "" : headers.get(c).trim();
            if (!g.isEmpty()) {
                String upperGene = g.toUpperCase(Locale.ROOT);
                Integer prevCol = seenGeneUpper2Col.putIfAbsent(upperGene, c + 1);
                if (prevCol != null) {
                    throw new IllegalArgumentException(
                            "Duplicate gene column '" + g + "' at column " + (c + 1) + " (already defined at column " + prevCol + ")"
                    );
                }
                geneCols.add(c);
                geneNames.add(g);
            }
        }

        List<String> speciesNames = new ArrayList<>();
        Map<String, Map<String, CountEntry>> species2GeneEntries = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> species2GeneCounts = new LinkedHashMap<>();

        for (int rIdx = 0; rIdx < rows.size(); rIdx++) {
            List<String> row = rows.get(rIdx);
            int fileRowNumber = rIdx + 2;
            if (row == null || nameColIdx >= row.size()) {
                continue;
            }
            String sp = row.get(nameColIdx) == null ? "" : row.get(nameColIdx).trim();
            if (sp.isEmpty() || species2GeneEntries.containsKey(sp)) {
                continue;
            }
            speciesNames.add(sp);
            Map<String, CountEntry> entryMap = new LinkedHashMap<>();
            Map<String, Integer> countMap = new LinkedHashMap<>();
            for (int i = 0; i < geneCols.size(); i++) {
                int col = geneCols.get(i);
                String g = geneNames.get(i);
                String cell = col < row.size() ? row.get(col) : "";
                CountEntry entry = parseCountEntry(cell, fileRowNumber, g);
                entryMap.put(g, entry);
                if (entry.isKnown()) {
                    countMap.put(g, entry.count());
                }
            }
            species2GeneEntries.put(sp, entryMap);
            species2GeneCounts.put(sp, countMap);
        }

        return new PathwayComponentCountData(
                speciesNames,
                geneNames,
                Map.of(),
                Map.of(),
                wrapNestedMap(species2GeneEntries),
                wrapNestedMap(species2GeneCounts),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                headers,
                rows
        );
    }

    private static <V> Map<String, Map<String, V>> wrapNestedMap(Map<String, Map<String, V>> src) {
        Map<String, Map<String, V>> out = new LinkedHashMap<>(src.size());
        for (Map.Entry<String, Map<String, V>> e : src.entrySet()) {
            out.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        }
        return out;
    }

    public static CountEntry parseCountEntry(String value, int rowNumber, String columnName) {
        if (value == null) {
            return CountEntry.missing("");
        }
        String trimmed = value.trim();
        if (MISSING_VALUE_TOKENS.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return CountEntry.missing(trimmed);
        }
        // Reject non-integer, negative, non-finite, scientific-notation, or garbage values strictly
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch < '0' || ch > '9') {
                throw new IllegalArgumentException(
                        "Invalid copy count '" + trimmed + "' at row " + rowNumber + ", column '" + columnName
                                + "' (expected non-negative integer or empty/NA)"
                );
            }
        }
        try {
            long parsed = Long.parseLong(trimmed);
            if (parsed < 0 || parsed > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Copy count out of range '" + trimmed + "' at row " + rowNumber + ", column '" + columnName
                                + "' (max " + Integer.MAX_VALUE + ")"
                );
            }
            return parsed == 0 ? CountEntry.ZERO : CountEntry.present((int) parsed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Copy count out of range '" + trimmed + "' at row " + rowNumber + ", column '" + columnName + "'",
                    ex
            );
        }
    }

    public boolean isEmpty() {
        return species2GeneEntries.isEmpty() && geneNames.isEmpty();
    }

    public String getStructureSignature() {
        return structureSignature;
    }

    public List<String> getSpeciesNames() {
        return speciesNames;
    }

    public List<String> getGeneNames() {
        return geneNames;
    }

    public Set<String> getGeneNameSetUpper() {
        Set<String> set = new HashSet<>();
        for (String g : geneNames) {
            set.add(g.toUpperCase(Locale.ROOT));
        }
        return set;
    }

    public Map<String, String> getGene2Category() {
        return gene2Category;
    }

    public Map<String, List<String>> getCategory2Genes() {
        return category2Genes;
    }

    public Map<String, Integer> getCategory2TotalGenes() {
        return category2TotalGenes;
    }

    public List<String> getNormalizedHeaders() {
        return normalizedHeaders;
    }

    public List<List<String>> getNormalizedRows() {
        return normalizedRows;
    }

    public boolean hasSpecies(String nodeName) {
        return resolveSpeciesMatch(species2GeneEntries, nodeName).isMatched();
    }

    /**
     * Finds the species identifier column index in a species-keyed table (prefers trimmed {@code "Name"},
     * falls back to case-insensitive trimmed {@code "name"}).
     */
    public static int findNameColumnIndex(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h != null && h.trim().equals("Name")) {
                return i;
            }
        }
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h != null && h.trim().equalsIgnoreCase("name")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Resolves a canonical gene key for grouping slide shapes belonging to the same gene or wildcard family.
     */
    public String resolveCanonicalGeneKey(String shapeGeneId) {
        if (shapeGeneId == null || shapeGeneId.isBlank()) {
            return "";
        }
        String trimmed = shapeGeneId.trim();
        if (trimmed.endsWith("*")) {
            String stem = trimmed.substring(0, trimmed.length() - 1).trim();
            return stem.isEmpty() ? "" : (stem.toUpperCase(Locale.ROOT) + "*");
        }
        for (String g : geneNames) {
            if (g.equalsIgnoreCase(trimmed)) {
                return g;
            }
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * Checks whether a candidate shape gene identifier (either exact gene ID or explicit {@code STEM*} family wildcard)
     * matches at least one gene column/row in this count dataset.
     */
    public boolean matchesAnyTableGene(String shapeGeneId) {
        return !resolveTableGenes(shapeGeneId).isEmpty();
    }

    /**
     * Resolves the canonical table gene names matched by {@code shapeGeneId}.
     * Normal identifiers match case-insensitively by exact name only; explicit trailing {@code *} matches family prefix.
     */
    public Set<String> resolveTableGenes(String shapeGeneId) {
        if (shapeGeneId == null || shapeGeneId.isBlank() || geneNames.isEmpty()) {
            return Set.of();
        }
        String trimmed = shapeGeneId.trim();
        if (trimmed.endsWith("*")) {
            String stem = trimmed.substring(0, trimmed.length() - 1).trim();
            if (stem.isEmpty()) {
                return Set.of();
            }
            String upperStem = stem.toUpperCase(Locale.ROOT);
            Set<String> matched = new LinkedHashSet<>();
            for (String g : geneNames) {
                if (g.toUpperCase(Locale.ROOT).startsWith(upperStem)) {
                    matched.add(g);
                }
            }
            return matched.isEmpty() ? Set.of() : Collections.unmodifiableSet(matched);
        }
        for (String g : geneNames) {
            if (g.equalsIgnoreCase(trimmed)) {
                return Set.of(g);
            }
        }
        return Set.of();
    }

    public Map<String, CountEntry> getGeneEntriesForSpecies(String nodeName) {
        Map<String, CountEntry> found = lookupBySpeciesName(species2GeneEntries, nodeName);
        return found != null ? found : Collections.emptyMap();
    }

    /**
     * Returns known integer gene copy counts for {@code nodeName} (excluding {@link CountState#MISSING} cells).
     */
    public Map<String, Integer> getGeneCountsForSpecies(String nodeName) {
        return lookupSpeciesMap(species2GeneCounts, nodeName);
    }

    public Map<String, Integer> getCategoryCopySumForSpecies(String nodeName) {
        return lookupSpeciesMap(species2CategoryCopySum, nodeName);
    }

    public Map<String, Integer> getCategoryPresentGenesForSpecies(String nodeName) {
        return lookupSpeciesMap(species2CategoryPresentGenes, nodeName);
    }

    public GeneMatch resolveGeneForSpecies(String speciesName, String shapeGeneId) {
        Set<String> matchedTableGenes = resolveTableGenes(shapeGeneId);
        if (matchedTableGenes.isEmpty()) {
            return GeneMatch.UNMAPPED;
        }
        Map<String, CountEntry> entries = getGeneEntriesForSpecies(speciesName);
        if (entries.isEmpty()) {
            return new GeneMatch(true, CountState.MISSING, 0, matchedTableGenes);
        }
        int sum = 0;
        boolean anyKnown = false;
        boolean anyPresent = false;
        for (String g : matchedTableGenes) {
            CountEntry ce = entries.get(g);
            if (ce != null && ce.isKnown()) {
                anyKnown = true;
                sum = Math.addExact(sum, ce.count());
                if (ce.isPresent()) {
                    anyPresent = true;
                }
            }
        }
        if (!anyKnown) {
            return new GeneMatch(true, CountState.MISSING, 0, matchedTableGenes);
        }
        return new GeneMatch(
                true,
                anyPresent ? CountState.PRESENT : CountState.ABSENT,
                sum,
                matchedTableGenes
        );
    }

    public CategoryMatch resolveCategoryForSpecies(String speciesName, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return CategoryMatch.UNMAPPED;
        }
        String normCat = categoryId.trim().toLowerCase(Locale.ROOT);
        List<String> catGenes = category2Genes.get(normCat);
        if (catGenes != null && !catGenes.isEmpty()) {
            Set<String> memberSet = new LinkedHashSet<>(catGenes);
            Map<String, Integer> sums = getCategoryCopySumForSpecies(speciesName);
            Map<String, Integer> presents = getCategoryPresentGenesForSpecies(speciesName);
            Map<String, Integer> knowns = lookupSpeciesMap(species2CategoryKnownGenes, speciesName);
            int total = category2TotalGenes.getOrDefault(normCat, catGenes.size());
            int known = knowns.getOrDefault(normCat, 0);
            int present = presents.getOrDefault(normCat, 0);
            int sum = sums.getOrDefault(normCat, 0);
            if (known == 0 || (sum == 0 && known < total)) {
                return new CategoryMatch(true, CountState.MISSING, 0, 0, known, total, Collections.unmodifiableSet(memberSet));
            }
            return new CategoryMatch(
                    true,
                    sum > 0 ? CountState.PRESENT : CountState.ABSENT,
                    sum,
                    present,
                    known,
                    total,
                    Collections.unmodifiableSet(memberSet)
            );
        }
        // Fallback when categories are represented directly as columns in a species-as-rows table
        GeneMatch asGene = resolveGeneForSpecies(speciesName, normCat);
        if (asGene.mappedInTable()) {
            return new CategoryMatch(
                    true,
                    asGene.state(),
                    asGene.count(),
                    asGene.state() == CountState.PRESENT ? 1 : 0,
                    asGene.state() == CountState.MISSING ? 0 : 1,
                    1,
                    asGene.matchedGenes()
            );
        }
        return CategoryMatch.UNMAPPED;
    }

    private static Map<String, Integer> lookupSpeciesMap(
            Map<String, Map<String, Integer>> map,
            String nodeName) {
        Map<String, Integer> found = lookupBySpeciesName(map, nodeName);
        return found != null ? found : Collections.emptyMap();
    }

    public static String normalizeSpeciesToken(String raw) {
        if (raw == null) {
            return "";
        }
        String replaced = raw.trim().replace('_', ' ');
        if (replaced.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(replaced.length());
        boolean prevSpace = false;
        for (int i = 0; i < replaced.length(); i++) {
            char c = replaced.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!prevSpace) {
                    sb.append(' ');
                    prevSpace = true;
                }
            } else {
                sb.append(Character.toLowerCase(c));
                prevSpace = false;
            }
        }
        return sb.toString();
    }

    /**
     * Unified 3-tier species name resolver shared by tree->panel and panel->tree interactions:
     * <ol>
     *   <li>Exact trimmed name match</li>
     *   <li>Normalized match (case-insensitive + {@code _} and whitespace unified)</li>
     *   <li>Bidirectional common/scientific species alias group match (rejects ambiguous multi-hits)</li>
     * </ol>
     */
    public static <T> SpeciesMatchResult<T> resolveSpeciesMatch(Map<String, T> map, String queryName) {
        if (queryName == null || queryName.isBlank() || map == null || map.isEmpty()) {
            return new SpeciesMatchResult<>(SpeciesMatchStatus.NOT_FOUND, null, null, List.of());
        }
        String trimmedQuery = queryName.trim();
        if (map.containsKey(trimmedQuery)) {
            return new SpeciesMatchResult<>(SpeciesMatchStatus.MATCHED, trimmedQuery, map.get(trimmedQuery), List.of(trimmedQuery));
        }

        // Tier 1: exact trimmed match across keys
        List<Map.Entry<String, T>> exactHits = new ArrayList<>();
        for (Map.Entry<String, T> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().trim().equals(trimmedQuery)) {
                exactHits.add(e);
            }
        }
        if (exactHits.size() == 1) {
            Map.Entry<String, T> hit = exactHits.getFirst();
            return new SpeciesMatchResult<>(SpeciesMatchStatus.MATCHED, hit.getKey(), hit.getValue(), List.of(hit.getKey()));
        }

        // Tier 2: normalized match (unifies '_' and spaces + case-insensitive)
        String normQuery = normalizeSpeciesToken(trimmedQuery);
        if (normQuery.isEmpty()) {
            return new SpeciesMatchResult<>(SpeciesMatchStatus.NOT_FOUND, null, null, List.of());
        }
        List<Map.Entry<String, T>> normHits = new ArrayList<>();
        for (Map.Entry<String, T> e : map.entrySet()) {
            if (e.getKey() != null && normalizeSpeciesToken(e.getKey()).equals(normQuery)) {
                normHits.add(e);
            }
        }
        if (normHits.size() == 1) {
            Map.Entry<String, T> hit = normHits.getFirst();
            return new SpeciesMatchResult<>(SpeciesMatchStatus.MATCHED, hit.getKey(), hit.getValue(), List.of(hit.getKey()));
        }
        if (normHits.size() > 1) {
            return new SpeciesMatchResult<>(
                    SpeciesMatchStatus.AMBIGUOUS,
                    null,
                    null,
                    normHits.stream().map(Map.Entry::getKey).toList()
            );
        }

        // Tier 3: bidirectional common <-> scientific species equivalence class
        Integer queryGroup = TOKEN_TO_ALIAS_GROUP.get(normQuery);
        if (queryGroup != null) {
            List<Map.Entry<String, T>> aliasHits = new ArrayList<>();
            for (Map.Entry<String, T> e : map.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                String normKey = normalizeSpeciesToken(e.getKey());
                if (queryGroup.equals(TOKEN_TO_ALIAS_GROUP.get(normKey))) {
                    aliasHits.add(e);
                }
            }
            if (aliasHits.size() == 1) {
                Map.Entry<String, T> hit = aliasHits.getFirst();
                return new SpeciesMatchResult<>(SpeciesMatchStatus.MATCHED, hit.getKey(), hit.getValue(), List.of(hit.getKey()));
            }
            if (aliasHits.size() > 1) {
                return new SpeciesMatchResult<>(
                        SpeciesMatchStatus.AMBIGUOUS,
                        null,
                        null,
                        aliasHits.stream().map(Map.Entry::getKey).toList()
                );
            }
        }

        return new SpeciesMatchResult<>(SpeciesMatchStatus.NOT_FOUND, null, null, List.of());
    }

    public static <T> T lookupBySpeciesName(Map<String, T> map, String nodeName) {
        SpeciesMatchResult<T> res = resolveSpeciesMatch(map, nodeName);
        return res.isMatched() ? res.value() : null;
    }

    /**
     * Resolves the copy count for a slide shape's gene identifier from a species' known gene count map.
     * Normal gene IDs match strictly by case-insensitive exact name (never substring/prefix);
     * explicit family wildcards ending with {@code *} sum matching family members.
     */
    public static OptionalInt resolveGeneCount(Map<String, Integer> speciesGeneCounts, String shapeGeneId) {
        if (speciesGeneCounts == null || speciesGeneCounts.isEmpty() || shapeGeneId == null || shapeGeneId.isBlank()) {
            return OptionalInt.empty();
        }
        String target = shapeGeneId.trim();
        if (target.endsWith("*")) {
            String stem = target.substring(0, target.length() - 1).trim();
            if (stem.isEmpty()) {
                return OptionalInt.empty();
            }
            String upperStem = stem.toUpperCase(Locale.ROOT);
            int sum = 0;
            boolean matched = false;
            for (Map.Entry<String, Integer> entry : speciesGeneCounts.entrySet()) {
                if (entry.getKey() != null && entry.getKey().toUpperCase(Locale.ROOT).startsWith(upperStem)) {
                    sum = Math.addExact(sum, entry.getValue());
                    matched = true;
                }
            }
            return matched ? OptionalInt.of(sum) : OptionalInt.empty();
        }
        Integer exact = speciesGeneCounts.get(target);
        if (exact != null) {
            return OptionalInt.of(exact);
        }
        for (Map.Entry<String, Integer> entry : speciesGeneCounts.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(target)) {
                return OptionalInt.of(entry.getValue());
            }
        }
        return OptionalInt.empty();
    }

    public record RawTable(List<String> headers, List<List<String>> rows) {}

    public static RawTable readRawTable(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return readExcel(file, name.endsWith(".xlsx"));
        }
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new RawTable(List.of(), List.of());
        }
        String first = lines.getFirst();
        if (!first.isEmpty() && first.charAt(0) == '\uFEFF') {
            first = first.substring(1);
        }
        List<String> headers = List.of(first.split("\t", -1));
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            String[] normalized = new String[headers.size()];
            for (int c = 0; c < normalized.length; c++) {
                normalized[c] = (c < parts.length && parts[c] != null) ? parts[c] : "";
            }
            rows.add(List.of(normalized));
        }
        return new RawTable(headers, rows);
    }

    public static RawTable readExcel(File file, boolean isXlsx) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = isXlsx ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            if (workbook.getNumberOfSheets() == 0) {
                return new RawTable(List.of(), List.of());
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return new RawTable(List.of(), List.of());
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return new RawTable(List.of(), List.of());
            }
            int colCount = headerRow.getLastCellNum();
            if (colCount <= 0) {
                return new RawTable(List.of(), List.of());
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<String> headers = new ArrayList<>(colCount);
            for (int c = 0; c < colCount; c++) {
                headers.add(evaluateCellToString(headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
            }
            List<List<String>> rows = new ArrayList<>();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int r = firstDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String[] cells = new String[colCount];
                for (int c = 0; c < colCount; c++) {
                    cells[c] = evaluateCellToString(row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator);
                }
                rows.add(List.of(cells));
            }
            return new RawTable(headers, rows);
        }
    }

    public static String evaluateCellToString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatExcelNumeric(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case BLANK -> "";
            case ERROR -> throw new IllegalArgumentException(
                    "Excel error cell at row " + (cell.getRowIndex() + 1) + ", column " + (cell.getColumnIndex() + 1)
                            + ": " + FormulaError.forInt(cell.getErrorCellValue()).getString()
            );
            case FORMULA -> evaluateFormulaCell(cell, evaluator);
            default -> "";
        };
    }

    private static String evaluateFormulaCell(Cell cell, FormulaEvaluator evaluator) {
        int rowNum = cell.getRowIndex() + 1;
        int colNum = cell.getColumnIndex() + 1;
        if (evaluator != null) {
            try {
                CellValue cv = evaluator.evaluate(cell);
                if (cv == null) {
                    return "";
                }
                return switch (cv.getCellType()) {
                    case STRING -> cv.getStringValue() != null ? cv.getStringValue() : "";
                    case NUMERIC -> formatExcelNumeric(cv.getNumberValue());
                    case BOOLEAN -> Boolean.toString(cv.getBooleanValue());
                    case BLANK -> "";
                    case ERROR -> throw new IllegalArgumentException(
                            "Excel formula error at row " + rowNum + ", column " + colNum
                                    + " (=" + cell.getCellFormula() + "): "
                                    + FormulaError.forInt(cv.getErrorValue()).getString()
                    );
                    default -> "";
                };
            } catch (NotImplementedException ex) {
                // Fallback to cached result only if Excel stored a valid non-error cached result
                return switch (cell.getCachedFormulaResultType()) {
                    case STRING -> cell.getStringCellValue();
                    case NUMERIC -> formatExcelNumeric(cell.getNumericCellValue());
                    case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
                    case BLANK -> "";
                    default -> throw new IllegalArgumentException(
                            "Unsupported Excel formula at row " + rowNum + ", column " + colNum + ": =" + cell.getCellFormula(),
                            ex
                    );
                };
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "Failed to evaluate Excel formula at row " + rowNum + ", column " + colNum + ": =" + cell.getCellFormula(),
                        ex
                );
            }
        }
        throw new IllegalArgumentException(
                "Cannot evaluate Excel formula without evaluator at row " + rowNum + ", column " + colNum + ": =" + cell.getCellFormula()
        );
    }

    private static String formatExcelNumeric(double val) {
        if (!Double.isFinite(val)) {
            return Double.toString(val);
        }
        if (val == Math.rint(val) && Math.abs(val) <= 9_007_199_254_740_992.0) {
            return Long.toString((long) val);
        }
        return BigDecimal.valueOf(val).toPlainString();
    }
}
