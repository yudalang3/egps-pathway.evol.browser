package module.evolview.phylotree.visualization.layout;

import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.model.tree.ScaleBarProperty;
import module.evolview.gfamily.work.gui.ScaleBarRectObject;
import module.evolview.pathwaybrowser.io.ImporterBean4PathwayFamilyBrowser;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import graphic.engine.guicalculator.BlankArea;
import javax.imageio.ImageIO;
import javax.swing.JViewport;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.lang.reflect.Method;

/** Distance scale invariants, independently of screenshot appearance. */
public class ScaleBarRegressionTest {
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static GraphicsNode example() {
        GraphicsNode root = new GraphicsNode(); root.setName("root");
        for (double length : new double[] {.37, 1.07}) {
            GraphicsNode leaf = new GraphicsNode(); leaf.setName("leaf-" + length);
            leaf.setRealBranchLength(length); root.addChild(leaf);
        }
        return root;
    }

    static BaseLayout layout(TreeLayout type, TreeLayoutProperties properties) throws Exception {
        properties.setMyLayout(type);
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(properties);
        Method factory = PhylogeneticTreePanel.class.getDeclaredMethod("obtainRightLayout", TreeLayout.class);
        factory.setAccessible(true);
        return (BaseLayout) factory.invoke(host, type);
    }

    static BufferedImage render(BaseLayout layout, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.white); graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.black);
        layout.paintGraphics(graphics); graphics.dispose();
        return image;
    }

    static void close(double actual, double expected, String message) {
        require(Math.abs(actual - expected) <= 1e-9 * Math.max(1, Math.abs(expected)), message + ": " + actual + " vs " + expected);
    }

    static void imports() throws Exception {
        var input = new VoiceParameterParser().getOrganizedParameterGetter("$input.newick.text=(A:0.3,B:1.07);\n$tree.show.scale.bar=false\n$tree.show.axis.bar=false\n$layout.initial=SPRIAL_ALPHA_PHYLO\n$font.axis=Monospaced,1,18");
        MTVImportInforBean bean = new ParamsAssignerAndParser4ModernTreeView().generateTreeFromKeyValue(input);
        MTVImportInforBean copied = new ImporterBean4PathwayFamilyBrowser(bean);
        require(!copied.isShowScaleBar() && !copied.isShowAxisBar(), "Pathway explicit false");
        require(copied.getInitialLayout() == bean.getInitialLayout(), "Copied initial layout");
        require(copied.getInput_nwk_string().equals(bean.getInput_nwk_string()), "Copied text input");
        require(copied.getAxisFont().equals(bean.getAxisFont()), "Copied independent axis font");
        require(new TreeParser4MTV().parseTree(copied).isPresent(), "Copied Bean parses");
        for (double bad : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            GraphicsNode root = example(); ((GraphicsNode) root.getChildAt(0)).setRealBranchLength(bad);
            try { new PhylogeneticTreePanel(new TreeLayoutProperties(root)); throw new AssertionError("Invalid lengths accepted"); }
            catch (IllegalArgumentException expected) { require(expected.getMessage().contains("finite"), "Useful error"); }
        }
        GraphicsNode root = example(); ((GraphicsNode) root.getChildAt(0)).setRealBranchLength(Double.MAX_VALUE);
        GraphicsNode child = new GraphicsNode(); child.setName("overflow"); child.setRealBranchLength(Double.MAX_VALUE);
        root.getChildAt(0).addChild(child);
        try { BranchLengthDomain.maximum(root, false); throw new AssertionError("Cumulative overflow accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    static void zeroAndSingleton() throws Exception {
        for (String text : new String[] {"(A:0,B:0);", "(A,B);", "A;", "(A:0);"}) {
            MTVImportInforBean bean = new MTVImportInforBean(); bean.setInput_nwk_string(text);
            GraphicsNode root = new TreeParser4MTV().parseTree(bean).orElseThrow();
            TreeLayoutProperties properties = new TreeLayoutProperties(root);
            PhylogeneticTreePanel panel = new PhylogeneticTreePanel(properties);
            JViewport viewport = new JViewport(); viewport.setSize(700, 500); viewport.setView(panel);
            panel.initializeLeftPanel();
            require(properties.getMyLayout() == TreeLayout.RECT_CLADO_ALIGNED_LEFT, "Zero tree falls back: " + text);
            require(!panel.supportsScaleBar() && !panel.supportsAxisBar(), "No artificial distance scale");
            require(Double.isFinite(root.getXSelf()) && Double.isFinite(root.getYSelf()), "Finite singleton");
            close(BranchLengthDomain.maximum(root, false), 0, "Stored zero lengths preserved");
            render(panel.getCurrLayout(), 700, 500);
        }
    }

    static void geometry() throws Exception {
        for (TreeLayout type : TreeLayout.values()) {
            TreeLayoutProperties properties = new TreeLayoutProperties(example());
            properties.setBlankArea(new BlankArea(31, 53, 83, 29));
            properties.setBranchLengthUnit("MYA");
            BaseLayout layout = layout(type, properties);
            layout.calculateForPainting(1000, 700);
            BufferedImage forward = render(layout, 1000, 700);
            Dimension canvasWithScale = layout.getRequiredCanvasSize(1000, 700);
            String marginsWithScale = properties.getBlankArea().toString();
            GraphicsNode longestLeaf = (GraphicsNode) properties.getOriginalRootNode().getChildAt(1);
            double originalX = longestLeaf.getXSelf(), originalY = longestLeaf.getYSelf();
            double originalRatio = layout.getCanvas2logicRatio();
            properties.setShowScaleBar(false);
            require(canvasWithScale.equals(layout.getRequiredCanvasSize(1000, 700)), "Overlay does not resize canvas: " + type);
            layout.calculateForPainting(1000, 700);
            require(marginsWithScale.equals(properties.getBlankArea().toString()), "Overlay does not change working margins: " + type);
            close(longestLeaf.getXSelf(), originalX, "Overlay preserves X: " + type);
            close(longestLeaf.getYSelf(), originalY, "Overlay preserves Y: " + type);
            close(layout.getCanvas2logicRatio(), originalRatio, "Overlay preserves scale: " + type);
            properties.setShowScaleBar(true);
            layout.calculateForPainting(1000, 700);
            ScaleBarProperty scale = layout.scaleBarProperty;
            double value = scale.getValue(), pixels = scale.getPixelLength();
            if (layout.supportsScaleBar()) {
                close(pixels, value * layout.getCanvas2logicRatio(), "Correct scale: " + type);
                GraphicsNode leaf = (GraphicsNode) properties.getOriginalRootNode().getChildAt(0);
                double edge = java.awt.geom.Point2D.distance(leaf.getXSelf(), leaf.getYSelf(), leaf.getXParent(), leaf.getYParent());
                close(pixels / value, edge / leaf.getRealBranchLength(), "Scale matches drawn branch: " + type);
                require(scale.getLabel().endsWith("MYA"), "Scale unit");
                require(new Rectangle(1000, 700).contains(scale.getBounds()), "Scale fits canvas");
                ScaleBarRectObject drag = new ScaleBarRectObject(scale);
                require(drag.contains(scale.getBounds().getCenterX(), scale.getBounds().getMaxY() - 1), "Label is draggable");
                drag.setFirstPressedPoint((int) scale.getxLocation(), (int) scale.getyLocation());
                drag.adjustPaintings(5000, 5000);
                require(new Rectangle(1000, 700).contains(scale.getBounds()), "Drag clamped");
                scale.setxLocation(120); scale.setyLocation(15);
            }
            properties.setNeedReverseAxisBar(true);
            layout.calculateForPainting(1000, 700);
            BufferedImage reverse = render(layout, 1000, 700);
            if (layout.supportsScaleBar()) {
                close(scale.getValue(), value, "Reverse keeps scale value: " + type);
                close(scale.getPixelLength(), pixels, "Reverse keeps scale length: " + type);
                close(scale.getxLocation(), 120, "Location retained");
            }
            if (type == TreeLayout.RECT_PHYLO_LEFT) {
                double left = properties.getBlankArea().getLeft();
                GraphicsNode leaf = (GraphicsNode) properties.getOriginalRootNode().getChildAt(1);
                double width = leaf.getXSelf() - left;
                for (int i = 0; i < layout.buttomScaleBarDrawProperty.getTickValues().size(); i++) {
                    double tick = layout.buttomScaleBarDrawProperty.getTickValues().get(i);
                    close(layout.buttomScaleBarDrawProperty.getDisplayeDoubles().get(i), width * (1.07 - tick) / 1.07, "Reverse tick geometry");
                }
            }
            if (type == TreeLayout.SPRIAL_BETA_PHYLO) require(!layout.supportsScaleBar() && layout.supportsAxisBar(), "Beta capability");
            if (type == TreeLayout.RECT_HALF_CIRCLE) require(layout.supportsScaleBar() && !layout.supportsAxisBar(), "Half-circle capability");
            if (type.isCladogram()) {
                properties.setShowScaleBar(false); properties.setShowAxisBar(false);
                require(java.util.Arrays.equals(reverse.getRGB(0, 0, 1000, 700, null, 0, 1000), render(layout, 1000, 700).getRGB(0, 0, 1000, 700, null, 0, 1000)), "Cladogram does not draw hidden metrics: " + type);
            }
            if (!type.isCladogram()) {
                ImageIO.write(forward, "png", Path.of("temp/scale-bar", type.name() + ".png").toFile());
            }
            for (int i = 0; i < 3; i++) layout.calculateForPainting(1000, 700);
            BlankArea configured = properties.getConfiguredBlankArea();
            require(configured.getTop() == 31 && configured.getLeft() == 53 && configured.getBottom() == 83 && configured.getRight() == 29, "No automatic margin drift: " + type);
        }
        TreeLayoutProperties largeFont = new TreeLayoutProperties(example());
        largeFont.setAxisFont(new Font("Serif", Font.PLAIN, 60));
        largeFont.setBlankArea(new BlankArea(0, 0, 0, 0));
        largeFont.setBranchLengthUnit("substitutions per site");
        BaseLayout largeLayout = layout(TreeLayout.RECT_PHYLO_LEFT, largeFont);
        Dimension size = largeLayout.getRequiredCanvasSize(200, 180);
        largeLayout.calculateForPainting(size.width, size.height);
        ImageIO.write(render(largeLayout, size.width, size.height), "png", Path.of("temp/scale-bar/large-font.png").toFile());
        require(new Rectangle(size).contains(largeLayout.scaleBarProperty.getBounds()), "Large font and long unit fit");
        for (TreeLayout type : TreeLayout.values()) {
            GraphicsNode root = new GraphicsNode(); root.setName("singleton");
            BaseLayout single = layout(type, new TreeLayoutProperties(root));
            single.calculateForPainting(1000, 700);
            require(Double.isFinite(root.getXSelf()) && Double.isFinite(root.getYSelf()), "Singleton coordinates: " + type);
        }
        for (double unit : new double[] {1e-12, 1e12}) {
            GraphicsNode root = example();
            for (int i = 0; i < root.getChildCount(); i++) ((GraphicsNode) root.getChildAt(i)).setRealBranchLength(((GraphicsNode) root.getChildAt(i)).getRealBranchLength() * unit);
            BaseLayout layout = layout(TreeLayout.RECT_PHYLO_LEFT, new TreeLayoutProperties(root));
            layout.calculateForPainting(1000, 700); render(layout, 1000, 700);
            require(layout.supportsScaleBar(), "Extreme finite units");
            close(layout.scaleBarProperty.getPixelLength() / layout.scaleBarProperty.getValue(), layout.getCanvas2logicRatio(), "Extreme unit scale");
        }
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("temp/scale-bar"));
        require(new MTVImportInforBean().isShowScaleBar(), "New imports must enable the scale bar");
        require(Boolean.TRUE.equals(TreeSettings.defaults().get("Display/Show scale bar")), "Registry default");
        for (TreeLayout type : TreeLayout.values()) {
            TreeLayoutProperties properties = new TreeLayoutProperties(example());
            BaseLayout layout = layout(type, properties);
            layout.calculateForPainting(1000, 700);
            if (type.isCladogram()) {
                require(!layout.supportsScaleBar(), "Cladogram scale: " + type);
                require(!layout.supportsAxisBar(), "Cladogram axis: " + type);
            }
        }
        imports(); zeroAndSingleton(); geometry();
        System.out.println("ScaleBarRegressionTest passed");
    }
}
