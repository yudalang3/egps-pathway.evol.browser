package module.evolview.moderntreeviewer;

import com.alibaba.fastjson.JSON;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import egps2.utils.common.util.EGPSFonts;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.gui.ModernTreeOperationPanel;
import module.evolview.moderntreeviewer.gui.TreeParameterTooltips;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;
import module.evolview.moderntreeviewer.para.*;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Tests each definition through the dialog's real text editor and Apply action. */
public final class TreeParameterAuditTest {
    private static class Controls extends module.evolview.gfamily.GeneFamilyController {
        PhylogeneticTreePanel selected;
        module.evolview.gfamily.work.gui.CtrlTreeLayoutPanel layouts;
        ModernTreeOperationPanel operations;
        boolean syncing;
        Controls() { super(null); }
        @Override public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() { return selected; }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return selected == null ? null : selected.getLayoutProperties(); }
        @Override public void syncTreeControlsFromModel() {
            if (syncing) return;
            syncing = true;
            try { if (layouts != null) layouts.reInitializeGUIAccording2treeLayoutProperties(); if (operations != null) operations.reInitializeGUIAccording2treeLayoutProperties(); }
            finally { syncing = false; }
        }
        @Override public void fireLayoutChanged() { }
        @Override public void reCalculateTreeLayoutAndRefreshViewPort() { selected.recalculatePreservingZoom(); }
        @Override public void refreshPhylogeneticTree() { selected.notifyTreeViewStateChanged(); selected.refreshViewPort(); }
    }
    private static Controls bind(PhylogeneticTreePanel tree) {
        try {
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            var field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
            Controls c = (Controls) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), Controls.class);
            c.selected = tree;
            c.layouts = new module.evolview.gfamily.work.gui.CtrlTreeLayoutPanel();
            c.operations = new ModernTreeOperationPanel();
            c.layouts.setController(c); c.operations.setController(c);
            tree.addPropertyChangeListener("treeViewState", e -> c.syncTreeControlsFromModel());
            return c;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static final List<String> REPORT = new ArrayList<>(List.of("Setting,VOICE,Value,Layout,Validation"));
    private static final Path OUTPUT = Path.of("temp/tree-parameter-audit");
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static boolean equal(Object a, Object b) {
        return a instanceof Number x && b instanceof Number y ? Math.abs(x.doubleValue() - y.doubleValue()) < .0001 : Objects.equals(a, b);
    }
    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    private static Map<String, Object> voice(String key, String value) {
        return TreeSettings.explicitImport(new VoiceParameterParser().getOrganizedParameterGetter("$" + key + "=" + value), null);
    }
    private static TreeLayoutProperties properties() {
        GraphicsNode root = new GraphicsNode(); root.setName("Root"); root.setBootstrapValue(95);
        for (int i = 0; i < 3; i++) {
            GraphicsNode clade = new GraphicsNode(); clade.setName("Clade" + i); clade.setRealBranchLength(i + 1); clade.setBootstrapValue(80 + i);
            root.addChild(clade);
            for (int j = 0; j < 3; j++) {
                GraphicsNode leaf = new GraphicsNode(); leaf.setName("Taxon_" + "long".repeat(j) + i + j); leaf.setRealBranchLength(j + 1.5); clade.addChild(leaf);
            }
        }
        TreeLayoutProperties p = new TreeLayoutProperties(root);
        TreeSettings.apply(p, TreeSettings.defaults());
        return p;
    }
    private static PhylogeneticTreePanel tree(TreeLayout layout) {
        var p = properties(); p.setMyLayout(layout);
        PhylogeneticTreePanel tree = new PhylogeneticTreePanel(p);
        JViewport viewport = new JViewport(); viewport.setSize(1000, 720); viewport.setView(tree);
        tree.initializeLeftPanel(); return tree;
    }
    private static void await(PhylogeneticTreePanel tree) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        boolean[] pending = {true};
        while (pending[0] && System.nanoTime() < until) {
            edt(() -> pending[0] = tree.isLayoutCalculationPending());
            if (pending[0]) Thread.sleep(20);
        }
        require(!pending[0], "Layout calculation completed");
    }
    private static Object alternative(TreeSettings.Definition d) {
        String path = d.path(); Object initial = d.initial();
        if (initial instanceof Boolean b) return !b;
        if (path.equals("Layout/Type")) return "Rectangular phylogram (aligned tips)";
        if (path.endsWith("/Family")) return initial.equals("Monospaced") ? "Serif" : "Monospaced";
        if (path.endsWith("/Style")) return initial.equals("Bold Italic") ? "Regular" : "Bold Italic";
        if (path.endsWith("/Scope")) return "Leaves only";
        if (path.endsWith("/Color")) return "#2357AB";
        if (path.equals("Display/Title text")) return "Audit tree with {0} leaves";
        if (path.equals("Display/Branch length unit")) return "MYA";
        if (path.endsWith("/Size")) return 18.0;
        if (path.equals("Node style/Branch line width")) return 3.25;
        if (path.equals("Node style/Node radius")) return 5;
        if (path.equals("Root/Root branch length")) return 35.5;
        if (path.startsWith("View/")) return 150;
        if (path.startsWith("Layout/Margins/")) return ((Number) initial).intValue() + 35;
        if (path.equals("Spiral layout/Beta factor")) return 1.75;
        if (path.equals("Spiral layout/Total angle")) return 900;
        if (path.equals("Spiral layout/Arm spacing")) return 18;
        if (path.equals("Circular layout/Arc extent")) return 270;
        if (path.equals("Circular layout/Inner radius")) return 85;
        if (path.equals("Slant layout/Tree width")) return 75.0;
        if (path.equals("Slant layout/Left margin")) return 30.0;
        if (path.equals("Slant layout/Rotation")) return 90;
        return 45;
    }
    private static String legacyValue(TreeSettings.Definition d, Object value, String key) {
        if (List.of("font.global", "tree.global.font", "font.title", "bottom.title.font", "font.axis").contains(key)) {
            String family = d.path().endsWith("/Family") ? value.toString() : "Monospaced";
            int style = d.path().endsWith("/Style") && value.equals("Regular") ? Font.PLAIN : Font.BOLD | Font.ITALIC;
            return EGPSFonts.codeFont(new Font(family, style, 18));
        }
        if (key.equals("layout.blank.space") || key.equals("blank.space")) {
            int[] margins = {20, 40, 80, 40};
            int side = List.of("Top", "Left", "Bottom", "Right").indexOf(d.path().substring(d.path().lastIndexOf('/') + 1));
            margins[side] = ((Number) value).intValue();
            return margins[0] + "," + margins[1] + "," + margins[2] + "," + margins[3];
        }
        if (key.equals("layout.spiral.beta.factor")) return Integer.toString((int) Math.round(((Number) value).doubleValue() * 100));
        if (value instanceof Boolean b) return b ? "T" : "F";
        return value.toString();
    }
    private static void schema() throws Exception {
        Set<String> keys = new HashSet<>(); int aliases = 0;
        var parser = new ParamsAssignerAndParser4ModernTreeView();
        for (var d : TreeSettings.definitions()) {
            require(keys.add(d.voice()), "Unique canonical key: " + d.voice());
            require(parser.getRequiredParams().containsKey(d.voice()), "Template key: " + d.voice());
            Object value = alternative(d);
            require(equal(voice(d.voice(), value.toString()).get(d.path()), value), "Canonical value: " + d.voice());
            for (String alias : d.aliases()) {
                require(equal(voice(alias, legacyValue(d, value, alias)).get(d.path()), value), "Legacy alias: " + alias + " -> " + d.path());
                aliases++;
            }
        }
        for (var setting : TreeSettings.inputSettings()) {
            String sample = setting.key().endsWith(".format") ? "1" : setting.key().endsWith("whitespace") ? "true" : "example";
            for (String alias : setting.aliases()) {
                var input = new VoiceParameterParser().getOrganizedParameterGetter("$" + alias + "=" + sample);
                require(TreeSettings.inputValue(input, setting.key()).equals(sample), "Import alias: " + alias);
                aliases++;
            }
        }
        String conflict = "$tree.global.font=Serif,0,10\n$font.global=Monospaced,1,12\n$font.labels.size.pt=19.5\n$font.axis=\n$font.axis.size.pt=21\n$font.axis.inherit.label.font=true\n$layout.spiral.beta.factor=200\n$layout.spiral.beta=1.75\n$blank.space=1,2,3,4\n$layout.blank.space=5,6,7,8\n$layout.margins.top.px=60";
        var values = TreeSettings.explicitImport(new VoiceParameterParser().getOrganizedParameterGetter(conflict), null);
        require(equal(values.get("Fonts/Labels/Size"), 19.5) && values.get("Fonts/Labels/Family").equals("Monospaced"), "Scalar > current alias > historical alias");
        require(equal(values.get("Layout/Margins/Top"), 60) && equal(values.get("Layout/Margins/Left"), 6), "Margins override per field");
        require(equal(values.get("Spiral layout/Beta factor"), 1.75) && Boolean.TRUE.equals(values.get("Fonts/Axis and scale bar/Inherit label font")), "Beta and explicit inheritance precedence");
        var fractional = parser.generateTreeFromKeyValue(new VoiceParameterParser().getOrganizedParameterGetter("$layout.slant.tree.width.percent=75.5\n$font.labels.size.pt=17.5"));
        require(fractional.getDefaultFont().getSize2D() == 17.5f, "Fractional fonts survive public parser");
        TreeLayoutProperties assigned = properties();
        new module.evolview.moderntreeviewer.io.TreePropertiesAssigner().assign(assigned, fractional);
        require(equal(assigned.getSlopeLayoutProperety().getLeafLocationRightMostRatio() * 100, 75.5), "Public Bean-to-model assignment preserves canonical fractional values");
        for (String name : List.of("Rectangular phylogram", "RECT_PHYLO_LEFT", "RECTANGULAR"))
            require(voice("layout.type", name).get("Layout/Type").equals("Rectangular phylogram"), "Layout spelling: " + name);
        try { voice("layout.type", "Rectangluar"); throw new AssertionError("Unknown layout accepted"); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains("layout.type"), "Layout error identifies key"); }
        try { TreeSettings.parse("{\"Display\":{\"Title text\":\"{bad}\"}}"); throw new AssertionError("Invalid title accepted"); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains("Display/Title text"), "Title template validates before paint"); }
        LoggerFactory.getLogger(TreeParameterAuditTest.class).info("Validated {} canonical display keys and {} legacy aliases", keys.size(), aliases);
    }
    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) result.add(type.cast(c));
            if (c instanceof Container child) result.addAll(components(child, type));
        }
        return result;
    }
    private static JButton button(Container c, String text) {
        return components(c, JButton.class).stream().filter(b -> text.equals(b.getText()) || text.equals(b.getName())).findFirst().orElseThrow();
    }
    private static BufferedImage render(JComponent component) {
        component.setSize(component.getPreferredSize()); layout(component);
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); component.printAll(g); g.dispose(); return image;
    }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container container) layout(container); }
    private static long hash(BufferedImage image) {
        long result = image.getWidth() * 31L + image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) result = result * 31 + image.getRGB(x, y);
        return result;
    }
    private static TreeLayout applicable(String path) {
        if (path.equals("Circular layout/Inner radius")) return TreeLayout.INNER_CIR_CLADO;
        if (path.startsWith("Circular layout/")) return TreeLayout.OUTER_CIR_PHYLO;
        if (path.startsWith("Spiral layout/")) return TreeLayout.SPRIAL_BETA_PHYLO;
        if (path.startsWith("Slant layout/")) return TreeLayout.SLOPE_CLADO_ALIGNED_LEFT;
        if (path.startsWith("Radial layout/")) return TreeLayout.RADIAL_PHYLO;
        return TreeLayout.RECT_PHYLO_LEFT;
    }
    private static void dialogMatrix(TreeSettingsPreferences prefs) throws Exception {
        Set<String> tested = new HashSet<>();
        for (var d : TreeSettings.definitions()) {
            PhylogeneticTreePanel[] tree = {null}; TreeSettingsDialog[] dialog = {null}; Controls[] controls = {null};
            long[] before = {0};
            edt(() -> { tree[0] = tree(applicable(d.path())); if (d.path().equals("Root/Root branch length")) { tree[0].getLayoutProperties().setShowRoot(true); tree[0].recalculatePreservingZoom(); } });
            await(tree[0]);
            Object value = alternative(d);
            edt(() -> {
                controls[0] = bind(tree[0]);
                before[0] = hash(render(tree[0]));
                dialog[0] = new TreeSettingsDialog(new JPanel(), prefs, prefs.loadAsync().join(), tree[0], () -> tree[0]);
                TreeSettingsEditor editor = components(dialog[0], TreeSettingsEditor.class).getFirst();
                Map<String, Object> draft = new LinkedHashMap<>(TreeSettings.snapshot(tree[0])); draft.put(d.path(), value);
                editor.setText(TreeSettings.documentText(draft)); button(dialog[0], "Apply").doClick();
                Object actual = TreeSettings.parse(editor.text()).get(d.path());
                require(equal(actual, value), "Dialog Apply and refreshed snapshot: " + d.path() + " expected=" + value + " actual=" + actual);
                require(components(dialog[0], JTextArea.class).stream().anyMatch(a -> a.getText().equals("Applied to the current tree.")), "Apply success feedback: " + d.path());
            });
            await(tree[0]);
            edt(() -> {
                long after = hash(render(tree[0]));
                boolean behaviorOnly = d.path().startsWith("Mouse wheel/") || d.path().equals("View/Lock aspect ratio")
                        || d.path().equals("Node style/Scope") || d.path().equals("Creative mode/Enabled") || d.path().endsWith("Inherit label font");
                require(behaviorOnly || before[0] != after, "Visible rendering effect: " + d.path());
                if (!d.path().equals("Node style/Scope")) require(equal(TreeSettings.snapshot(tree[0]).get(d.path()), value), "Reopened snapshot: " + d.path());
                for (Container group : List.of(controls[0].layouts, controls[0].operations)) {
                    TreeParameterTooltips.install(group, true);
                    for (JComponent control : components(group, JComponent.class)) {
                        if (!d.path().equals(control.getClientProperty("tree.parameter"))) continue;
                        require(control.getToolTipText() != null && control.getToolTipText().contains(d.voice()), "GUI tooltip canonical key: " + d.path());
                        if (control instanceof JSpinner spinner) {
                            double expected = ((Number) value).doubleValue() * (d.path().equals("Spiral layout/Beta factor") ? 100 : 1);
                            require(equal(spinner.getValue(), expected), "GUI numeric sync: " + d.path() + " got " + spinner.getValue());
                        } else if (control instanceof AbstractButton toggle && value instanceof Boolean b) require(toggle.isSelected() == b, "GUI toggle sync: " + d.path());
                    }
                }
                REPORT.add(csv(d.path()) + "," + csv(d.voice()) + "," + csv(value.toString()) + "," + csv(applicable(d.path()).name()) + "," + (behaviorOnly ? "Apply+state+controls" : "Apply+state+controls+render"));
                dialog[0].dispose();
            });
            tested.add(d.path());
        }
        require(tested.size() == TreeSettings.defaults().size(), "Every editable parameter was tested through Apply");
    }
    private static String csv(String text) { return "\"" + text.replace("\"", "\"\"") + "\""; }

    private static void margins() throws Exception {
        for (TreeLayout layout : TreeLayout.values()) {
            PhylogeneticTreePanel[] tree = {null};
            edt(() -> { tree[0] = tree(layout); tree[0].setTreePanelScale(140, 160); });
            await(tree[0]);
            for (String side : List.of("Top", "Left", "Bottom", "Right")) {
                var expected = new java.util.concurrent.atomic.AtomicReference<Map<String, Object>>(); long[] before = {0}; Dimension[] zoom = {null};
                edt(() -> {
                    zoom[0] = tree[0].getTreePanelScale();
                    before[0] = hash(render(tree[0]));
                    TreeSettingsSession session = new TreeSettingsSession(tree[0], () -> tree[0], Map.of());
                    expected.set(new LinkedHashMap<>(TreeSettings.parse(session.text())));
                    String path = "Layout/Margins/" + side;
                    expected.get().put(path, ((Number) expected.get().get(path)).intValue() + 35);
                    session.apply(TreeSettings.documentText(expected.get()));
                    for (int i = 0; i < 3; i++) session.apply(session.text());
                });
                await(tree[0]);
                edt(() -> {
                    Map<String, Object> actual = TreeSettings.snapshot(tree[0]);
                    for (String name : List.of("Top", "Left", "Bottom", "Right")) require(equal(expected.get().get("Layout/Margins/" + name), actual.get("Layout/Margins/" + name)), "No margin drift: " + layout + " " + side + " / " + name);
                    require(zoom[0].equals(tree[0].getTreePanelScale()), "Margins preserve zoom: " + layout);
                    require(before[0] != hash(render(tree[0])), "Margin affects canvas/coordinates: " + layout + " " + side);
                    REPORT.add(csv("Layout/Margins/" + side) + ",,," + csv(layout.name()) + ",repeat+zoom+render");
                });
            }
            if (layout.name().startsWith("RADIAL_")) {
                edt(() -> tree[0].updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT));
                await(tree[0]);
                edt(() -> tree[0].updateViewAccording2TreeLayout(layout));
                await(tree[0]);
                edt(() -> require(tree[0].getLayoutProperties().getConfiguredBlankArea().getTop() == 55, "Cached radial layout uses current margins: " + layout));
            }
        }
    }

    private static void behavior() throws Exception {
        PhylogeneticTreePanel[] tree = {null};
        edt(() -> tree[0] = tree(TreeLayout.RECT_PHYLO_LEFT));
        for (boolean height : new boolean[]{false, true}) for (boolean width : new boolean[]{false, true}) {
            Dimension[] before = {null};
            edt(() -> {
                TreeSettings.applyToTree(tree[0], Map.of("Mouse wheel/Zoom height", height, "Mouse wheel/Zoom width", width));
                before[0] = tree[0].getTreePanelScale();
                tree[0].getTreeListener().mouseWheelMoved(new java.awt.event.MouseWheelEvent(tree[0], java.awt.event.MouseEvent.MOUSE_WHEEL,
                        System.currentTimeMillis(), 0, 300, 250, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1));
            });
            Thread.sleep(140);
            edt(() -> {
                Dimension after = tree[0].getTreePanelScale();
                require((after.height > before[0].height) == height && (after.width > before[0].width) == width, "Wheel behavior follows both flags");
            });
        }
        edt(() -> {
            tree[0].setTreePanelScale(200, 300);
            TreeSettings.applyToTree(tree[0], Map.of("View/Lock aspect ratio", true, "View/Height scale", 240));
            require(tree[0].getTreePanelScale().equals(new Dimension(360, 240)), "Advanced single-axis zoom obeys lock");
            TreeSettings.applyToTree(tree[0], Map.of("View/Height scale", 160, "View/Width scale", 240));
            require(tree[0].getTreePanelScale().equals(new Dimension(240, 160)), "Two explicit axes establish new proportion");
            var p = tree[0].getLayoutProperties();
            p.setAxisFontInherited(true);
            TreeSettings.applyToTree(tree[0], Map.of("Fonts/Axis and scale bar/Inherit label font", false));
            Font axis = p.getAxisFont(); p.setGlobalFont(new Font("Serif", Font.BOLD, 22));
            require(p.getAxisFont().equals(axis), "Disabled inheritance remains independent");
            TreeSettings.applyToTree(tree[0], Map.of("Fonts/Axis and scale bar/Inherit label font", true));
            require(p.getAxisFont().equals(p.getGlobalFont()), "Enabled inheritance follows labels");
            GraphicsNode clade = (GraphicsNode) p.getOriginalRootNode().getChildAt(0);
            p.getSelectedNodes().add(clade);
            TreeSettings.applyToTree(tree[0], Map.of("Node style/Scope", "Leaves only", "Node style/Node radius", 7));
            require(clade.getDrawUnit().getCircleRadius() == 0 && p.getLeaves().getFirst().getDrawUnit().getCircleRadius() == 7
                    && p.getLeaves().getLast().getDrawUnit().getCircleRadius() == 0, "Leaves-only respects selected clade");
            Controls controls = bind(tree[0]);
            p.setShowRoot(true); tree[0].notifyTreeViewStateChanged();
            int index = 0;
            for (JSpinner spinner : components(controls.layouts, JSpinner.class)) {
                if ("Root/Root branch length".equals(spinner.getName())) {
                    double length = 41.5 + index++;
                    spinner.setValue(length);
                    require(equal(TreeSettings.snapshot(tree[0]).get("Root/Root branch length"), length), "Each root-length GUI preserves fractional edits");
                }
            }
            require(index == 3, "Rectangular, circular and spiral root editors exercised");
            tree[0].updateViewAccording2TreeLayout(TreeLayout.SLOPE_CLADO_ALIGNED_LEFT);
            for (JSpinner spinner : components(controls.layouts, JSpinner.class)) if ("Slant layout/Tree width".equals(spinner.getName())) {
                spinner.setValue(75.5);
                require(equal(TreeSettings.snapshot(tree[0]).get("Slant layout/Tree width"), 75.5), "Slant GUI edit appears in Advanced parameters snapshot");
            }
        });
    }

    private static void select(PhylogeneticTreePanel tree, GraphicsNode node) {
        try {
            var method = tree.getTreeListener().getClass().getDeclaredMethod("controlPressSelect", GraphicsNode.class);
            method.setAccessible(true); method.invoke(tree.getTreeListener(), node);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void operations() throws Exception {
        PhylogeneticTreePanel[] tree = {null}; Controls[] controls = {null};
        edt(() -> { tree[0] = tree(TreeLayout.RECT_PHYLO_LEFT); controls[0] = bind(tree[0]); });
        Controls c = controls[0];
        java.awt.datatransfer.Clipboard clipboard = new java.awt.datatransfer.Clipboard("isolated tree action test");
        edt(() -> {
            c.operations = operationPanel(text -> clipboard.setContents(new java.awt.datatransfer.StringSelection(text), null), message -> { throw new AssertionError(message); });
            c.operations.setController(c);
        });
        {
            edt(() -> {
                require(!button(c.operations, "Center selected node").isEnabled() && !button(c.operations, "Copy subtree as Newick").isEnabled(), "Selection-dependent buttons start disabled");
                int[] columns = {0};
                for (String name : List.of("Clear selection", "Center selected node", "Copy tree as Newick", "Copy subtree as Newick")) {
                    JButton b = button(c.operations, name);
                    GridBagConstraints cell = ((GridBagLayout) c.operations.getLayout()).getConstraints(b);
                    require(cell.gridy == 2 && cell.gridx == columns[0]++, "Four actions share a complete row");
                    require(b.getIcon().getIconWidth() == 20 && b.getIcon().getIconHeight() == 20 && b.getToolTipText() != null, "Consistent action icons and help");
                }
                GraphicsNode first = tree[0].getLayoutProperties().getLeaves().getFirst(), last = tree[0].getLayoutProperties().getLeaves().getLast();
                select(tree[0], first); select(tree[0], last); select(tree[0], first);
                require(tree[0].getLayoutProperties().getSelectedNodes().equals(List.of(last)) && !first.getDrawUnit().isBranchSelected(), "Ctrl reselect removes only that node");
                select(tree[0], first);
                require(tree[0].getLayoutProperties().getSelectedNodes().size() == 2 && tree[0].getLayoutProperties().getSelectedNodes().getLast() == first, "Selecting again appends the active node without duplicates");
                tree[0].setTreePanelScale(220, 220); render(tree[0]);
                Dimension zoom = tree[0].getTreePanelScale();
                button(c.operations, "Center selected node").doClick();
                require(zoom.equals(tree[0].getTreePanelScale()), "Center preserves zoom");
                tree[0].getLayoutProperties().getLeaves().getFirst().getDrawUnit().setNodeSelected(true);
                button(c.operations, "Clear selection").doClick();
                require(tree[0].getLayoutProperties().getSelectedNodes().isEmpty() && !button(c.operations, "Center selected node").isEnabled(), "Clear syncs action state");
                require(!first.getDrawUnit().isNodeSelected() && !first.getDrawUnit().isBranchSelected(), "Clear includes search markers and branches");
                JButton fit = components(c.operations, JButton.class).stream().filter(b -> b.getToolTipText() != null && b.getToolTipText().startsWith("Fit the tree")).findFirst().orElseThrow();
                fit.doClick(); require(tree[0].getTreePanelScale().equals(new Dimension(100, 100)), "Fit frame resets zoom");
                first.setRealBranchLength(7.125);
                tree[0].updateViewAccording2TreeLayout(TreeLayout.RECT_CLADO_EQUAL_LEFT);
                first.setDisplayedBranchLength(0.5);
                require(first.getDisplayedBranchLength() != first.getRealBranchLength(), "Copy fixture distinguishes real and displayed branches");
                button(c.operations, "Copy tree as Newick").doClick();
            });
            waitCopy(c.operations);
            String full = clipboard.getData(DataFlavor.stringFlavor).toString();
            require(full.contains(":7.125") && full.contains("Taxon_20") && full.contains("Clade2"), "Whole-tree clipboard contains real lengths and unselected clades");
            edt(() -> {
                GraphicsNode clade = (GraphicsNode) tree[0].getRootNode().getChildAt(0);
                select(tree[0], clade);
                var snapshot = ModernTreeOperationPanel.exportSnapshot(clade, true);
                require(snapshot.getLength() == 0 && snapshot.getChildCount() == 3 && snapshot.getChildAt(0).getLength() == 7.125, "Subtree snapshot excludes incoming branch and retains real descendant lengths");
                button(c.operations, "Copy subtree as Newick").doClick();
            });
            waitCopy(c.operations);
            String subtree = clipboard.getData(DataFlavor.stringFlavor).toString();
            require(subtree.contains(":7.125") && !subtree.contains("Clade2"), "Subtree clipboard excludes other clades");
            edt(() -> {
                var search = new module.evolview.gfamily.work.gui.dialog.SearchDialogContent(c);
                search.addNotify();
                JTextField input = components(search, JTextField.class).stream().filter(JTextField::isEditable).findFirst().orElseThrow();
                input.setText("not-present"); button(search, "Find").doClick();
                require(!button(search, "Find Prev").isEnabled(), "No-match search has no previous result");
                input.setText("Taxon"); button(search, "Find").doClick();
                require(button(search, "Find Prev").isEnabled(), "Successful search can navigate");
                button(c.operations, "Clear selection").doClick();
                require(!button(search, "Find Prev").isEnabled(), "Clear resets open search results");
                search.removeNotify();
                TreeParameterTooltips.install(c.layouts, true); TreeParameterTooltips.install(c.operations, true);
                try {
                    c.operations.setPreferredSize(new Dimension(330, c.operations.getPreferredSize().height));
                    ImageIO.write(render(c.operations), "png", OUTPUT.resolve("tree-operation-four-actions.png").toFile());
                    BufferedImage high = new BufferedImage(c.operations.getWidth() * 2, c.operations.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = high.createGraphics(); g.scale(2, 2); c.operations.printAll(g); g.dispose();
                    ImageIO.write(high, "png", OUTPUT.resolve("tree-operation-2x.png").toFile());
                    ImageIO.write(render(c.layouts), "png", OUTPUT.resolve("tree-layout-tooltips.png").toFile());
                } catch (java.io.IOException error) { throw new AssertionError(error); }
                c.selected = null; c.syncTreeControlsFromModel();
                for (String name : List.of("Clear selection", "Center selected node", "Copy tree as Newick", "Copy subtree as Newick")) {
                    JButton b = button(c.operations, name);
                    require(!b.isEnabled() && b.getToolTipText().contains("Load a tree"), "No-tree action and reason: " + name);
                }
            });
        }

        java.util.concurrent.atomic.AtomicReference<String> error = new java.util.concurrent.atomic.AtomicReference<>();
        ModernTreeOperationPanel[] failurePanel = {null};
        edt(() -> {
            failurePanel[0] = operationPanel(text -> { throw new IllegalStateException("Clipboard is busy"); }, error::set);
            c.selected = tree[0]; failurePanel[0].setController(c);
            button(failurePanel[0], "Copy tree as Newick").doClick();
        });
        waitCopy(failurePanel[0]);
        require("Clipboard is busy".equals(error.get()), "Clipboard failure reports reason and restores action availability");
        nativeClipboard(tree[0], c);
    }

    private static ModernTreeOperationPanel operationPanel(java.util.function.Consumer<String> writer, java.util.function.Consumer<String> reporter) {
        try {
            var constructor = ModernTreeOperationPanel.class.getDeclaredConstructor(java.util.function.Consumer.class, java.util.function.Consumer.class);
            constructor.setAccessible(true);
            return constructor.newInstance(writer, reporter);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static void nativeClipboard(PhylogeneticTreePanel tree, Controls controls) throws Exception {
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        java.awt.datatransfer.Transferable original = null;
        IllegalStateException unavailable = null;
        for (int i = 0; i < 5; i++) {
            try { original = clipboard.getContents(null); unavailable = null; break; }
            catch (IllegalStateException busy) { unavailable = busy; Thread.sleep(100); }
        }
        if (unavailable != null) {
            REPORT.add(csv("Windows system clipboard") + ",,,," + csv("UNVERIFIED success: " + unavailable.getMessage() + "; isolated success and busy failure paths passed"));
            LoggerFactory.getLogger(TreeParameterAuditTest.class).warn("Native clipboard success could not be verified: {}. Isolated clipboard success and busy-error recovery passed.", unavailable.getMessage());
            return;
        }
        java.util.concurrent.atomic.AtomicReference<String> error = new java.util.concurrent.atomic.AtomicReference<>();
        ModernTreeOperationPanel[] panel = {null};
        try {
            edt(() -> {
                panel[0] = operationPanel(text -> clipboard.setContents(new java.awt.datatransfer.StringSelection(text), null), error::set);
                controls.selected = tree; panel[0].setController(controls);
                button(panel[0], "Copy tree as Newick").doClick();
            });
            waitCopy(panel[0]);
            require(error.get() == null && clipboard.getData(DataFlavor.stringFlavor).toString().contains(":7.125"), "Native clipboard contains real lengths");
            REPORT.add(csv("Windows system clipboard") + ",,,,native write+read passed; original clipboard restored");
        } finally {
            if (original != null) clipboard.setContents(original, null);
        }
    }

    private static void waitCopy(ModernTreeOperationPanel panel) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); boolean[] busy = {true};
        while (busy[0] && System.nanoTime() < deadline) {
            edt(() -> busy[0] = !button(panel, "Copy tree as Newick").isEnabled());
            if (busy[0]) Thread.sleep(20);
        }
        require(!busy[0], "Asynchronous copy completed");
    }

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--write-docs")) {
            Files.writeString(Path.of("docs/current/modern_tree_view_parameters.md"), TreeSettings.referenceMarkdown(false));
            Files.writeString(Path.of("docs/current/modern_tree_view_parameters_zh.md"), TreeSettings.referenceMarkdown(true));
            return;
        }
        Files.createDirectories(OUTPUT); Files.createDirectories(Path.of("logs"));
        schema();
        TreeSettingsPreferences prefs = new TreeSettingsPreferences(Files.createTempDirectory(OUTPUT, "preferences-").resolve("settings.jsonc"));
        prefs.loadAsync().get(5, TimeUnit.SECONDS);
        try { dialogMatrix(prefs); margins(); behavior(); operations(); }
        finally { Files.write(Path.of("logs/tree-parameter-matrix.csv"), REPORT); edt(() -> { for (Window w : Window.getWindows()) w.dispose(); }); }
        LoggerFactory.getLogger(TreeParameterAuditTest.class).info("TreeParameterAuditTest passed");
    }
}
