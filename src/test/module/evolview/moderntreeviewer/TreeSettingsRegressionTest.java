package module.evolview.moderntreeviewer;

import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.gui.CreativeModeTaskPanel;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.moderntreeviewer.para.TreeSettingsEditor;
import module.evolview.moderntreeviewer.para.TreeSettingsPreferences;
import module.evolview.moderntreeviewer.para.TreeSettingsSession;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TreeSettingsRegressionTest {
    private static final Path OUTPUT = Path.of("temp/tree-settings");

    private static class MainFace extends MTreeViewMainFace {
        IndependentModuleLoader loader;
        MainFace() { super(null); }
        @Override public void invokeTheFeatureMethod(int index) { }
        @Override public Optional<egps2.modulei.IModuleLoader> getModuleLoader() { return Optional.ofNullable(loader); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static boolean equivalent(Object expected, Object actual) {
        return expected instanceof Number a && actual instanceof Number b
                ? Math.abs(a.doubleValue() - b.doubleValue()) < 0.00001 : Objects.equals(expected, actual);
    }

    private static void invalid(String text, String fragment) {
        try { TreeSettings.parse(text); throw new AssertionError("Accepted invalid settings: " + text); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains(fragment), "Useful error: " + expected.getMessage()); }
    }

    private static TreeLayoutProperties properties() {
        GraphicsNode root = new GraphicsNode();
        for (String name : List.of("Human", "Chimpanzee")) {
            GraphicsNode leaf = new GraphicsNode();
            leaf.setName(name);
            leaf.setRealBranchLength(2);
            root.addChild(leaf);
        }
        TreeLayoutProperties result = new TreeLayoutProperties(root);
        TreeSettings.apply(result, TreeSettings.defaults());
        return result;
    }

    private static void testParserAndMapping() {
        require(TreeSettings.parse(TreeSettings.defaultText()).equals(TreeSettings.defaults()), "Documented defaults match the actual settings");
        require(!TreeSettings.defaultText().contains("\t"), "Default text has no tabs");
        String comments = "{/* note */\"Display\":{\"Title text\":\"https://example.org/a/*b*/ \\\"x\\\" 中文\",},\n// note\n\"Labels\":{\"Show leaf names\":false,},}";
        require(TreeSettings.parse(comments).get("Display/Title text").equals("https://example.org/a/*b*/ \"x\" 中文"), "Comments do not damage strings");
        invalid("{\n/* unfinished", "line 2");
        invalid("{\"Labels\":{},\"Labels\":{}}", "Duplicate setting");
        invalid("{\"Labels\":{\"Show leaf names\":true,\"Show leaf names\":false}}", "Duplicate setting");
        invalid("{\"Unknown\":true}", "Unknown");
        invalid("{\"Labels\":{\"Show leaf names\":\"false\"}}", "Show leaf names");
        invalid("{\"Root\":{\"Root branch length\":-1}}", "Root branch length");
        invalid("{\"Circular layout\":{\"Start angle\":1.5}}", "integer");
        invalid("{\"Slant layout\":{\"Rotation\":45}}", "90");
        invalid("{\"Labels\":{\"Show leaf names\":null}}", "true or false");
        for (String malformed : List.of("{'Labels':{}}", "{Labels:{}}", "{,,}", "{} trailing", "{\"a\":01}", "{\"a\":\"\\q\"}", "{\"a\":+1}")) {
            try { TreeSettings.parse(malformed); throw new AssertionError("Accepted malformed JSON: " + malformed); }
            catch (IllegalArgumentException expected) { }
        }
        TreeLayoutProperties properties = properties();
        require(properties.getMyLayout() == TreeLayout.RECT_PHYLO_LEFT && properties.getShowLeafPropertiesInfo().isShowLeafLabel(), "Unified defaults");
        Map<String, Object> defaultSnapshot = TreeSettings.explicitImport(null, properties);
        TreeSettings.defaults().forEach((key, value) -> require(equivalent(value, defaultSnapshot.get(key)), "Default setting round trip: " + key));
        for (TreeLayout layout : TreeLayout.values()) {
            properties.setMyLayout(layout);
            Object friendlyName = TreeSettings.explicitImport(null, properties).get("Layout/Type");
            TreeSettings.apply(properties, TreeSettings.parse("{\"Layout\":{\"Type\":" + com.alibaba.fastjson.JSON.toJSONString(friendlyName) + "}}"));
            require(properties.getMyLayout() == layout, "Friendly layout mapping: " + layout);
        }
        Map<String, Object> modified = TreeSettings.parse("""
                {
                    "Display": {"Show scale bar": true, "Show axis": false, "Show title": false, "Show canvas size": true,
                        "Title text": "Custom", "Branch length unit": "MYA", "Reverse axis": true},
                    "Labels": {"Show leaf names": false, "Right align leaf labels": false, "Show internal node names": true,
                        "Show bootstrap values": true, "Show branch lengths": true},
                    "Root": {"Show root branch": true, "Root branch length": 25.5},
                    "Fonts": {"Labels": {"Family": "Dialog", "Size": 17.5, "Style": "Bold"},
                        "Title": {"Family": "Dialog", "Size": 18, "Style": "Italic"},
                        "Axis and scale bar": {"Family": "Monospaced", "Size": 12, "Style": "Bold Italic"}},
                    "Layout": {"Type": "Spiral beta phylogram", "Margins": {"Top": 30, "Left": 45, "Bottom": 90, "Right": 50}},
                    "Rectangular layout": {"Branch curvature": 30},
                    "Circular layout": {"Start angle": 90, "Arc extent": 270, "Inner radius": 60},
                    "Spiral layout": {"Start angle": 35, "Total angle": 900, "Arm spacing": 15, "Beta factor": 1.75},
                    "Slant layout": {"Tree width": 80, "Left margin": 25, "Rotation": 90},
                    "Radial layout": {"Rotation": 45}, "Mouse wheel": {"Zoom height": false, "Zoom width": false}
                }
                """);
        TreeSettings.apply(properties, modified);
        Map<String, Object> actual = TreeSettings.explicitImport(null, properties);
        for (String key : modified.keySet()) require(equivalent(modified.get(key), actual.get(key)), "Applied setting: " + key + " expected " + modified.get(key) + " got " + actual.get(key));
        require(!properties.getLeaves().getFirst().getDrawUnit().isDrawName(), "Leaf visibility updates draw units");
        require(properties.getOriginalRootNode().getDrawUnit().isDrawName(), "Internal labels update draw units");
        require(TreeSettings.changes(Map.of("Labels/Show leaf names", true), Map.of()).containsKey("Labels/Show leaf names"), "Deleting an explicit default still resets the current tree");
    }

    private static TreeSettingsPreferences testStorage() throws Exception {
        Path directory = Files.createTempDirectory(OUTPUT, "preferences-");
        Path path = directory.resolve("user.jsonc");
        TreeSettingsPreferences preferences = new TreeSettingsPreferences(path);
        require(preferences.loadAsync().get(5, TimeUnit.SECONDS).values().isEmpty(), "Missing file uses defaults");
        String text = "{\r\n    // 保留我的注释\r\n    \"Labels\": {\"Show leaf names\": false,},\r\n}\r\n";
        preferences.saveAsync(text).get(5, TimeUnit.SECONDS);
        require(Files.readString(path).equals(text), "Save preserves comments, indentation and line endings");
        require(new TreeSettingsPreferences(path).loadAsync().get(5, TimeUnit.SECONDS).text().equals(text), "Restart reloads the original text");
        var removal = preferences.saveAsync("{}").get(5, TimeUnit.SECONDS);
        require(Boolean.TRUE.equals(removal.changes().get("Labels/Show leaf names")), "Removing an override restores the default");
        String before = Files.readString(path);
        try { preferences.saveAsync("{broken}"); throw new AssertionError("Saved invalid JSON"); }
        catch (IllegalArgumentException expected) { }
        require(Files.readString(path).equals(before), "Validation failure leaves the file unchanged");
        Files.writeString(path, "{/* broken");
        TreeSettingsPreferences damaged = new TreeSettingsPreferences(path);
        var state = damaged.loadAsync().get(5, TimeUnit.SECONDS);
        require(state.values().isEmpty() && state.warning() != null && state.text().equals("{/* broken"), "Damaged preferences preserve repair text and use defaults");
        require(damaged.claimWarning() && !damaged.claimWarning(), "Warn once for damaged preferences");
        require(Files.readString(path).equals("{/* broken"), "Loading never rewrites a damaged file");
        damaged.saveAsync("{}").get(5, TimeUnit.SECONDS);
        Path blocked = directory.resolve("blocked");
        Files.writeString(blocked, "keep this file");
        TreeSettingsPreferences unwritable = new TreeSettingsPreferences(blocked.resolve("user.jsonc"));
        var previous = unwritable.loadAsync().get(5, TimeUnit.SECONDS);
        try { unwritable.saveAsync(text).get(5, TimeUnit.SECONDS); throw new AssertionError("Saved through a non-directory"); }
        catch (java.util.concurrent.ExecutionException expected) { }
        require(unwritable.loadAsync().get().equals(previous) && Files.readString(blocked).equals("keep this file"), "Save failure preserves storage and cached state");
        preferences.saveAsync("""
                {
                    // Global preferences should not override explicit import parameters.
                    "Labels": {"Show leaf names": false, "Show bootstrap values": true},
                    "Layout": {"Type": "Circular phylogram"},
                    "Spiral layout": {"Start angle": 35}
                }
                """).get(5, TimeUnit.SECONDS);
        return preferences;
    }

    private static void testEditor() throws Exception {
        String raw = "{\n    // My notes\n    \"Labels\": {\"Show bootstrap values\": true}\n}\n";
        TreeSettingsEditor panel = new TreeSettingsEditor(raw);
        JTextPane left = (JTextPane) field(TreeSettingsEditor.class, "defaults").get(panel);
        JTextPane right = (JTextPane) field(TreeSettingsEditor.class, "user").get(panel);
        require(!left.isEditable() && right.isEditable(), "Read-only defaults and editable user pane");
        left.select(0, 20);
        require(left.getSelectedText() != null, "Defaults can be selected for copying");
        left.setCaretPosition(0);
        require(panel.text().equals(raw), "Unedited user text is preserved");
        int comment = left.getText().indexOf("//");
        int key = left.getText().indexOf("\"Display\"");
        require(!StyleConstants.getForeground(left.getStyledDocument().getCharacterElement(comment).getAttributes())
                .equals(StyleConstants.getForeground(left.getStyledDocument().getCharacterElement(key).getAttributes())), "Comments and keys are highlighted differently");
        panel.setSize(1200, 760);
        layout(panel);
        BufferedImage image = new BufferedImage(1200, 760, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", OUTPUT.resolve("settings-editor.png").toFile());
        right.setCaretPosition(right.getText().indexOf("//"));
        int length = right.getDocument().getLength();
        right.getActionMap().get("four-spaces").actionPerformed(new ActionEvent(right, ActionEvent.ACTION_PERFORMED, "Tab"));
        require(right.getDocument().getLength() == length + 4 && !right.getText().contains("\t"), "Tab inserts four spaces");
        String windowsText = raw.replace("\n", "\r\n");
        TreeSettingsEditor windowsPanel = new TreeSettingsEditor(windowsText);
        JTextPane windowsUser = (JTextPane) field(TreeSettingsEditor.class, "user").get(windowsPanel);
        String documentText = windowsUser.getDocument().getText(0, windowsUser.getDocument().getLength());
        require(windowsPanel.text().equals(windowsText), "Opening preserves Windows line endings");
        require(StyleConstants.getForeground(windowsUser.getStyledDocument().getCharacterElement(documentText.indexOf("\"Labels\"")).getAttributes())
                .equals(StyleConstants.getForeground(left.getStyledDocument().getCharacterElement(key).getAttributes())),
                "Windows line endings do not shift syntax highlighting");
    }

    private static void testLiveApply() {
        TreeLayoutProperties properties = properties();
        PhylogeneticTreePanel tree = new PhylogeneticTreePanel(properties);
        JViewport viewport = new JViewport();
        viewport.setSize(1000, 700);
        viewport.setView(tree);
        tree.initializeLeftPanel();
        tree.setTreePanelScale(150, 180);
        Dimension scale = tree.getTreePanelScale();
        TreeSettings.applyToTree(tree, TreeSettings.parse("{\"Labels\":{\"Show leaf names\":false},\"Display\":{\"Title text\":\"Changed\"}}"));
        require(tree.getTreePanelScale().equals(scale) && !properties.getShowLeafPropertiesInfo().isShowLeafLabel(), "Apply preserves zoom and updates labels");
        TreeSettings.applyToTree(tree, TreeSettings.parse("{\"Labels\":{\"Show leaf names\":true},\"Layout\":{\"Margins\":{\"Top\":35}}}"));
        int right = properties.getBlankArea().getRight();
        TreeSettings.applyToTree(tree, TreeSettings.parse("{\"Layout\":{\"Margins\":{\"Top\":36}}}"));
        require(properties.getBlankArea().getRight() == right && properties.getTitleString().equals("Changed"),
                "Margin edits do not accumulate label reserves or reset unrelated settings");
        properties.setCreativeMode(true);
        TreeSettings.applyToTree(tree, TreeSettings.parse("{\"Layout\":{\"Type\":\"Circular phylogram\"},\"Labels\":{\"Show leaf names\":true}}"));
        require(!properties.isCreativeMode() && properties.getShowLeafPropertiesInfo().isShowLeafLabel(), "Layout changes obey creative restrictions and apply requested labels");
    }

    private static JScrollPane scroll(JTextPane pane) {
        return (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, pane);
    }

    private static void requireScrollProgress(JScrollBar source, JScrollBar target) {
        int sourceRange = source.getMaximum() - source.getMinimum() - source.getVisibleAmount();
        int targetRange = target.getMaximum() - target.getMinimum() - target.getVisibleAmount();
        int expected = target.getMinimum() + (int) Math.round(
                (source.getValue() - source.getMinimum()) * (double) targetRange / sourceRange);
        require(Math.abs(target.getValue() - expected) <= 1, "Peer scrollbar follows proportional progress");
    }

    private static void requireLineNumbersFollow(JTextPane editor) {
        JScrollPane scroll = scroll(editor);
        JViewport header = scroll.getRowHeader();
        require(header.getViewPosition().y == scroll.getViewport().getViewPosition().y,
                "Line numbers scroll at the same vertical coordinate as the text");
        require(header.getView().getPreferredSize().height == editor.getPreferredSize().height,
                "Line-number height covers the complete document");
        int y = scroll.getViewport().getViewPosition().y;
        if (y > editor.getFontMetrics(editor.getFont()).getHeight() * 2) {
            int first = editor.getDocument().getDefaultRootElement()
                    .getElementIndex(editor.viewToModel2D(new Point(0, y)));
            require(first > 0, "Scrolled line numbers use document rows beyond the first line");
        }
    }

    private static void snapshot(JComponent component, String name) throws Exception {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        component.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", OUTPUT.resolve(name + ".png").toFile());
    }

    private static void verifyTooltip(JComponent component, String name) throws Exception {
        require(component.getToolTipText().startsWith("<html>"), "Tooltip uses formatted HTML: " + name);
        JToolTip tooltip = component.createToolTip();
        tooltip.setTipText(component.getToolTipText());
        tooltip.setSize(tooltip.getPreferredSize());
        require(tooltip.getWidth() >= 390 && tooltip.getWidth() <= 480, "Tooltip has a readable bounded width: " + name);
        require(tooltip.getHeight() > 32, "Tooltip displays multiple lines: " + name);
        require(tooltip.getFont().equals(UIManager.getFont("ToolTip.font")), "Tooltip uses the host font");
        snapshot(tooltip, name);
    }

    private static void testEditorScroll() throws Exception {
        TreeSettingsEditor editor = new TreeSettingsEditor(TreeSettings.documentText(TreeSettings.defaults())
                + "\n// " + "Extra wide comment ".repeat(30) + "\n// Extra line\n");
        JTextPane left = (JTextPane) field(TreeSettingsEditor.class, "defaults").get(editor);
        JTextPane right = (JTextPane) field(TreeSettingsEditor.class, "user").get(editor);
        editor.setSize(900, 600);
        layout(editor);
        layout(editor);
        for (JScrollBar[] pair : new JScrollBar[][] {
                {scroll(left).getVerticalScrollBar(), scroll(right).getVerticalScrollBar()},
                {scroll(left).getHorizontalScrollBar(), scroll(right).getHorizontalScrollBar()}}) {
            for (int direction = 0; direction < 2; direction++) {
                JScrollBar source = pair[direction], target = pair[1 - direction];
                int range = source.getMaximum() - source.getMinimum() - source.getVisibleAmount();
                require(range > 0, "Fixture has scrollable content");
                for (double progress : new double[] {0.37, 1, 0}) {
                    source.setValue(source.getMinimum() + (int) Math.round(range * progress));
                    requireScrollProgress(source, target);
                    requireLineNumbersFollow(left);
                    requireLineNumbersFollow(right);
                    if (source == scroll(left).getVerticalScrollBar())
                        snapshot(editor, "settings-lines-" + (progress == 0 ? "top" : progress == 1 ? "bottom" : "middle"));
                }
            }
        }
        right.setText("{}");
        layout(editor);
        layout(editor);
        scroll(left).getVerticalScrollBar().setValue(100);
        require(scroll(right).getVerticalScrollBar().getValue() == 0,
                "A short document stays at its only valid scroll position");
        int narrow = scroll(right).getRowHeader().getView().getPreferredSize().width;
        right.setText("// Row\n".repeat(1001));
        editor.setSize(1050, 700);
        layout(editor);
        layout(editor);
        require(scroll(right).getRowHeader().getView().getPreferredSize().width > narrow,
                "Line-number column grows for four-digit document rows");
        scroll(right).getVerticalScrollBar().setValue(1000);
        requireLineNumbersFollow(right);
        right.setText("{}\n");
        layout(editor);
        layout(editor);
        require(scroll(right).getRowHeader().getView().getPreferredSize().width == narrow,
                "Deleting lines restores the narrower line-number column");
        requireLineNumbersFollow(right);
    }

    private record EditorView(int dot, int mark, Point left, Point right) {
        static EditorView capture(JTextPane left, JTextPane right) {
            return new EditorView(right.getCaret().getDot(), right.getCaret().getMark(),
                    scroll(left).getViewport().getViewPosition(), scroll(right).getViewport().getViewPosition());
        }
    }

    private static void testApplyView(TreeSettingsPreferences preferences) throws Exception {
        if (GraphicsEnvironment.isHeadless()) return;
        var dialogs = new module.evolview.moderntreeviewer.para.TreeSettingsDialog[1];
        TreeSettingsEditor[] editors = new TreeSettingsEditor[1];
        JTextPane[] panes = new JTextPane[2];
        PhylogeneticTreePanel[] trees = new PhylogeneticTreePanel[1];
        EditorView[] before = new EditorView[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    trees[0] = new PhylogeneticTreePanel(properties());
                    JViewport viewport = new JViewport();
                    viewport.setSize(1000, 700);
                    viewport.setView(trees[0]);
                    trees[0].initializeLeftPanel();
                    dialogs[0] = new module.evolview.moderntreeviewer.para.TreeSettingsDialog(new JPanel(), preferences,
                            new TreeSettingsPreferences.State("{}", Map.of(), null), trees[0], () -> trees[0]);
                    editors[0] = (TreeSettingsEditor) ((BorderLayout) dialogs[0].getContentPane().getLayout())
                            .getLayoutComponent(BorderLayout.CENTER);
                    panes[0] = (JTextPane) field(TreeSettingsEditor.class, "defaults").get(editors[0]);
                    panes[1] = (JTextPane) field(TreeSettingsEditor.class, "user").get(editors[0]);
                    Map<String, Object> values = new LinkedHashMap<>(TreeSettings.snapshot(trees[0]));
                    values.put("Labels/Show leaf names", false);
                    editors[0].setText(TreeSettings.documentText(values) + "\n// Draft comment\n");
                    layout(dialogs[0]);
                    layout(dialogs[0]);
                    int position = panes[1].getText().indexOf("\"Labels\"");
                    panes[1].getCaret().setDot(position + 8);
                    panes[1].getCaret().moveDot(position);
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            // First Apply refreshes canonical text; second Apply repeats unchanged text;
            // invalid input must leave the editor alone.
            for (int attempt = 0; attempt < 3; attempt++) {
                if (attempt == 2) SwingUtilities.invokeAndWait(() -> panes[1].replaceSelection("invalid JSON"));
                SwingUtilities.invokeAndWait(() -> {
                    JScrollPane right = scroll(panes[1]);
                    right.getVerticalScrollBar().setValue(700);
                    right.getHorizontalScrollBar().setValue(20);
                    before[0] = EditorView.capture(panes[0], panes[1]);
                    require(before[0].right().y > 0 && before[0].right().x > 0,
                            "Apply fixture is scrolled on both axes");
                    buttons(dialogs[0].getContentPane()).stream().filter(b -> "Apply".equals(b.getText()))
                            .findFirst().orElseThrow().doClick();
                });
                CountDownLatch highlighted = new CountDownLatch(1);
                SwingUtilities.invokeAndWait(() -> {
                    javax.swing.Timer timer = new javax.swing.Timer(180, event -> highlighted.countDown());
                    timer.setRepeats(false);
                    timer.start();
                });
                require(highlighted.await(5, TimeUnit.SECONDS), "Editor highlighting settles");
                SwingUtilities.invokeAndWait(() -> {
                    layout(dialogs[0]);
                    require(EditorView.capture(panes[0], panes[1]).equals(before[0]),
                            "Apply preserves caret direction, selection and both viewports after deferred layout");
                    requireLineNumbersFollow(panes[0]);
                    requireLineNumbersFollow(panes[1]);
                    require(!trees[0].getLayoutProperties().getShowLeafPropertiesInfo().isShowLeafLabel(),
                            "Apply updates the actual tree");
                });
            }
            SwingUtilities.invokeAndWait(() -> editors[0].setTextPreservingView("{}"));
            SwingUtilities.invokeAndWait(() -> {
                layout(dialogs[0]);
                require(panes[1].getCaret().getDot() <= 2 && panes[1].getCaret().getMark() <= 2,
                        "Shorter refreshed text clamps selection to valid offsets");
                require(scroll(panes[1]).getViewport().getViewPosition().equals(new Point()),
                        "Shorter refreshed text clamps scrolling to its valid range");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (dialogs[0] != null) dialogs[0].dispose(); });
        }
    }

    private static OrganizedParameterGetter input(Map<String, String> values) throws Exception {
        Constructor<OrganizedParameterGetter> constructor = OrganizedParameterGetter.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        Map<String, LinkedList<String>> entries = new LinkedHashMap<>();
        values.forEach((key, value) -> entries.put("$" + key, new LinkedList<>(List.of("$" + key + "=" + value))));
        return constructor.newInstance(entries);
    }

    private static void testSnapshotSession() {
        TreeLayoutProperties p = properties();
        PhylogeneticTreePanel tree = new PhylogeneticTreePanel(p);
        JViewport viewport = new JViewport();
        viewport.setSize(1000, 700); viewport.setView(tree); tree.initializeLeftPanel();
        tree.setTreePanelScale(175, 220);
        GraphicsNode a = p.getLeaves().get(0), b = p.getLeaves().get(1);
        a.getDrawUnit().setCircleRadius(3);
        b.getDrawUnit().setCircleRadius(6);
        a.getDrawUnit().setLineColor(Color.RED);
        b.getDrawUnit().setLineColor(Color.BLUE);
        var session = new TreeSettingsSession(tree, () -> tree, Map.of());
        String originalTitle = p.getTitleString();
        String misspelled = session.text().replace("\"Title text\"", "\"Ttile text\"");
        try { session.apply(misspelled); throw new AssertionError("Accepted misspelled parameter"); }
        catch (IllegalArgumentException expected) {
            require(expected.getMessage().contains("Display/Ttile text") && p.getTitleString().equals(originalTitle), "Spelling errors identify the parameter path and leave the tree unchanged");
        }
        require(TreeSettings.parse(session.text()).get("Node style/Node radius").equals("Mixed"), "Mixed radius is visible");
        require(TreeSettings.parse(session.text()).get("View/Width scale").equals(220), "Snapshot uses actual view zoom");
        int[] events = {0};
        tree.addPropertyChangeListener("treeViewState", e -> events[0]++);
        Dimension zoom = tree.getTreePanelScale();
        int right = p.getConfiguredBlankArea().getRight();
        for (int i = 0; i < 3; i++) session.apply(session.text());
        require(events[0] == 0 && zoom.equals(tree.getTreePanelScale()), "Unchanged Apply neither notifies nor changes zoom");
        session.apply(session.text().replace("\"Title text\": \"" + p.getTitleString() + "\"", "\"Title text\": \"New title\""));
        require(a.getDrawUnit().getCircleRadius() == 3 && b.getDrawUnit().getCircleRadius() == 6 && b.getDrawUnit().getLineColor().equals(Color.BLUE), "An unrelated edit preserves mixed styles");
        require(right == p.getConfiguredBlankArea().getRight(), "Snapshots exclude automatic label reserve");
        p.getSelectedNodes().add(a);
        session = new TreeSettingsSession(tree, () -> tree, Map.of());
        session.apply(session.text().replace("\"Node radius\": 3", "\"Node radius\": 9"));
        require(a.getDrawUnit().getCircleRadius() == 9 && b.getDrawUnit().getCircleRadius() == 6, "Selected node scope does not alter unselected nodes");
        p.getSelectedNodes().clear();
        session = new TreeSettingsSession(tree, () -> tree, Map.of());
        session.apply(session.text().replace("\"Scope\": \"Selected clades\"", "\"Scope\": \"Leaves only\"").replace("\"Node radius\": \"Mixed\"", "\"Node radius\": 4"));
        require(a.getDrawUnit().getCircleRadius() == 4 && b.getDrawUnit().getCircleRadius() == 4 && p.getOriginalRootNode().getDrawUnit().getCircleRadius() == 0, "Leaf-only scope preserves internal nodes");
        session = new TreeSettingsSession(tree, () -> tree, Map.of());
        session.apply(session.text().replace("\"Lock aspect ratio\": false", "\"Lock aspect ratio\": true").replace("\"Height scale\": 175", "\"Height scale\": 200"));
        require(p.isLockAspectRatio() && tree.getTreePanelScale().height == 200, "Scale and aspect ratio apply to actual state");
        PhylogeneticTreePanel[] active = {tree};
        var stale = new TreeSettingsSession(tree, () -> active[0], Map.of());
        active[0] = new PhylogeneticTreePanel(properties());
        try { stale.apply(stale.text()); throw new AssertionError("Applied stale draft"); }
        catch (IllegalArgumentException expected) { require(!stale.canApply(), "Old tree draft is rejected"); }
        var empty = new TreeSettingsSession(null, () -> null, Map.of("View/Height scale", 300));
        require(!empty.canApply() && empty.values(empty.text()).get("View/Height scale").equals(300), "No-tree defaults remain editable");
        p.setMyLayout(TreeLayout.OUTER_CIR_PHYLO);
        try { TreeSettings.applyToTree(tree, Map.of("Creative mode/Enabled", true, "Display/Title text", "Wrong")); throw new AssertionError("Enabled unsupported creator"); }
        catch (IllegalArgumentException expected) { require(!p.getTitleString().equals("Wrong"), "Invalid combination does not partially apply"); }
        p.setAxisFontInherited(true); p.setGlobalFont(new Font("Dialog", Font.BOLD, 18));
        require(p.getAxisFont().equals(p.getGlobalFont()), "Inherited axis font follows labels");
        Font axis = new Font("Monospaced", Font.ITALIC, 11);
        p.setAxisFont(axis); p.setGlobalFont(new Font("Serif", Font.PLAIN, 20));
        require(p.getAxisFont().equals(axis), "Explicit axis font stays independent");
    }

    private static void testReusableDefaults() throws Exception {
        Path path = Files.createTempDirectory(OUTPUT, "reusable-").resolve("user.jsonc");
        String original = "{\r\n    // Keep this note and URL https://example.test/a/*b*/\r\n    \"Display\": { \"Title text\" : \"old\" /* keep inline */ },\r\n    \"Node style\": { \"Node radius\": 7 },\r\n}\r\n";
        Files.writeString(path, original);
        var prefs = new TreeSettingsPreferences(path);
        prefs.loadAsync().get(5, TimeUnit.SECONDS);
        Map<String, Object> values = new LinkedHashMap<>(TreeSettings.defaults());
        values.put("Display/Title text", "URL https://example.test/a/*b*/");
        values.put("Node style/Node radius", "Mixed");
        values.put("Creative mode/Enabled", true);
        prefs.saveDefaultsAsync(values).get(5, TimeUnit.SECONDS);
        String saved = Files.readString(path);
        require(saved.contains("// Keep this note") && saved.contains("/* keep inline */") && saved.contains("\"Title text\" : \"URL https://example.test/a/*b*/\""), "Path updates preserve comments, strings and spacing");
        Map<String, Object> parsed = TreeSettings.parse(saved);
        require(parsed.get("Node style/Node radius").equals(7) && !parsed.containsKey("Creative mode/Enabled") && !parsed.containsKey("Node style/Scope"), "Mixed and transient state are not persisted");
        require(new TreeSettingsPreferences(path).loadAsync().get(5, TimeUnit.SECONDS).text().equals(saved), "Restart preserves saved text");
    }

    private static void testVoiceSchema() throws Exception {
        var parser = new module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView();
        var o = input(Map.of("view.height.scale", "250", "view.width.scale", "320", "view.lock.aspect.ratio", "T", "node.color", "#12AB34", "node.radius", "5", "node.line.width", "2.5", "layout.spiral.start.degree", "45", "creative.mode", "T", "font.axis", ""));
        TreeLayoutProperties p = properties();
        var values = TreeSettings.explicitImport(o, p);
        TreeSettings.apply(p, values);
        require(p.getInitialHeightScale() == 250 && p.getInitialWidthScale() == 320 && p.isLockAspectRatio() && p.isCreativeMode(), "New VOICE view settings share the schema");
        require(p.isAxisFontInherited() && p.getSprialLayoutPropertiy().getGlobalStartDegree() == 45 && p.getLeaves().getFirst().getDrawUnit().getCircleRadius() == 5, "VOICE axis inheritance and node styling");
        for (var setting : TreeSettings.voiceSettings()) require(parser.getRequiredParams().containsKey(setting.key()), "VOICE schema key: " + setting.key());
        var voice = allocate(VOICE4MTV.class);
        String template = voice.getExampleText();
        require(template.contains("# $view.height.scale.percent=") && template.contains("# $creative.mode.enabled=") && template.contains("\n$input.newick.text=")
                && template.contains("\n$input.newick.format=") && template.contains("\n$input.node.styles.file="), "Fresh templates use canonical names, explicit example inputs and comment unrelated parameters");
        p.setAxisFont(new Font("Monospaced", Font.BOLD, 12));
        require(TreeSettings.explicitImport(input(Map.of()), p).isEmpty(), "Absent VOICE keys create no overrides");
        TreeSettings.apply(p, TreeSettings.explicitImport(input(Map.of("font.axis", "")), p));
        require(p.isAxisFontInherited(), "Explicit empty font.axis overrides an independent preference");
        Path annotations = Files.createTempFile(OUTPUT, "node-styles-", ".tsv");
        Files.writeString(annotations, "name\tfillColor\tcircleRadius\tlineThickness\nHuman\t#FF00FF\t8\t4\n");
        var bean = new module.evolview.moderntreeviewer.io.MTVImportInforBean();
        bean.setNodeVisualAnnotationConfigFilePath(annotations.toString());
        new module.evolview.moderntreeviewer.io.TreePropertiesAssigner().assignGraphicsNodeEffects(p.getOriginalRootNode(), bean);
        require(p.getLeaves().getFirst().getDrawUnit().getCircleRadius() == 8 && p.getLeaves().get(1).getDrawUnit().getCircleRadius() == 5,
                "Per-node annotations override uniform styles only on matching nodes");
    }

    private static void testDialogFonts(TreeSettingsPreferences preferences) throws Exception {
        var launch = egps2.UnifiedAccessPoint.getLaunchProperty();
        Font oldTitle = launch.getDialogTitleFont(), oldContent = launch.getDialogContentFont(), oldButton = launch.getDialogButtonFont();
        Font title = new Font("Serif", Font.BOLD, 19), content = new Font("Monospaced", Font.PLAIN, 13), button = new Font("Dialog", Font.ITALIC, 15);
        launch.setDialogTitleFont(title); launch.setDialogContentFont(content); launch.setDialogButtonFont(button);
        try {
            TreeSettingsEditor editor = new TreeSettingsEditor(TreeSettings.documentText(TreeSettings.defaults()));
            JTextPane left = (JTextPane) field(TreeSettingsEditor.class, "defaults").get(editor);
            JTextPane right = (JTextPane) field(TreeSettingsEditor.class, "user").get(editor);
            require(left.getFont().equals(content) && right.getFont().equals(content), "Editors use Dialog content font exactly");
            verifyEditorFonts(editor, title, content);
            verifyTooltip(left, "tooltip-defaults");
            verifyTooltip(right, "tooltip-editor");
            if (!GraphicsEnvironment.isHeadless()) {
                var dialog = new module.evolview.moderntreeviewer.para.TreeSettingsDialog(new JPanel(), preferences,
                        new TreeSettingsPreferences.State("{}", Map.of(), null), null, () -> null);
                try {
                    require(dialog.isModal() && dialog.getTitle().equals("Advanced parameters"), "Advanced parameters stays modal");
                    for (JButton b : buttons(dialog.getContentPane())) {
                        if (b.getText() != null && !b.getText().isBlank())
                            verifyTooltip(b, "tooltip-" + b.getText().replaceAll("[^a-zA-Z]+", "-"));
                        if (List.of("Apply", "Save as user defaults…", "Close").contains(b.getText())) require(b.getFont().equals(button), "Dialog button font: " + b.getText());
                        if (b.getText().equals("Apply")) {
                            require(!b.isEnabled() && b.getToolTipText().contains("Load a tree"), "Disabled Apply explains why a tree is required");
                            require(b.getMargin().left >= 18 && b.getMargin().top >= 6, "Buttons have explicit text padding");
                        }
                    }
                    Container pane = dialog.getContentPane();
                    pane.setSize(1250, 790); layout(pane);
                    BufferedImage image = new BufferedImage(1250, 790, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics(); pane.printAll(graphics); graphics.dispose();
                    ImageIO.write(image, "png", OUTPUT.resolve("advanced-dialog-fonts.png").toFile());
                } finally { dialog.dispose(); }
                var damaged = new module.evolview.moderntreeviewer.para.TreeSettingsDialog(new JPanel(), preferences,
                        new TreeSettingsPreferences.State("{/* broken", Map.of(), "Invalid <node> & value"), null, () -> null);
                try {
                    require(buttons(damaged.getContentPane()).stream().anyMatch(b -> "Repair user defaults…".equals(b.getText()) && b.isEnabled()),
                            "Damaged user defaults expose the repair editor");
                    require(buttons(damaged.getContentPane()).stream().anyMatch(b -> "Save as user defaults…".equals(b.getText()) && !b.isEnabled()),
                            "Damaged user defaults cannot be overwritten by a snapshot");
                    for (JButton b : buttons(damaged.getContentPane()))
                        if (b.getText() != null && !b.getText().isBlank())
                            verifyTooltip(b, "tooltip-damaged-" + b.getText().replaceAll("[^a-zA-Z]+", "-"));
                    JPanel bottom = (JPanel) ((BorderLayout) damaged.getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
                    JTextArea warning = (JTextArea) ((BorderLayout) bottom.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                    require(warning.getToolTipText().contains("Invalid &lt;node&gt; &amp; value"),
                            "Dynamic error text is escaped before HTML rendering");
                    verifyTooltip(warning, "tooltip-error");
                } finally { damaged.dispose(); }
            }
        } finally {
            launch.setDialogTitleFont(oldTitle); launch.setDialogContentFont(oldContent); launch.setDialogButtonFont(oldButton);
        }
    }

    private static void testRepairDialog() throws Exception {
        if (GraphicsEnvironment.isHeadless()) return;
        Path path = Files.createTempDirectory(OUTPUT, "repair-dialog-").resolve("user.jsonc");
        Files.writeString(path, "{/* broken");
        TreeSettingsPreferences preferences = new TreeSettingsPreferences(path);
        TreeSettingsPreferences.State damaged = preferences.loadAsync().get(5, TimeUnit.SECONDS);
        require(damaged.warning() != null, "Damaged defaults are detected before repair");
        SwingUtilities.invokeAndWait(() -> {
            var dialog = new module.evolview.moderntreeviewer.para.TreeSettingsDialog(new JPanel(), preferences, damaged, null, () -> null);
            try {
                JButton repair = buttons(dialog.getContentPane()).stream()
                        .filter(b -> "Repair user defaults…".equals(b.getText())).findFirst().orElseThrow();
                Throwable[] repairFailure = new Throwable[1];
                SwingUtilities.invokeLater(() -> {
                    try {
                        JDialog child = Arrays.stream(Window.getWindows()).filter(w -> w instanceof JDialog d
                                && d.isVisible() && "Repair user defaults".equals(d.getTitle())).map(w -> (JDialog) w).findFirst().orElseThrow();
                        TreeSettingsEditor editor = (TreeSettingsEditor) ((BorderLayout) child.getContentPane().getLayout())
                                .getLayoutComponent(BorderLayout.CENTER);
                        require(editor.text().equals("{/* broken"), "Repair editor shows the original damaged text");
                        JTextPane input = (JTextPane) field(TreeSettingsEditor.class, "user").get(editor);
                        verifyTooltip(input, "tooltip-repair-editor");
                        for (JButton b : buttons(child.getContentPane()))
                            if (b.getText() != null && !b.getText().isBlank())
                                verifyTooltip(b, "tooltip-repair-" + b.getText().replaceAll("[^a-zA-Z]+", "-"));
                        input.setText("{\"Labels\":{\"Show leaf names\":false}}\n");
                        buttons(child.getContentPane()).stream().filter(b -> "Save repaired defaults".equals(b.getText()))
                                .findFirst().orElseThrow().doClick();
                    } catch (Throwable error) {
                        repairFailure[0] = error;
                        for (Window child : dialog.getOwnedWindows()) child.dispose();
                    }
                });
                repair.doClick();
                if (repairFailure[0] != null) throw new AssertionError("Repair dialog interaction failed", repairFailure[0]);
                require(buttons(dialog.getContentPane()).stream().anyMatch(b -> "Save as user defaults…".equals(b.getText()) && b.isEnabled()),
                        "Successful repair enables saving reusable defaults");
                TreeSettingsEditor current = (TreeSettingsEditor) ((BorderLayout) dialog.getContentPane().getLayout())
                        .getLayoutComponent(BorderLayout.CENTER);
                require(Boolean.FALSE.equals(TreeSettings.parse(current.text()).get("Labels/Show leaf names")),
                        "No-tree parameter snapshot refreshes after repairing user defaults");
            } finally { dialog.dispose(); }
        });
        require(Files.readString(path).equals("{\"Labels\":{\"Show leaf names\":false}}\n"),
                "Repair writes the corrected JSONC to the original path");
    }

    private static void verifyEditorFonts(Container parent, Font title, Font content) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel label && label.getText() != null) require(c.getFont().equals(label.getText().contains("—") ? title : content), "Heading and explanation fonts");
            if (c.getClass().getSimpleName().equals("LineNumbers")) require(c.getFont().equals(content), "Line number font");
            if (c instanceof Container child) verifyEditorFonts(child, title, content);
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        return type.cast(allocator.getMethod("allocateInstance", Class.class).invoke(field(allocator, "theUnsafe").get(null), type));
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) if (component instanceof Container child) layout(child);
    }

    private static void testImport(TreeSettingsPreferences preferences) throws Exception {
        MainFace face = allocate(MainFace.class);
        ModernTreeController controller = allocate(ModernTreeController.class);
        field(ModernTreeController.class, "mainFace").set(controller, face);
        field(GeneFamilyController.class, "treeControlsListener").set(controller, (PropertyChangeListener) event -> controller.syncTreeControlsFromModel());
        face.controller = controller;
        face.treeSettingsPreferences = preferences;
        SwingUtilities.invokeAndWait(() -> {
            face.scrollPane = new JScrollPane();
            face.scrollPane.getViewport().setSize(1000, 700);
            face.creativeModeTaskPanel = new CreativeModeTaskPanel();
        });
        VOICE4MTV importer = allocate(VOICE4MTV.class);
        field(VOICE4MTV.class, "mTreeViewMainFace").set(importer, face);
        importer.execute(input(Map.of("input.nwk.string", "(Human:2,Chimpanzee:5);", "layout.initial", "RECT_PHYLO_LEFT", "leaf.show.label", "T",
                "view.height.scale", "300", "view.width.scale", "250", "view.lock.aspect.ratio", "T", "node.color", "#12AB34", "node.line.width", "2.5", "node.radius", "5", "creative.mode", "T")));
        SwingUtilities.invokeAndWait(() -> {
            TreeLayoutProperties properties = face.phylogeneticTreePanel.getLayoutProperties();
            require(properties.getMyLayout() == TreeLayout.RECT_PHYLO_LEFT && properties.getShowLeafPropertiesInfo().isShowLeafLabel(), "Explicit import overrides preferences");
            require(properties.getShowInnerNodePropertiesInfo().isShowInternalNodeBootstrap() && properties.getSprialLayoutPropertiy().getGlobalStartDegree() == 35,
                    "Unspecified import settings retain user preferences");
            require(face.phylogeneticTreePanel.getPropertyChangeListeners("treeViewState").length == 1, "Imported tree controls stay bound");
            require(face.phylogeneticTreePanel.getTreePanelScale().equals(new Dimension(250, 300)) && properties.isLockAspectRatio() && properties.isCreativeMode(),
                    "Explicit VOICE zoom, lock and creator survive panel initialization");
        });
        PhylogeneticTreePanel old = face.phylogeneticTreePanel;
        importer.execute(input(Map.of("input.nwk.string", "(Bonobo:2,Gorilla:5);")));
        SwingUtilities.invokeAndWait(() -> {
            TreeLayoutProperties properties = face.phylogeneticTreePanel.getLayoutProperties();
            require(properties.getMyLayout() == TreeLayout.OUTER_CIR_PHYLO && !properties.getShowLeafPropertiesInfo().isShowLeafLabel(), "Reimport without explicit options uses preferences");
            require(old.getPropertyChangeListeners("treeViewState").length == 0, "Reimport releases old controls");
        });
    }

    private static void testDirectLaunch(TreeSettingsPreferences preferences) throws Exception {
        boolean hasOverrides = !preferences.loadAsync().get(5, TimeUnit.SECONDS).values().isEmpty();
        for (boolean suppliedProperties : new boolean[] {false, true}) {
            MainFace[] selected = new MainFace[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    MainFace face = allocate(MainFace.class);
                    selected[0] = face;
                    ModernTreeController controller = allocate(ModernTreeController.class);
                    field(ModernTreeController.class, "mainFace").set(controller, face);
                    field(GeneFamilyController.class, "treeControlsListener").set(controller,
                            (PropertyChangeListener) event -> controller.syncTreeControlsFromModel());
                    face.controller = controller;
                    face.treeSettingsPreferences = preferences;
                    field(MTreeViewMainFace.class, "str2numberMap").set(face, new HashMap<String, Integer>());
                    Method controls = MTreeViewMainFace.class.getDeclaredMethod("getLeftToolPane");
                    controls.setAccessible(true);
                    JPanel left = (JPanel) controls.invoke(face);
                    require(buttons(left).stream().anyMatch(b -> "Advanced parameters".equals(b.getText()) && b.isEnabled()),
                            "Tree settings is available before a tree is loaded");
                    JButton advanced = buttons(left).stream().filter(b -> "Advanced parameters".equals(b.getText())).findFirst().orElseThrow();
                    Color normal = advanced.getBackground();
                    advanced.getModel().setRollover(true);
                    require(!advanced.getBackground().equals(normal), "Advanced entry responds to hover");
                    advanced.getModel().setRollover(false);
                    require(advanced.getBackground().equals(normal), "Advanced entry returns to its normal color");
                    if (!suppliedProperties) {
                        left.setSize(420, 500);
                        layout(left);
                        BufferedImage image = new BufferedImage(420, 500, BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = image.createGraphics();
                        left.printAll(graphics);
                        graphics.dispose();
                        ImageIO.write(image, "png", OUTPUT.resolve("advanced-entry.png").toFile());
                    }
                    face.scrollPane = new JScrollPane();
                    face.scrollPane.getViewport().setSize(1000, 700);
                    TreeLayoutProperties explicit = properties();
                    explicit.setMyLayout(TreeLayout.RECT_PHYLO_ALIGN2TIP);
                    face.loader = new IndependentModuleLoader();
                    face.loader.setModuleData(explicit.getOriginalRootNode(), suppliedProperties ? explicit : null);
                    face.initializeGraphics();
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            SwingUtilities.invokeAndWait(() -> {
                TreeLayoutProperties actual = selected[0].phylogeneticTreePanel.getLayoutProperties();
                TreeLayout expected = suppliedProperties ? TreeLayout.RECT_PHYLO_ALIGN2TIP
                        : hasOverrides ? TreeLayout.OUTER_CIR_PHYLO : TreeLayout.RECT_PHYLO_LEFT;
                require(actual.getMyLayout() == expected, "Direct-launch precedence, supplied properties = " + suppliedProperties);
                require(actual.getShowLeafPropertiesInfo().isShowLeafLabel() == (suppliedProperties || !hasOverrides),
                        "Direct-launch label precedence, supplied properties = " + suppliedProperties);
            });
        }
    }

    private static List<JButton> buttons(Container parent) {
        List<JButton> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button) result.add(button);
            if (child instanceof Container nested) result.addAll(buttons(nested));
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        testParserAndMapping();
        TreeSettingsPreferences preferences = testStorage();
        testReusableDefaults();
        testVoiceSchema();
        testRepairDialog();
        SwingUtilities.invokeAndWait(() -> {
            try { testEditor(); testEditorScroll(); testLiveApply(); testSnapshotSession(); testDialogFonts(preferences); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
        testApplyView(preferences);
        testImport(preferences);
        testDirectLaunch(preferences);
        testDirectLaunch(new TreeSettingsPreferences(Files.createTempDirectory(OUTPUT, "defaults-").resolve("user.jsonc")));
        LoggerFactory.getLogger(TreeSettingsRegressionTest.class).info("TreeSettingsRegressionTest passed");
    }
}
