package module.evolview.moderntreeviewer;

import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.*;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.phylotree.visualization.layout.*;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JEditorPane;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Exercises bundled resources through the real VOICE parser, tree importer and renderers. */
public final class ModernTreeExamplesTest {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static List<GraphicsNode> nodes(GraphicsNode root) {
        List<GraphicsNode> result = new ArrayList<>();
        result.add(root);
        for (int i = 0; i < root.getChildCount(); i++) result.addAll(nodes((GraphicsNode) root.getChildAt(i)));
        return result;
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of("temp/modern-tree-examples");
        Files.createDirectories(output);
        if (args.length == 1 && args[0].equals("--install-default")) {
            ModernTreeExamples.install(ModernTreeExamples.directory());
            LoggerFactory.getLogger(ModernTreeExamplesTest.class).info("Installed examples in {}", ModernTreeExamples.directory());
            return;
        }
        Path data = Files.createTempDirectory(output, "config-");
        var workers = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < 4; i++) workers.add(CompletableFuture.runAsync(() -> {
            try { ModernTreeExamples.install(data); }
            catch (Exception error) { throw new RuntimeException(error); }
        }));
        CompletableFuture.allOf(workers.toArray(CompletableFuture[]::new)).join();
        try (var installed = Files.list(data)) {
            require(installed.count() == ModernTreeExamples.resources().size(), "Concurrent installs publish complete resources only");
        }
        Path first = data.resolve(ModernTreeExamples.ALL.getFirst().file());
        String original = Files.readString(first);
        Files.writeString(first, "user-edited data");
        var modified = Files.getLastModifiedTime(first);
        Path missing = data.resolve("10-node-styles.tsv");
        Files.delete(missing);
        ModernTreeExamples.install(data);
        require(Files.readString(first).equals("user-edited data") && Files.getLastModifiedTime(first).equals(modified), "Existing user data is preserved");
        require(Files.exists(missing), "Only missing files are restored");
        Files.writeString(first, original);
        Path blocked = Files.createTempDirectory(output, "blocked-");
        Files.createDirectory(blocked.resolve(ModernTreeExamples.ALL.getFirst().file()));
        try { ModernTreeExamples.install(blocked); throw new AssertionError("Directory conflict accepted"); }
        catch (java.io.IOException expected) { require(expected.getMessage().contains("01-great-apes"), "Failure identifies target"); }

        VOICE4MTV voice = new VOICE4MTV(null);
        voice.actionBeforeVoiceGuiAppear();
        require(voice.getNumberOfExamples() == 12, "Example count");
        String initial = voice.getExampleText();
        for (int i = 1; i < 12; i++) require(voice.getExampleText().contains("# Example " + (i + 1) + ":"), "Stable order");
        require(initial.equals(voice.getExampleText()), "Example cycle wraps");
        voice.actionBeforeVoiceGuiAppear();
        require(initial.equals(voice.getExampleText()), "Reopening resets to first example");

        for (int i = 0; i < ModernTreeExamples.ALL.size(); i++) {
            var example = ModernTreeExamples.ALL.get(i);
            String text = ModernTreeExamples.text(i, data);
            Files.writeString(output.resolve(example.file() + ".voice"), text);
            require(text.contains("# Source:") && text.contains("SOURCES.txt"), "Every VOICE example carries provenance");
            if (i == 3 || i == 4) {
                require(text.contains("ape 5.8-1") && text.contains("# Reference:") && text.contains("# Data license: GPL-2"), "ape attribution survives in VOICE text");
                require(text.contains(i == 3 ? "ape::bird.orders" : "ape::hivtree.newick"), "Original ape dataset identified");
            }
            var input = new VoiceParameterParser().getOrganizedParameterGetter(text);
            var bean = new ParamsAssignerAndParser4ModernTreeView().generateTreeFromKeyValue(input);
            require(bean.getInput_nwk_string().isEmpty(), "Examples read installed files");
            GraphicsNode root = new TreeParser4MTV().parseTree(bean).orElseThrow();
            require(GraphicTreePropertyCalculator.getLeaves(root).size() == example.leaves(), "Leaf count: " + example.file());
            require(nodes(root).stream().allMatch(n -> Double.isFinite(n.getRealBranchLength()) && n.getRealBranchLength() >= 0), "Finite nonnegative lengths");
            TreeLayoutProperties properties = new TreeLayoutProperties(root);
            TreePropertiesAssigner assigner = new TreePropertiesAssigner();
            assigner.assign(properties, bean);
            TreeSettings.apply(properties, TreeSettings.defaults());
            TreeSettings.apply(properties, TreeSettings.explicitImport(input, properties));
            assigner.assignGraphicsNodeEffects(root, bean);
            if (i == 1) require(nodes(root).stream().anyMatch(n -> n.getChildCount() > 0 && n.getBootstrap() == 65), "Bootstrap values parsed as support");
            if (i == 5) require(nodes(root).stream().anyMatch(n -> "Clade1to16".equals(n.getName())), "Internal Newick names retained");
            if (i == 6) require(depth(root) >= 11, "Ladder has deep asymmetric topology");
            if (i == 7) {
                require(root.getChildCount() > 2 && nodes(root).stream().anyMatch(n -> n != root && n.getChildCount() > 2), "Root and internal polytomies");
                require(nodes(root).stream().anyMatch(n -> n != root && n.getRealBranchLength() == 0), "Zero branches retained");
            }
            if (i == 9) {
                GraphicsNode group = nodes(root).stream().filter(n -> "GroupA".equals(n.getName())).findFirst().orElseThrow();
                require(group.getDrawUnit().getCircleRadius() == 5 && group.getDrawUnit().getLineColor().equals(Color.decode("#0072B2")), "Table annotations applied");
            }
            if (i == 11) require(!properties.getShowLeafPropertiesInfo().isShowLeafLabel(), "Large tree labels initially hidden");
            if (List.of(0, 1, 2, 4, 7, 9, 10, 11).contains(i)) {
                SwingUtilities.invokeAndWait(() -> {
                    try { render(properties, root, example.layout(), output.resolve(example.file() + ".png")); }
                    catch (Exception error) { throw new RuntimeException(error); }
                });
            }
        }
        // Existing invalid data must reach the normal parser error, never be silently repaired.
        Files.writeString(first, "invalid user Newick");
        ModernTreeExamples.install(data);
        var bad = new ParamsAssignerAndParser4ModernTreeView().generateTreeFromKeyValue(
                new VoiceParameterParser().getOrganizedParameterGetter(ModernTreeExamples.text(0, data)));
        try { new TreeParser4MTV().parseTree(bad); throw new AssertionError("Invalid existing data accepted"); }
        catch (module.evoltreio.exception.NewickParseException expected) { }
        require(Files.readString(first).equals("invalid user Newick"), "Invalid existing data is not overwritten");
        for (String language : List.of("zh", "en")) {
            String manual;
            try (var input = ModernTreeExamplesTest.class.getResourceAsStream("manual_" + language + ".html")) {
                require(input != null, "Bundled help exists");
                manual = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            require(manual.contains("ape::bird.orders") && manual.contains("ape::hivtree.newick")
                    && manual.contains("ape 5.8-1") && manual.contains("GPL-2")
                    && manual.contains("Sibley") && manual.contains("Strimmer"), "Help preserves dataset attribution and licensing");
            String section = manual.substring(manual.indexOf("<!-- example-data-sources-start -->"), manual.indexOf("<!-- example-data-sources-end -->"));
            SwingUtilities.invokeAndWait(() -> {
                try {
                    JEditorPane help = new JEditorPane("text/html", "<html><body>" + section + "</body></html>");
                    help.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
                    help.setFont(egps2.UnifiedAccessPoint.getLaunchProperty().getDefaultFont());
                    help.setSize(1000, 1400);
                    help.setSize(1000, help.getPreferredSize().height);
                    BufferedImage image = new BufferedImage(help.getWidth(), help.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    try { help.printAll(graphics); } finally { graphics.dispose(); }
                    ImageIO.write(image, "png", output.resolve("help-sources-" + language + ".png").toFile());
                } catch (Exception error) { throw new RuntimeException(error); }
            });
        }
        LoggerFactory.getLogger(ModernTreeExamplesTest.class).info("ModernTreeExamplesTest passed; previews in {}", output);
    }

    private static int depth(GraphicsNode root) {
        int maximum = 0;
        for (int i = 0; i < root.getChildCount(); i++) maximum = Math.max(maximum, 1 + depth((GraphicsNode) root.getChildAt(i)));
        return maximum;
    }

    private static void render(TreeLayoutProperties properties, GraphicsNode root, String name, Path output) throws Exception {
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(properties);
        BaseLayout layout = name.equals("Circular phylogram") ? new CircularPhylo(properties, root, host)
                : name.equals("Radial phylogram") ? new RadialPhyloLayout(properties, root, host)
                : name.equals("Spiral alpha phylogram") ? new SpiralPhyloWithAlpha(properties, root, host)
                : new RectPhyloLayout(properties, root, host);
        Dimension size = layout.getRequiredCanvasSize(1300, 900);
        layout.calculateForPainting(size.width, size.height);
        for (GraphicsNode node : nodes(root)) require(Double.isFinite(node.getXSelf()) && Double.isFinite(node.getYSelf()), "Finite layout coordinates");
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, size.width, size.height);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            layout.paintGraphics(graphics);
            layout.drawFixedTitle(graphics, size.width, size.height);
        } finally { graphics.dispose(); }
        ImageIO.write(image, "png", output.toFile());
    }
}
