package module.evolview.moderntreeviewer;

import module.evolview.gfamily.work.gui.dialog.SearchDialogContent;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Exercises the real Modern Tree controller, Swing buttons, and painted tree. */
public final class TreeSearchRegressionTest {
    private static final List<String> failures = new ArrayList<>();

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static <T extends Component> T component(Container parent, Class<T> type, String text) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child) && (text == null || child instanceof JButton b && text.equals(b.getText())))
                return type.cast(child);
            if (child instanceof Container container) {
                T found = component(container, type, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        var field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static final class Fixture {
        final GraphicsNode root = new GraphicsNode("Apes");
        final GraphicsNode bonobo = new GraphicsNode("Bonobo");
        final GraphicsNode human = new GraphicsNode("Human");
        final TreeLayoutProperties properties;
        final PhylogeneticTreePanel tree;
        final ModernTreeController controller;
        final SearchDialogContent search;
        final JTextField input;
        final JDialog dialog = new JDialog((Frame) null);

        Fixture() throws Exception {
            root.addChild(human); root.addChild(bonobo);
            human.setRealBranchLength(2); human.setDisplayedBranchLength(2);
            bonobo.setRealBranchLength(3); bonobo.setDisplayedBranchLength(3);
            properties = new TreeLayoutProperties(root);
            properties.setMyLayout(TreeLayout.RECT_PHYLO_LEFT);
            properties.getShowLeafPropertiesInfo().setShowLeafLabel(true);
            human.getDrawUnit().setDrawName(true); bonobo.getDrawUnit().setDrawName(true);
            tree = new PhylogeneticTreePanel(properties);
            JViewport viewport = new JViewport();
            viewport.setSize(900, 600); viewport.setView(tree); tree.initializeLeftPanel();
            tree.setSize(tree.getPreferredSize());
            MTreeViewMainFace face = allocate(MTreeViewMainFace.class);
            face.phylogeneticTreePanel = tree;
            controller = allocate(ModernTreeController.class);
            var field = ModernTreeController.class.getDeclaredField("mainFace");
            field.setAccessible(true); field.set(controller, face);
            controller.setTreeLayoutProperties(properties);
            search = new SearchDialogContent(controller);
            dialog.add(search); dialog.pack();
            input = component(search, JTextField.class, null);
        }

        void click(String text) { component(search, JButton.class, text).doClick(0); }
        void find(String text) { input.setText(text); click("Find"); }
        String status() {
            for (Component c : search.getComponents())
                if (c instanceof JTextField f && !f.isEditable()) return f.getText();
            throw new AssertionError("Missing status");
        }
        BufferedImage render() {
            BufferedImage image = new BufferedImage(tree.getWidth(), tree.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); tree.printAll(g); g.dispose(); return image;
        }
    }

    @FunctionalInterface private interface Check { void run(Fixture f) throws Exception; }
    private static void check(String name, Check check) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Fixture fixture = null;
            try {
                fixture = new Fixture(); check.run(fixture);
                LoggerFactory.getLogger(TreeSearchRegressionTest.class).info("PASS {}", name);
            } catch (Throwable error) {
                failures.add(name + ": " + error);
                LoggerFactory.getLogger(TreeSearchRegressionTest.class).error("FAIL {}", name, error);
            } finally { if (fixture != null) fixture.dialog.dispose(); }
        });
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("temp/tree-search"));
        try {
            check("Screenshot query and visible highlight", f -> {
                BufferedImage before = f.render(); f.find("Bon");
                require(f.bonobo.getDrawUnit().isNodeSelected(), "Bon must find Bonobo");
                require(f.status().contains("1"), "Find must report a result");
                BufferedImage after = f.render();
                require(!java.util.Arrays.equals(before.getRGB(0, 0, before.getWidth(), before.getHeight(), null, 0, before.getWidth()),
                        after.getRGB(0, 0, after.getWidth(), after.getHeight(), null, 0, after.getWidth())), "Highlight must change the canvas");
                ImageIO.write(after, "png", Path.of("temp/tree-search/bonobo-highlight.png").toFile());
            });
            check("Enter searches", f -> {
                f.input.setText("Bon"); f.input.postActionEvent();
                require(f.bonobo.getDrawUnit().isNodeSelected(), "Enter must execute Find");
            });
            check("Previous honors edited query", f -> {
                f.find("Bon"); f.input.setText("Human"); f.click("Find Prev");
                require(f.human.getDrawUnit().isNodeSelected() && !f.bonobo.getDrawUnit().isNodeSelected(), "Previous must search current text");
                f.click("Find");
                require(f.human.getDrawUnit().isNodeSelected(), "Next must retain the new result set");
            });
            check("Changed query removes old highlights", f -> {
                f.find("Bon"); f.find("Human");
                require(!f.bonobo.getDrawUnit().isNodeSelected(), "Old result must not stay highlighted");
                f.find("absent");
                require(!f.human.getDrawUnit().isNodeSelected(), "No-match query clears the previous search marker");
                require(!component(f.search, JButton.class, "Find Prev").isEnabled(), "No-match disables Previous");
            });
            check("Empty query resets navigation", f -> {
                f.find("Bon"); f.find("");
                require(!component(f.search, JButton.class, "Find Prev").isEnabled(), "Empty query disables Previous");
            });
            check("Clear then repeat same query", f -> {
                f.find("Bon"); f.click("Clear Highlights"); f.click("Find");
                require(f.bonobo.getDrawUnit().isNodeSelected(), "Find after Clear must search again");
            });
            check("Reopen and repeat same query", f -> {
                f.find("Bon"); f.click("Close");
                f.dialog.pack(); f.click("Find");
                require(f.bonobo.getDrawUnit().isNodeSelected() && component(f.search, JButton.class, "Find Prev").isEnabled(), "Reopened dialog must search");
            });
            check("Navigation wraps in both directions", f -> {
                f.find("o");
                require(f.bonobo.getDrawUnit().isNodeSelected(), "First result");
                f.click("Find Prev"); f.click("Find");
                require(f.bonobo.getDrawUnit().isNodeSelected(), "Single-result wrap");
                f.human.setName("Bon cousin"); f.search.resetResults(); f.click("Find");
                require(f.human.getDrawUnit().isNodeSelected() && f.status().startsWith("1 / 2"), "First of multiple results");
                f.click("Find Prev");
                require(f.bonobo.getDrawUnit().isNodeSelected() && f.status().startsWith("2 / 2"), "Previous wraps to last");
                f.click("Find");
                require(f.human.getDrawUnit().isNodeSelected() && !f.bonobo.getDrawUnit().isNodeSelected(), "Next wraps to first");
            });
            check("Highlight all, close, reopen, and clear preserve manual styling", f -> {
                f.human.setName("Bon cousin");
                f.properties.getSelectedNodes().add(f.human);
                f.human.getDrawUnit().setBranchSelected(true);
                f.human.getDrawUnit().setLineColor(Color.BLUE);
                f.input.setText("Bon"); f.click("Highlight All");
                require(f.human.getDrawUnit().isNodeSelected() && f.bonobo.getDrawUnit().isNodeSelected(), "All matches highlighted");
                require(!f.dialog.isDisplayable(), "Highlight All closes dialog");
                f.dialog.pack(); f.click("Clear Highlights");
                require(!f.human.getDrawUnit().isNodeSelected() && !f.bonobo.getDrawUnit().isNodeSelected(), "Clear removes all search markers");
                require(f.human.getDrawUnit().isBranchSelected() && f.properties.getSelectedNodes().contains(f.human)
                        && Color.BLUE.equals(f.human.getDrawUnit().getLineColor()), "Manual selection and color preserved");
                f.click("Find"); f.click("Close");
                require(f.human.getDrawUnit().isNodeSelected(), "Close keeps search marker");
            });
            check("No matches remain actionable", f -> {
                f.find("absent"); String status = f.status(); f.click("Find");
                require(status.equals(f.status()) && status.contains("No matching"), "Repeated misses report a clear status");
                f.click("Highlight All");
                require(f.dialog.isDisplayable(), "No-match Highlight All keeps feedback visible");
                f.find("Bon"); require(f.bonobo.getDrawUnit().isNodeSelected(), "Recover after no matches");
            });
            check("No tree is safe for every search action", f -> {
                f.find("Bon");
                f.controller.setTreeLayoutProperties(null);
                for (String action : List.of("Find Prev", "Find", "Highlight All", "Clear Highlights")) {
                    f.input.setText("Bon"); f.click(action);
                    require(f.status().contains("tree first"), action + " explains missing tree");
                    require(!component(f.search, JButton.class, "Find Prev").isEnabled(), "No stale Previous");
                }
                f.click("Close"); require(!f.dialog.isDisplayable(), "Close works without a tree");
            });
            check("Internal, unnamed, Unicode and literal case-sensitive names", f -> {
                f.find("Ape"); require(f.root.getDrawUnit().isNodeSelected(), "Internal nodes are searchable");
                f.human.setName(null); f.bonobo.setName("倭黑猩猩 [Bonobo]");
                f.find("倭黑"); require(f.bonobo.getDrawUnit().isNodeSelected(), "Unicode substring");
                f.find("[Bon"); require(f.bonobo.getDrawUnit().isNodeSelected(), "Search is literal, not regex");
                f.find("bon"); require(f.status().contains("No matching"), "Documented case sensitivity");
                f.find("   "); require(f.status().contains("at least"), "Whitespace-only query is empty input");
            });
            check("External clear resets cached search", f -> {
                f.find("Bon"); f.tree.clearNodeSelection();
                require(!component(f.search, JButton.class, "Find Prev").isEnabled(), "Toolbar clear invalidates results");
                f.click("Find"); require(f.bonobo.getDrawUnit().isNodeSelected(), "Same query recovers after toolbar clear");
            });
            check("Search follows displayed subtree", f -> {
                f.find("Bon");
                f.tree.resetNewRoot(f.human);
                f.click("Find"); require(f.status().contains("No matching"), "Changed root invalidates cached search; other subtrees are outside scope");
                f.find("Human"); require(f.human.getDrawUnit().isNodeSelected(), "Displayed subtree root is searchable");
            });
            check("Collapsed results explain invisible markers", f -> {
                f.root.setCollapse(true); f.find("Bon");
                require(f.bonobo.getDrawUnit().isNodeSelected() && f.status().contains("collapsed"), "Hidden match has explicit feedback");
                require(f.root.getCollapse(), "Search preserves collapse state");
            });
            check("Large result sets do not overflow", f -> {
                for (int i = 0; i < 33000; i++) f.root.addChild(new GraphicsNode("match-" + i));
                f.find("match-"); f.click("Find Prev");
                require(f.status().startsWith("33000 / 33000"), "Previous reaches result above Short.MAX_VALUE");
                f.click("Find"); require(f.status().startsWith("1 / 33000"), "Large result set wraps forward");
            });
            check("Deep trees can be searched and cleared without recursive overflow", f -> {
                GraphicsNode current = f.bonobo;
                for (int i = 0; i < 15000; i++) {
                    GraphicsNode child = new GraphicsNode(); current.addChild(child); current = child;
                }
                current.setName("deep target"); f.find("deep target");
                require(current.getDrawUnit().isNodeSelected(), "Deep result found");
                f.click("Clear Highlights"); require(!current.getDrawUnit().isNodeSelected(), "Deep marker cleared");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> { for (Window window : Window.getWindows()) window.dispose(); });
        }
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        LoggerFactory.getLogger(TreeSearchRegressionTest.class).info("TreeSearchRegressionTest passed");
    }
}
