package module.evolview.phylotree.visualization.layout;

import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.moderntreeviewer.gui.TreeParameterTooltips;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import static module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest.require;

/** Real button events and tooltip rendering on both application control panels. */
public class ScaleBarControlsRegressionTest {
    public static class Gene extends module.evolview.gfamily.GeneFamilyController {
        PhylogeneticTreePanel selected;
        public Gene() { super(null); }
        @Override public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() { return selected; }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return selected.getLayoutProperties(); }
        @Override public void syncTreeControlsFromModel() { }
    }
    public static class Pathway extends module.evolview.pathwaybrowser.PathwayBrowserController {
        PhylogeneticTreePanel selected;
        public Pathway() { super(null); }
        @Override public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() { return selected; }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return selected.getLayoutProperties(); }
        @Override public void syncTreeControlsFromModel() { }
    }

    static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    static JToggleButton button(Object panel, String name) throws Exception {
        Class<?> type = panel.getClass();
        while (type != null) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return (JToggleButton) field.get(panel); }
            catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new AssertionError(name);
    }

    static PhylogeneticTreePanel tree() {
        PhylogeneticTreePanel tree = new PhylogeneticTreePanel(new TreeLayoutProperties(ScaleBarRegressionTest.example()));
        JViewport viewport = new JViewport(); viewport.setSize(1000, 700); viewport.setView(tree);
        tree.initializeLeftPanel(); return tree;
    }

    static void sync(JComponent panel) throws Exception {
        panel.getClass().getMethod("reInitializeGUIAccording2treeLayoutProperties").invoke(panel);
        TreeParameterTooltips.install(panel, true);
    }

    static void exports(PhylogeneticTreePanel tree) throws Exception {
        tree.getLayoutProperties().setBranchLengthUnit("scale-test-unit");
        tree.getLayoutProperties().setAxisFont(new Font("Serif", Font.PLAIN, 14));
        for (TreeLayout layout : new TreeLayout[] {TreeLayout.RECT_PHYLO_LEFT, TreeLayout.RECT_CLADO_ALIGNED_LEFT}) {
            tree.updateViewAccording2TreeLayout(layout);
            tree.setSize(tree.getPreferredSize());
            String prefix = "temp/scale-bar/export-" + layout.name();
            for (String format : new String[] {"png", "jpg"})
                egps2.utils.common.util.EGPSPrintUtilities.saveAsBitGraphics(format, tree, Path.of(prefix + "." + format).toFile());
            for (var format : egps2.frame.gui.VectorGraphicsEncoder.VectorGraphicsFormat.values()) {
                egps2.frame.gui.VectorGraphicsEncoder.saveVectorGraphic(tree, prefix + "." + format.name().toLowerCase(), format);
                require(Files.size(Path.of(prefix + "." + format.name().toLowerCase())) > 100, "Nonempty export: " + format);
            }
            String svg = Files.readString(Path.of(prefix + ".svg"));
            require(svg.contains("scale-test-unit") == !layout.isCladogram(), "SVG metric visibility");
            com.itextpdf.text.pdf.PdfReader reader = new com.itextpdf.text.pdf.PdfReader(prefix + ".pdf");
            try {
                String text = com.itextpdf.text.pdf.parser.PdfTextExtractor.getTextFromPage(reader, 1);
                require(text.contains("scale-test-unit") == !layout.isCladogram(), "PDF metric visibility");
            } finally { reader.close(); }
        }
    }

    static void run(boolean pathway) throws Exception {
        PhylogeneticTreePanel tree = tree();
        TreeLayoutProperties properties = tree.getLayoutProperties();
        Object controller = pathway ? allocate(Pathway.class) : allocate(Gene.class);
        controller.getClass().getDeclaredField("selected").set(controller, tree);
        String family = pathway ? "pathwaybrowser" : "gfamily.work";
        JComponent panel = (JComponent) Class.forName("module.evolview." + family + ".gui.CtrlTreeOperationPanelByMiglayout").getConstructor().newInstance();
        Class<?> controllerType = pathway ? module.evolview.pathwaybrowser.PathwayBrowserController.class : module.evolview.gfamily.GeneFamilyController.class;
        panel.getClass().getMethod("setController", controllerType).invoke(panel, controller);
        JToggleButton scale = button(panel, "scaleBar"), axis = button(panel, "axisButton");
        require(scale.isSelected() && scale.isEnabled() && axis.isSelected() && axis.isEnabled(), "Imported defaults");
        for (TreeLayout layout : TreeLayout.values()) if (layout.isCladogram()) {
            tree.updateViewAccording2TreeLayout(layout); sync(panel);
            for (JToggleButton control : new JToggleButton[] {scale, axis}) {
                require(!control.isEnabled() && !control.isSelected(), "Disabled and unselected: " + layout);
                require(control.getToolTipText().contains("Cladograms") && control.getToolTipText().contains("phylogram"), "Specific tooltip survives installation");
                require(control.getToolTipText().contains("<b>Next step:</b>") && control.getToolTipText().contains("width='400'"), "Layered, width-limited tooltip");
                control.doClick(); require(!control.isSelected(), "Disabled click has no effect");
            }
            require(properties.isShowScaleBar() && properties.isShowAxisBar(), "Cladogram preserves preferences");
            tree.cancelLayoutCalculation();
        }
        JToolTip tip = scale.createToolTip(); tip.setTipText(scale.getToolTipText());
        tip.setSize(tip.getPreferredSize());
        BufferedImage image = new BufferedImage(tip.getWidth(), tip.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); tip.paint(graphics); graphics.dispose();
        ImageIO.write(image, "png", Path.of("temp/scale-bar", family + "-disabled-tooltip.png").toFile());
        tree.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT); sync(panel);
        require(scale.isSelected() && axis.isSelected(), "Restored true choices");
        Dimension canvas = tree.getPreferredSize(), zoom = tree.getTreePanelScale();
        String margins = properties.getBlankArea().toString();
        double leafX = ((module.evolview.model.tree.GraphicsNode) tree.getRootNode().getChildAt(1)).getXSelf();
        double ratio = tree.getCurrLayout().getCanvas2logicRatio();
        scale.doClick(); sync(panel);
        require(canvas.equals(tree.getPreferredSize()) && zoom.equals(tree.getTreePanelScale()), "Scale toggle preserves canvas and zoom");
        require(margins.equals(properties.getBlankArea().toString()), "Scale toggle preserves working margins");
        require(leafX == ((module.evolview.model.tree.GraphicsNode) tree.getRootNode().getChildAt(1)).getXSelf()
                && ratio == tree.getCurrLayout().getCanvas2logicRatio(), "Scale toggle preserves tree coordinates and ratio");
        module.evolview.moderntreeviewer.para.TreeSettings.applyToTree(tree, java.util.Map.of("Display/Show scale bar", true));
        require(canvas.equals(tree.getPreferredSize()) && margins.equals(properties.getBlankArea().toString()), "Advanced scale toggle is overlay-only");
        module.evolview.moderntreeviewer.para.TreeSettings.applyToTree(tree, java.util.Map.of("Display/Show scale bar", false));
        axis.doClick(); sync(panel);
        require(!properties.isShowScaleBar() && !properties.isShowAxisBar(), "User closes metrics");
        tree.updateViewAccording2TreeLayout(TreeLayout.RECT_CLADO_EQUAL_LEFT); sync(panel);
        tree.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT); sync(panel);
        require(!scale.isSelected() && !axis.isSelected() && scale.isEnabled() && axis.isEnabled(), "Restored false choices");
        tree.updateViewAccording2TreeLayout(TreeLayout.SPRIAL_BETA_PHYLO); sync(panel);
        require(!scale.isEnabled() && scale.getToolTipText().contains("varies with angle") && axis.isEnabled(), "Beta capability and reason");
        PhylogeneticTreePanel second = tree();
        controller.getClass().getDeclaredField("selected").set(controller, second); sync(panel);
        require(scale.isSelected() && axis.isSelected(), "Separate tree keeps its own defaults");
        if (!pathway) exports(second);
        tree.cancelLayoutCalculation(); second.cancelLayoutCalculation();
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("temp/scale-bar"));
        SwingUtilities.invokeAndWait(() -> {
            try { run(false); run(true); } catch (Exception error) { throw new RuntimeException(error); }
        });
        System.out.println("ScaleBarControlsRegressionTest passed");
    }
}
