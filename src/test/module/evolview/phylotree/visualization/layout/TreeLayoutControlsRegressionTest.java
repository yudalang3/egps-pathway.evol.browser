package test.module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import module.evolview.phylotree.visualization.annotation.DrawPropInternalNode2LeafAnno;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Exercises real Swing events on both module panels without constructing the desktop mainframe. */
public class TreeLayoutControlsRegressionTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class Panel extends PhylogeneticTreePanel {
        boolean pending;
        int updates;
        Panel(TreeLayoutProperties properties) { super(properties); }
        @Override public void updatePhylogeneticTreePanel() { updates++; super.updatePhylogeneticTreePanel(); }
        @Override public boolean isLayoutCalculationPending() { return pending || super.isLayoutCalculationPending(); }
        void pending(boolean value) {
            boolean previous = pending;
            pending = value;
            firePropertyChange("layoutCalculationPending", previous, value);
        }
        @Override public void cancelLayoutCalculation() {
            pending(false);
            super.cancelLayoutCalculation();
        }
    }

    public static class GeneController extends GeneFamilyController {
        Panel tree;
        int recalculations;
        public GeneController() { super(null); }
        @Override public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() { return tree; }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return tree.getLayoutProperties(); }
        @Override public void reCalculateTreeLayoutPreservingZoom() { recalculations++; tree.recalculatePreservingZoom(); }
        @Override public void refreshPhylogeneticTree() { }
        @Override public void fireLayoutChanged() { }
    }

    public static class PathController extends PathwayBrowserController {
        Panel tree;
        int recalculations;
        public PathController() { super(null); }
        @Override public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() { return tree; }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return tree.getLayoutProperties(); }
        @Override public void reCalculateTreeLayoutPreservingZoom() { recalculations++; tree.recalculatePreservingZoom(); }
        @Override public void refreshPhylogeneticTree() { }
        @Override public void fireLayoutChanged() { }
    }

    // Controller constructors create the application JFrame. Allocate only the overridden test doubles.
    private static <T> T controller(Class<T> type) throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field field = allocator.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(allocator.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static Panel tree() {
        GraphicsNode root = TreeTipLayoutRegressionTest.example();
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        properties.setGlobalFont(new Font("Dialog", Font.PLAIN, 14));
        properties.setShowAxisBar(false);
        properties.setMyLayout(TreeLayout.RECT_PHYLO_LEFT);
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
        Panel panel = new Panel(properties);
        JViewport viewport = new JViewport();
        viewport.setSize(1100, 760);
        viewport.setView(panel);
        panel.initializeLeftPanel();
        viewport.doLayout();
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object owner, String name) throws Exception {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(owner);
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static List<Component> components(Container panel) {
        List<Component> result = new ArrayList<>();
        for (Component child : panel.getComponents()) {
            result.add(child);
            if (child instanceof Container container) result.addAll(components(container));
        }
        return result;
    }

    private static AbstractButton button(Container panel, String text) {
        return components(panel).stream().filter(c -> c instanceof AbstractButton b && text.equals(b.getText()))
                .map(c -> (AbstractButton) c).findFirst().orElseThrow(() -> new AssertionError("Missing button: " + text));
    }

    private static void bind(Object panel, Object controller, Class<?> controllerType) throws Exception {
        panel.getClass().getMethod("setController", controllerType).invoke(panel, controller);
    }

    private static void sync(Object panel) throws Exception {
        panel.getClass().getMethod("reInitializeGUIAccording2treeLayoutProperties").invoke(panel);
    }

    private static void screenshot(JComponent panel, String name) throws Exception {
        Dimension size = panel.getPreferredSize();
        panel.setSize(Math.max(290, size.width), size.height + 12);
        layout(panel);
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        panel.printAll(graphics); graphics.dispose();
        new File("temp/tree-layout-controls").mkdirs();
        ImageIO.write(image, "png", new File("temp/tree-layout-controls/" + name + ".png"));
    }

    private static void layout(Container panel) {
        panel.doLayout();
        for (Component component : panel.getComponents()) if (component instanceof Container child) layout(child);
    }

    private static void testModule(boolean pathway) throws Exception {
        Panel tree = tree();
        Object controller;
        Class<?> controllerType;
        String base;
        if (pathway) {
            PathController value = controller(PathController.class); value.tree = tree; controller = value;
            controllerType = PathwayBrowserController.class; base = "module.evolview.pathwaybrowser.gui";
        } else {
            GeneController value = controller(GeneController.class); value.tree = tree; controller = value;
            controllerType = GeneFamilyController.class; base = "module.evolview.gfamily.work.gui";
        }
        JComponent zoom = (JComponent) Class.forName(base + ".GUITreeZoomContainerInTopFixPanel").getConstructor().newInstance();
        bind(zoom, controller, controllerType); sync(zoom);
        JSpinner height = field(zoom, "spinnerVerticalOrWholeZoom");
        JSpinner width = field(zoom, "spinnerHorizontalChange");
        JCheckBox lock = field(zoom, "chckbxLockAspectRatio");
        Dimension fitted = tree.getTreePanelDimension();
        height.setValue(200);
        require(tree.getTreePanelDimension().height == 2 * fitted.height && tree.getTreePanelDimension().width == fitted.width, "Height only");
        Point position = ((JViewport) tree.getParent()).getViewPosition();
        require(Math.abs(position.y - fitted.height / 2.0) <= 1 && position.x == 0, "Manual zoom keeps viewport center anchored");
        width.setValue(150);
        lock.setSelected(true);
        height.setValue(300);
        require(width.getValue().equals(225), "Locked ratio preserves existing proportions");
        width.setValue(150);
        require(height.getValue().equals(200), "Locked reverse scaling");
        height.setValue(100);
        require(height.getValue().equals(133) && width.getValue().equals(100), "Locked ratio at minimum");
        height.setValue(200);
        width.setValue(150);
        tree.updatePhylogeneticTreePanel();
        require(tree.getTreePanelScale().equals(new Dimension(150, 200)), "Recalculation preserves zoom");
        tree.continuouslyZoomInOrOut(76, 0, new Point(200, 200));
        require(height.getValue().equals(tree.getTreePanelScale().height), "Wheel synchronizes spinner");
        tree.continuouslyZoomInOrOut(-100000, -100000, new Point(100, 100));
        require(tree.getTreePanelScale().equals(new Dimension(100, 100)), "Wheel minimum");
        tree.setTreePanelScale(200, 300);
        for (TreeLayout layout : new TreeLayout[] {TreeLayout.OUTER_CIR_PHYLO, TreeLayout.SPRIAL_ALPHA_PHYLO, TreeLayout.RADIAL_PHYLO}) {
            tree.updateViewAccording2TreeLayout(layout);
            require(tree.getTreePanelScale().equals(new Dimension(300, 300)), "Normalize uniform layout: " + layout);
            height.setValue(200);
            require(width.getValue().equals(200), "Uniform manual scale: " + layout);
            width.setValue(300);
        }
        tree.updatePhylogeneticTreePanel4fitFrame();
        require(height.getValue().equals(100) && width.getValue().equals(100), "Fit resets display");

        String[] names = {"SubRectangularLayout", "SubCircularLayout", "SubSprialLayout", "SubSlopeLayout", "SubRadicalLayout"};
        for (String name : names) {
            JComponent panel = (JComponent) Class.forName(base + ".tree.control." + name).getConstructor().newInstance();
            bind(panel, controller, controllerType);
            if (name.equals("SubSprialLayout")) {
                for (String type : new String[] {"Alpha", "Beta"}) for (String mode : new String[] {"Phylogram", "Cladogram"}) {
                    button(panel, type).doClick(); button(panel, mode).doClick();
                    String expected = "SPRIAL_" + type.toUpperCase() + (mode.equals("Phylogram") ? "_PHYLO" : "_CLADO");
                    require(tree.getLayoutProperties().getMyLayout().name().equals(expected), "Spiral combination " + expected);
                    require(((JSpinner) field(panel, "betaFactor")).isEnabled() == type.equals("Beta"), "Beta-only factor");
                }
                tree.getLayoutProperties().getSprialLayoutPropertiy().setBetaFactor(2.5);
                sync(panel);
                require(((JSpinner) field(panel, "betaFactor")).getValue().equals(250), "Model beta factor");
            } else if (name.equals("SubRadicalLayout")) {
                String[] algorithms = {"Equal-angle", "Equal-daylight", "N-body"};
                for (int i = 0; i < algorithms.length; i++) for (String mode : new String[] {"Phylogram", "Cladogram"}) {
                    button(panel, algorithms[i]).doClick(); button(panel, mode).doClick();
                    String expected = "RADIAL_" + (i == 0 ? "" : i == 1 ? "EQUAL_DAYLIGHT_" : "NBODY_") + (mode.equals("Phylogram") ? "PHYLO" : "CLADO");
                    require(tree.getLayoutProperties().getMyLayout().name().equals(expected), "Unrooted combination " + expected);
                    require(button(panel, "Cancel N-body calculation").isVisible() == (i == 2), "Cancel visibility");
                }
                tree.cancelLayoutCalculation();
                tree.pending(false);
                require(!button(panel, "Cancel N-body calculation").isEnabled(), "Cancel disabled while idle");
                screenshot(panel, pathway ? "pathway-nbody" : "gene-nbody");
                tree.pending(true);
                require(button(panel, "Cancel N-body calculation").isEnabled(), "Cancel enabled during calculation");
                button(panel, "Cancel N-body calculation").doClick();
                require(!button(panel, "Cancel N-body calculation").isEnabled(), "Cancel disables immediately");
                button(panel, "Equal-angle").doClick();
            } else if (name.equals("SubCircularLayout")) {
                tree.getLayoutProperties().getCircularLayoutPropertiy().setGlobalExtendDegree(270);
                sync(panel);
                require(((JSlider) field(panel, "sliderExtentDegree")).getValue() == 270, "Circular slider from model");
                require(((JSpinner) field(panel, "spinnerExtentDegree")).getValue().equals(270), "Circular spinner from model");
                button(panel, "Align tips");
            } else if (name.equals("SubRectangularLayout")) button(panel, "Align tips");
            int count = pathway ? ((PathController) controller).recalculations : ((GeneController) controller).recalculations;
            int updates = tree.updates;
            sync(panel);
            require(count == (pathway ? ((PathController) controller).recalculations : ((GeneController) controller).recalculations), "Sync has no recalculation: " + name);
            require(tree.updates == updates, "Sync must not apply another layout: " + name);
            screenshot(panel, (pathway ? "pathway-" : "gene-") + name);
        }
        Panel second = tree();
        second.setTreePanelScale(150, 175);
        if (pathway) ((PathController) controller).tree = second; else ((GeneController) controller).tree = second;
        sync(zoom);
        require(height.getValue().equals(150) && width.getValue().equals(175), "Tree switch reads its own zoom");
        tree.setTreePanelScale(400, 400);
        require(height.getValue().equals(150), "Old tree observer detached");
        tree.cancelLayoutCalculation(); second.cancelLayoutCalculation();
        JComponent complete = (JComponent) Class.forName(base + ".CtrlTreeLayoutPanel").getConstructor().newInstance();
        bind(complete, controller, controllerType);
        second.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_BETA_CLADO);
        int updates = second.updates;
        sync(complete);
        require(second.updates == updates, "Complete panel sync preserves imported layout without calculation");
        screenshot(complete, pathway ? "pathway-complete" : "gene-complete");
    }

    private static void testSlope() throws Exception {
        Panel panel = tree();
        TreeLayoutProperties properties = panel.getLayoutProperties();
        properties.setBlankArea(new BlankArea(30, 55, 95, 25));
        GraphicsNode root = properties.getOriginalRootNode();
        ((GraphicsNode) root.getChildAt(0).getChildAt(0)).setName("中文标签 Arabidopsis thaliana");
        SlopeCladoAlignedLeft slope = new SlopeCladoAlignedLeft(properties, root, panel);
        for (Dimension canvas : new Dimension[] {new Dimension(1100, 760), new Dimension(760, 1100)}) {
            for (boolean align : new boolean[] {false, true}) for (int rotation : new int[] {0, 90, 180, 270}) {
                properties.setShouldLeafNameRightAlign(align);
                properties.getSlopeLayoutProperety().setSlopeLayoutRotationDeg(rotation);
                Dimension size = slope.getRequiredCanvasSize(canvas.width, canvas.height);
                slope.calculateForPainting(size.width, size.height);
                Rectangle2D bounds = slope.getVisibleTreeBounds();
                BlankArea blank = properties.getBlankArea();
                Rectangle2D work = new Rectangle2D.Double(blank.getLeft(), blank.getTop(), blank.getWorkWidth(size.width), blank.getWorkHeight(size.height));
                require(Math.abs(bounds.getCenterX() - work.getCenterX()) < 1e-7 && Math.abs(bounds.getCenterY() - work.getCenterY()) < 1e-7, "Slope centered: " + rotation);
                require(work.contains(bounds), "Slope labels fit: " + rotation + " " + bounds);
                verifyEdges(root);
                BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, size.width, size.height);
                var before = graphics.getTransform(); slope.paintGraphics(graphics);
                require(before.equals(graphics.getTransform()), "Slope paint restores transform"); graphics.dispose();
                if (!align && canvas.width == 1100) ImageIO.write(image, "png", new File("temp/tree-layout-controls/slope-" + rotation + ".png"));
            }
        }
        DrawPropInternalNode2LeafAnno annotation = new DrawPropInternalNode2LeafAnno(new Color(120, 170, 230, 70), root);
        panel.getMainAnnotationsProperties().getInternalNode2LeafAnnos().add(annotation);
        root.setCgbIDFirst(777);
        properties.getHighlightProperties().getNodes2highlight().put(root.getCGBIDInstance(), Color.BLUE);
        for (int rotation : new int[] {0, 90, 180, 270}) {
            properties.getSlopeLayoutProperety().setSlopeLayoutRotationDeg(rotation);
            slope.calculateForPainting(1100, 760);
            var leaves = module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator.getLeaves(root);
            Rectangle2D expected = new Rectangle2D.Double(root.getXSelf(), root.getYSelf(), 0, 0);
            expected.add(leaves.getFirst().getXSelf(), leaves.getFirst().getYSelf());
            expected.add(leaves.getLast().getXSelf(), leaves.getLast().getYSelf());
            Rectangle2D actual = annotation.getDrawShape().getBounds2D();
            require(Math.abs(actual.getMinX() - expected.getMinX()) < .001 && Math.abs(actual.getMaxX() - expected.getMaxX()) < .001
                    && Math.abs(actual.getMinY() - expected.getMinY()) < .001 && Math.abs(actual.getMaxY() - expected.getMaxY()) < .001, "Annotations share node transform");
            java.util.Collection<TracingPaintingProperty> traces = field(slope, "tracingPaintingShapes");
            require(traces.size() == 1 && traces.iterator().next().pointOfNode.distance(root.getXSelf(), root.getYSelf()) < .001, "Highlight shares node transform");
        }
        panel.updateViewAccording2TreeLayout(TreeLayout.SLOPE_CLADO_ALIGNED_LEFT);
        panel.setTreePanelScale(200, 150);
        properties.getSlopeLayoutProperety().setSlopeLayoutRotationDeg(90);
        panel.recalculatePreservingZoom();
        require(panel.getTreePanelScale().equals(new Dimension(150, 200)), "Rotation preserves independent zoom");
    }

    private static void verifyEdges(GraphicsNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            GraphicsNode child = (GraphicsNode) node.getChildAt(i);
            require(Math.abs(child.getXParent() - node.getXSelf()) < 1e-7 && Math.abs(child.getYParent() - node.getYSelf()) < 1e-7, "Slope parent endpoints agree");
            verifyEdges(child);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { testModule(false); testModule(true); testSlope(); }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("TreeLayoutControlsRegressionTest passed");
    }
}
