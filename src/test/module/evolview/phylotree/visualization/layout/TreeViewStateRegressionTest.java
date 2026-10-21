package test.module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.*;
import module.evolview.moderntreeviewer.io.*;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import module.evolview.phylotree.visualization.graphics.phylogeny.PhyloGraphicsTreeEncoderDecoder;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import module.evolview.phylotree.visualization.layout.*;
import module.evolview.phylotree.visualization.util.TreeConversionUtil;
import module.remnant.treeoperator.DefaultNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TreeViewStateRegressionTest {
    private static final String OUTPUT = "temp/tree-view-state/";
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

    private static GraphicsNode parse(String newick, int format) throws Exception {
        MTVImportInforBean input = new MTVImportInforBean();
        input.setInput_nwk_string(newick); input.setNwkFormat(format);
        return new TreeParser4MTV().parseTree(input).orElseThrow();
    }

    private static GraphicsNode example() throws Exception {
        return parse("((Human:1,Chimpanzee:1)99.5:1,(Bonobo:1,(Gorilla:1,Orangutan:1)0:1)95:1)100:0;", 0);
    }

    private static class Panel extends PhylogeneticTreePanel {
        int calculations, refreshes;
        Panel(TreeLayoutProperties properties) { super(properties); }
        @Override public void updatePhylogeneticTreePanel() { calculations++; super.updatePhylogeneticTreePanel(); }
        @Override public void refreshViewPort() { refreshes++; super.refreshViewPort(); }
    }

    public static class Gene extends GeneFamilyController {
        Panel selected;
        TreeLayoutProperties global;
        module.evolview.gfamily.work.gui.CtrlTreeLayoutPanel layoutControls;
        module.evolview.gfamily.work.gui.CtrlTreeOperationPanelByMiglayout operations;
        public Gene() { super(null); }
        @Override public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() { return selected; }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return global; }
        @Override public module.evolview.gfamily.work.gui.CtrlTreeLayoutPanel getTreeLayoutPanel() { return layoutControls; }
        @Override public module.evolview.gfamily.work.gui.CtrlTreeOperationPanelByMiglayout getLeftOparetionPanel() { return operations; }
    }

    public static class Pathway extends PathwayBrowserController {
        Panel selected;
        TreeLayoutProperties global;
        module.evolview.pathwaybrowser.gui.CtrlTreeLayoutPanel layoutControls;
        module.evolview.pathwaybrowser.gui.CtrlTreeOperationPanelByMiglayout operations;
        public Pathway() { super(null); }
        @Override public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() { return selected; }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return global; }
        @Override public module.evolview.pathwaybrowser.gui.CtrlTreeLayoutPanel getTreeLayoutPanel() { return layoutControls; }
        @Override public module.evolview.pathwaybrowser.gui.CtrlTreeOperationPanelByMiglayout getLeftOparetionPanel() { return operations; }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field singleton = allocator.getDeclaredField("theUnsafe"); singleton.setAccessible(true);
        return type.cast(allocator.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type));
    }

    private static void field(Object object, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }

    private static List<JToggleButton> toggles(Container container) {
        List<JToggleButton> result = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JToggleButton button) result.add(button);
            else if (child instanceof Container nested) result.addAll(toggles(nested));
        }
        return result;
    }

    private static Panel panel(TreeLayout initial) throws Exception {
        return panel(initial, true);
    }

    private static Panel panel(TreeLayout initial, boolean leafLabels) throws Exception {
        TreeLayoutProperties properties = new TreeLayoutProperties(example());
        properties.setMyLayout(initial); properties.setShowAxisBar(false); properties.setShowTitle(false);
        properties.setGlobalFont(new Font("Dialog", Font.PLAIN, 14));
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, leafLabels);
        Panel tree = new Panel(properties);
        JViewport viewport = new JViewport(); viewport.setSize(1000, 740); viewport.setView(tree);
        tree.initializeLeftPanel(); viewport.doLayout();
        return tree;
    }

    private static void assertLeafState(Panel tree, boolean visible) {
        require(tree.getLayoutProperties().getShowLeafPropertiesInfo().isShowLeafLabel() == visible, "Model leaf state");
        for (GraphicsNode leaf : tree.getLayoutProperties().getLeaves())
            require(leaf.getDrawUnit().isDrawName() == visible, "Per-leaf drawing state");
    }

    private static void screenshot(JComponent component, String name) throws Exception {
        Dimension preferred = component.getPreferredSize();
        component.setSize(Math.max(300, preferred.width), preferred.height + 8);
        layout(component);
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); component.printAll(graphics); graphics.dispose();
        ImageIO.write(image, "png", new File(OUTPUT + name + ".png"));
    }

    private static void layout(Container component) {
        component.doLayout();
        for (Component child : component.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    private static void testControls(boolean pathway) throws Exception {
        Panel tree = panel(TreeLayout.RECT_PHYLO_LEFT);
        JComponent operation;
        Runnable synchronize;
        java.util.function.Consumer<Panel> select;
        if (pathway) {
            Pathway controller = allocate(Pathway.class); controller.selected = tree; controller.global = tree.getLayoutProperties();
            field(controller, PathwayBrowserController.class, "treeControlsListener", (PropertyChangeListener) e -> controller.syncTreeControlsFromModel());
            controller.setLeftControlPanelContainner(allocate(module.evolview.pathwaybrowser.gui.ControlPanelContainer.class));
            controller.layoutControls = new module.evolview.pathwaybrowser.gui.CtrlTreeLayoutPanel();
            controller.operations = new module.evolview.pathwaybrowser.gui.CtrlTreeOperationPanelByMiglayout();
            controller.operations.setController(controller); controller.layoutControls.setController(controller);
            operation = controller.operations; synchronize = controller::syncTreeControlsFromModel;
            select = value -> controller.selected = value;
        } else {
            Gene controller = allocate(Gene.class); controller.selected = tree; controller.global = tree.getLayoutProperties();
            field(controller, GeneFamilyController.class, "treeControlsListener", (PropertyChangeListener) e -> controller.syncTreeControlsFromModel());
            controller.setLeftControlPanelContainner(allocate(module.evolview.gfamily.work.gui.ControlPanelContainer.class));
            controller.layoutControls = new module.evolview.gfamily.work.gui.CtrlTreeLayoutPanel();
            controller.operations = new module.evolview.gfamily.work.gui.CtrlTreeOperationPanelByMiglayout();
            controller.operations.setController(controller); controller.layoutControls.setController(controller);
            operation = controller.operations; synchronize = controller::syncTreeControlsFromModel;
            select = value -> controller.selected = value;
        }
        synchronize.run();
        List<JToggleButton> controls = toggles(operation);
        require(controls.size() == 8, "Eight display controls");
        // The visual order is scale, leaf, title, axis, W&H, internal names, length, bootstrap.
        JToggleButton leaf = controls.get(1), bootstrap = controls.get(7);
        int[] listeners = controls.stream().mapToInt(c -> c.getActionListeners().length).toArray();
        int computations = tree.calculations, refreshes = tree.refreshes;
        for (int i = 0; i < 20; i++) synchronize.run();
        require(tree.calculations == computations && tree.refreshes == refreshes, "Pure synchronization has zero drawing side effects");
        for (int i = 0; i < controls.size(); i++) require(controls.get(i).getActionListeners().length == listeners[i], "Listener counts remain fixed");

        tree.setTreePanelScale(175, 175);
        computations = tree.calculations;
        tree.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_ALPHA_PHYLO);
        assertLeafState(tree, false); require(!leaf.isSelected(), "Spiral default updates operation icon");
        require(tree.calculations == computations + 1, "Family change calculates once");
        computations = tree.calculations; refreshes = tree.refreshes;
        leaf.doClick();
        assertLeafState(tree, true);
        require(tree.calculations == computations + 1 && tree.refreshes == refreshes + 1, "One leaf click gives one calculation and refresh");
        require(tree.getTreePanelScale().equals(new Dimension(175, 175)), "Leaf toggle retains zoom");
        tree.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_BETA_CLADO);
        assertLeafState(tree, true);
        tree.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT);
        assertLeafState(tree, true);
        int rightMargin = tree.getLayoutProperties().getBlankArea().getRight();
        for (int repeat = 0; repeat < 10; repeat++) {
            tree.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_BETA_PHYLO);
            tree.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT);
        }
        require(tree.getLayoutProperties().getBlankArea().getRight() == rightMargin, "Layout round trips do not accumulate label margins");
        tree.setLeafLabelsVisible(false); tree.recalculatePreservingZoom();
        tree.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_ALPHA_PHYLO); assertLeafState(tree, true);
        tree.updateViewAccording2TreeLayout(TreeLayout.OUTER_CIR_PHYLO); assertLeafState(tree, false);

        computations = tree.calculations; refreshes = tree.refreshes;
        bootstrap.doClick();
        require(tree.getLayoutProperties().getShowInnerNodePropertiesInfo().isShowInternalNodeBootstrap(), "Bootstrap icon commits model");
        require(tree.calculations == computations + 1 && tree.refreshes == refreshes + 1,
                "Bootstrap click reflows label geometry and refreshes once");
        tree.getLayoutProperties().setShowTitle(true);
        tree.getLayoutProperties().setShowAxisBar(true);
        tree.getLayoutProperties().setShowWidthAndHeightString(true);
        tree.getLayoutProperties().setShowNodeBranchLength(true);
        tree.getLayoutProperties().getShowInnerNodePropertiesInfo().setShowInternalNodeLabel(true);
        tree.notifyTreeViewStateChanged();
        require(controls.get(2).isSelected() && controls.get(4).isSelected()
                && controls.get(5).isSelected() && controls.get(6).isSelected(), "All model flags populate icons");
        require(!controls.get(3).isEnabled() && !controls.get(3).isSelected() && tree.getLayoutProperties().isShowAxisBar(),
                "Unsupported axis icon is off while the stored preference remains enabled");
        tree.updateViewAccording2TreeLayout(TreeLayout.OUTER_CIR_PHYLO_ALIGNED);
        require(controls.get(3).isEnabled() && controls.get(3).isSelected(), "Supported axis restores preference");
        screenshot(operation, pathway ? "pathway-operation" : "gene-operation");

        tree.getLayoutProperties().setRootTipLength(200);
        tree.updateViewAccording2TreeLayout(TreeLayout.INNER_CIR_CLADO);
        Object layoutPanel = pathway ? ((Pathway) fieldOwner(operation)).layoutControls : ((Gene) fieldOwner(operation)).layoutControls;
        Field circularField = layoutPanel.getClass().getDeclaredField("subCirLayout"); circularField.setAccessible(true);
        Container circularPanel = (Container) circularField.get(layoutPanel);
        JCheckBox rootToggle = null;
        for (Component child : circularPanel.getComponents()) if (child instanceof JCheckBox check && check.getText().startsWith("Show root")) rootToggle = check;
        require(rootToggle != null && !rootToggle.isSelected(), "Root icon reads model");
        computations = tree.calculations; rootToggle.doClick();
        require(tree.calculations == computations + 1 && tree.getLayoutProperties().isShowRoot(), "Root visibility reflows once to reserve outside space");

        Panel other = panel(TreeLayout.RECT_PHYLO_LEFT);
        select.accept(other); synchronize.run();
        require(leaf.isSelected(), "New tree has separate leaf state");
        computations = other.calculations; refreshes = other.refreshes;
        tree.notifyTreeViewStateChanged();
        require(leaf.isSelected() && other.calculations == computations && other.refreshes == refreshes, "Old view events cannot change active view");
        leaf.doClick(); assertLeafState(other, false);
        assertLeafState(tree, false); // The old tree remains hidden.
        other.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_BETA_PHYLO); assertLeafState(other, false);
        select.accept(tree); synchronize.run();
        tree.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_BETA_PHYLO); assertLeafState(tree, true);
        select.accept(null); synchronize.run();
        for (JToggleButton control : controls) require(!control.isEnabled(), "No tree disables operations");
    }

    private static Object fieldOwner(Object panel) throws Exception {
        Class<?> type = panel.getClass().getSuperclass();
        Field field = type.getDeclaredField("controller"); field.setAccessible(true); return field.get(panel);
    }

    private static class Inner extends CircularInnerCladoAligned {
        Inner(TreeLayoutProperties properties, GraphicsNode root, TreeLayoutHost host) { super(properties, root, host); }
        Point2D center() { return new Point2D.Double(centerX, centerY); }
    }

    private static void testInnerRoot() throws Exception {
        Panel host = panel(TreeLayout.INNER_CIR_CLADO);
        TreeLayoutProperties properties = host.getLayoutProperties();
        properties.setBlankArea(new BlankArea(30, 75, 90, 25));
        GraphicsNode root = host.getRootNode();
        Inner inner = new Inner(properties, root, host);
        for (Dimension canvas : new Dimension[] {new Dimension(1000, 740), new Dimension(740, 1000)})
            for (int angle : new int[] {0, 90, 180, 285}) for (int length : new int[] {0, 10, 200}) {
                properties.setShowRoot(true); properties.setRootTipLength(length);
                properties.getCircularLayoutPropertiy().setGlobalStartDegree(angle);
                Dimension required = inner.getRequiredCanvasSize(canvas.width, canvas.height);
                inner.calculateForPainting(required.width, required.height);
                Point2D center = inner.center();
                double dx = root.getXParent() - root.getXSelf(), dy = root.getYParent() - root.getYSelf();
                require(Math.abs(Math.hypot(dx, dy) - length) < 1e-7, "Root length in pixels");
                require(dx * (root.getXSelf() - center.getX()) + dy * (root.getYSelf() - center.getY()) >= -1e-7, "Root points outwards");
                BlankArea blank = properties.getBlankArea();
                require(root.getXParent() >= blank.getLeft() && root.getXParent() <= required.width - blank.getRight()
                        && root.getYParent() >= blank.getTop() && root.getYParent() <= required.height - blank.getBottom(), "Root endpoint fits work area");
                if (canvas.width == 1000 && angle == 285 && length == 10) saveTree(inner, required, "inner-root-outward");
            }
        properties.setRootTipLength(0); properties.setShowRoot(true);
        BufferedImage zero = saveTree(inner, new Dimension(1000, 740), "inner-root-zero");
        properties.setShowRoot(false);
        BufferedImage hidden = saveTree(inner, new Dimension(1000, 740), "inner-root-hidden");
        for (int x = 0; x < zero.getWidth(); x++) for (int y = 0; y < zero.getHeight(); y++)
            require(zero.getRGB(x, y) == hidden.getRGB(x, y), "Zero-length root paints nothing");
    }

    private static void testData() throws Exception {
        GraphicsNode root = example();
        GraphicsNode first = (GraphicsNode) root.getChildAt(0);
        require(first.hasBootstrapValue() && first.getBootstrapValue() == 99.5 && first.isNameBootstrap(), "Parsed decimal support and source");
        GraphicsNode zero = (GraphicsNode) root.getChildAt(1).getChildAt(1);
        require(zero.hasBootstrapValue() && zero.getBootstrapValue() == 0, "Explicit zero support");
        GraphicsNode missing = parse("((A:1,B:1):.25,C:1):0;", 0);
        require(!missing.hasBootstrapValue() && !((GraphicsNode) missing.getChildAt(0)).hasBootstrapValue(), "Missing remains missing");
        GraphicsNode named = parse("((A:1,B:1)95:.25,C:1)Root:0;", 1);
        require(!((GraphicsNode) named.getChildAt(0)).hasBootstrapValue(), "Numeric format-1 name is a name");
        GraphicsNode strict = parse("((1:1,B:1)0:.25,C:1)95:0;", 2);
        require(((GraphicsNode) strict.getChildAt(0)).hasBootstrapValue() && !((GraphicsNode) strict.getChildAt(0).getChildAt(0)).hasBootstrapValue(), "Strict support versus numeric leaf");
        java.nio.file.Path newickPath = java.nio.file.Path.of(OUTPUT + "support-source.nwk");
        java.nio.file.Files.writeString(newickPath, "(A:1,B:1)0:0;");
        MTVImportInforBean fromFile = new MTVImportInforBean();
        fromFile.setInput_nwk_string(null); fromFile.setInput_nwk_path(newickPath.toString()); fromFile.setNwkFormat(0);
        require(new TreeParser4MTV().parseTree(fromFile).orElseThrow().hasBootstrapValue(), "File input retains explicit zero support");
        java.nio.file.Path tablePath = java.nio.file.Path.of(OUTPUT + "numeric-name-tree.tsv");
        java.nio.file.Files.writeString(tablePath, "95\tA,B\nA\t\nB\t\n");
        MTVImportInforBean fromTable = new MTVImportInforBean();
        fromTable.setInput_tableLike_path(tablePath.toString());
        GraphicsNode table = new TreeParser4MTV().parseTree(fromTable).orElseThrow();
        require("95".equals(table.getName()) && !table.hasBootstrapValue(), "Table numeric name is not support");
        GraphicsNode copied = NodeUtils.convertBasicNode2graphicsNode(root);
        require(((GraphicsNode) copied.getChildAt(0)).getBootstrapValue() == 99.5 && ((GraphicsNode) copied.getChildAt(0)).isNameBootstrap(), "Subtree conversion retains support");
        GraphicsNode cloned = GraphicTreePropertyCalculator.clone(root);
        require(((GraphicsNode) cloned.getChildAt(1).getChildAt(1)).hasBootstrapValue(), "Clone retains zero presence");
        first.setName("Real clade"); require(!first.isNameBootstrap() && first.getBootstrapValue() == 99.5, "Renaming clears only source marker");
        first.setBootstrap(95); require(first.getBootstrapValue() == 95, "Legacy setter maps to double");
        first.setBootstrapValue(99.5); require(first.getBootstrap() == 99, "Legacy integer getter compatibility");
        PhyloGraphicsTreeEncoderDecoder codec = new PhyloGraphicsTreeEncoderDecoder();
        DefaultGraphicsPhyloNode roundTrip = codec.decode(codec.encode(root));
        DefaultGraphicsPhyloNode decoded = (DefaultGraphicsPhyloNode) roundTrip.getChildAt(0);
        require(decoded.hasBootstrapValue() && decoded.getBootstrapValue() == 99.5 && decoded.getLength() == 1 && "Real clade".equals(decoded.getName()), "Name/support/length round trip");
        DefaultGraphicsPhyloNode namedRoundTrip = codec.decode(codec.encode(named));
        DefaultGraphicsPhyloNode numberName = (DefaultGraphicsPhyloNode) namedRoundTrip.getChildAt(0);
        require("95".equals(numberName.getName()) && !numberName.hasBootstrapValue() && numberName.getLength() == .25, "Numeric name with empty support round trip");
        DefaultGraphicsPhyloNode zeroRoundTrip = codec.decode(codec.encode(strict));
        require(((DefaultGraphicsPhyloNode) zeroRoundTrip.getChildAt(0)).hasBootstrapValue()
                && ((DefaultGraphicsPhyloNode) zeroRoundTrip.getChildAt(0)).getBootstrapValue() == 0, "Zero survives encoding");
        DefaultNode legacyRoot = new DefaultNode(), legacyInternal = new DefaultNode();
        legacyRoot.addChild(legacyInternal); legacyInternal.addChild(new DefaultNode()); legacyInternal.addChild(new DefaultNode());
        legacyInternal.setBs(1);
        GraphicsNode legacy = TreeConversionUtil.node4eGPSToGraphicsNode(legacyRoot, true);
        require(((GraphicsNode) legacy.getChildAt(0)).getBootstrapValue() == 100, "Known normalized 1 becomes 100");
        legacyInternal.setBs(0);
        require(((GraphicsNode) TreeConversionUtil.node4eGPSToGraphicsNode(legacyRoot, true).getChildAt(0)).hasBootstrapValue(), "Calculated zero is present");
        require(!((GraphicsNode) TreeConversionUtil.node4eGPSToGraphicsNode(legacyRoot, false).getChildAt(0)).hasBootstrapValue(), "No-bootstrap run has no fabricated zero");
    }

    private static BufferedImage saveTree(BaseLayout layout, Dimension size, String name) throws Exception {
        layout.calculateForPainting(size.width, size.height);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, size.width, size.height);
        AffineTransform original = graphics.getTransform(); layout.paintGraphics(graphics);
        require(original.equals(graphics.getTransform()), "Drawing restores transforms: " + name);
        graphics.dispose(); ImageIO.write(image, "png", new File(OUTPUT + name + ".png")); return image;
    }

    private static void testBootstrapDrawing() throws Exception {
        Panel tree = panel(TreeLayout.RECT_PHYLO_LEFT);
        TreeLayoutProperties properties = tree.getLayoutProperties();
        tree.setLeafLabelsVisible(false);
        List<BaseLayout> layouts = List.of(new RectPhyloLayout(properties, tree.getRootNode(), tree),
                new CircularPhylo(properties, tree.getRootNode(), tree), new SpiralPhyloWithBeta(properties, tree.getRootNode(), tree),
                new SlopeCladoAlignedLeft(properties, tree.getRootNode(), tree), new RadialPhyloLayout(properties, tree.getRootNode(), tree));
        for (BaseLayout layout : layouts) {
            Dimension size = layout.getRequiredCanvasSize(1000, 740);
            properties.getShowInnerNodePropertiesInfo().setShowInternalNodeBootstrap(false);
            BufferedImage hidden = saveTree(layout, size, layout.getClass().getSimpleName() + "-without-support");
            properties.getShowInnerNodePropertiesInfo().setShowInternalNodeBootstrap(true);
            BufferedImage shown = saveTree(layout, size, layout.getClass().getSimpleName() + "-with-support");
            int changed = 0;
            for (int x = 0; x < size.width; x++) for (int y = 0; y < size.height; y++) if (hidden.getRGB(x, y) != shown.getRGB(x, y)) changed++;
            require(changed > 20, "Bootstrap switch draws actual text without leaf labels: " + layout.getClass().getSimpleName());
        }
        GraphicsNode first = (GraphicsNode) tree.getRootNode().getChildAt(0);
        first.setName("Named clade"); first.getDrawUnit().setDrawName(true);
        properties.getShowInnerNodePropertiesInfo().setShowInternalNodeLabel(true);
        properties.setShowNodeBranchLength(true);
        BaseLayout slope = layouts.get(3);
        for (int rotation : new int[] {0, 90, 180, 270}) {
            properties.getSlopeLayoutProperety().setSlopeLayoutRotationDeg(rotation);
            saveTree(slope, new Dimension(1000, 740), "slope-all-annotations-" + rotation);
        }
        first.setCollapse(true);
        saveTree(layouts.get(0), new Dimension(1000, 740), "collapsed-support");
        tree.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT);
        tree.setSize(tree.getPreferredSize());
        egps2.utils.common.util.SaveUtil exporter = new egps2.utils.common.util.SaveUtil();
        exporter.directlyProduceFigureAccording2filePath(OUTPUT + "bootstrap-native.png", tree);
        exporter.directlyProduceFigureAccording2filePath(OUTPUT + "bootstrap-native.svg", tree);
        require(ImageIO.read(new File(OUTPUT + "bootstrap-native.png")) != null, "Native PNG export");
        String svg = java.nio.file.Files.readString(java.nio.file.Path.of(OUTPUT + "bootstrap-native.svg"));
        require(svg.contains("99.5") && svg.length() > 1000, "Native SVG contains bootstrap text");
    }

    public static void main(String[] args) throws Exception {
        new File(OUTPUT).mkdirs();
        testData();
        SwingUtilities.invokeAndWait(() -> {
            try {
                testControls(false); testControls(true); testInnerRoot(); testBootstrapDrawing();
                Panel loaded = panel(TreeLayout.SPRIAL_BETA_PHYLO, true); assertLeafState(loaded, true);
                loaded.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_ALPHA_CLADO); assertLeafState(loaded, true);
                Panel hidden = panel(TreeLayout.SPRIAL_ALPHA_PHYLO, false); assertLeafState(hidden, false);
                hidden.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT); assertLeafState(hidden, false);
            }
            catch (Exception error) { throw new RuntimeException(error); }
        });
        System.out.println("TreeViewStateRegressionTest passed");
    }
}
