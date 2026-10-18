package test.module.evolview.phylotree.visualization.layout;

import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.phylotree.visualization.layout.*;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import graphic.engine.guicalculator.BlankArea;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import static test.module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculatorTest.require;

public class TreeTipLayoutRegressionTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("TreeTipLayoutRegressionTest passed");
    }

    private static void run() throws Exception {
        GraphicsNode root = example();
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        properties.setGlobalFont(new Font("Dialog", Font.PLAIN, 14));
        properties.setShowAxisBar(false);
        properties.setShouldLeafNameRightAlign(true);
        properties.setBlankArea(new BlankArea(30, 55, 95, 25));
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
        BlankArea blank = properties.getBlankArea();
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(properties);
        FontMetrics metrics = host.getFontMetrics(properties.getGlobalFont());
        for (double angle : new double[] {0, 89, 90, 91, 179, 180, 181, 269, 270, 271, 360, 630, 720}) {
            TipLabelGeometry geometry = TipLabelGeometry.at(angle, 3, "短名字 WWW", metrics);
            require(Math.abs(geometry.rotation()) <= Math.PI / 2 + 1e-12, "Upright angle " + angle);
            boolean left = Math.cos(Math.toRadians(-angle)) < -1e-12;
            double gap = left ? -geometry.box().getMaxX() : geometry.box().getMinX();
            require(Math.abs(gap - 7) < 1e-9, "Fixed marker-to-label gap");
        }
        BaseLayout[] layouts = {
            new RadialPhyloLayout(properties, root, host), new RadialCladoLayout(properties, root, host),
            new RadicalEqualDaylightPhyloLayout(properties, root, host), new RadicalEqualDaylightCladoLayout(properties, root, host),
            new RadialNBodyLayout(properties, root, host, false), new RadialNBodyLayout(properties, root, host, true),
            new SpiralPhyloWithAlpha(properties, root, host), new SprialCladoAlignedWithAlpha(properties, root, host),
            new SpiralPhyloWithBeta(properties, root, host), new SpiralCladoAlignedWithBeta(properties, root, host),
            new CircularPhylo(properties, root, host)
        };
        double[] realLengths = allNodes(root).stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray();
        for (BaseLayout layout : layouts) {
            for (int rotation : new int[] {0, 90, 180, 270}) {
                properties.setRadicalLayoutRotationDeg(rotation);
                for (double beta : new double[] {1, 2.5, 5}) {
                    properties.getSprialLayoutPropertiy().setBetaFactor(beta);
                    Dimension size = layout.getRequiredCanvasSize(1100, 760);
                    layout.calculateForPainting(size.width, size.height);
                    Rectangle2D work = new Rectangle2D.Double(blank.getLeft(), blank.getTop(), blank.getWorkWidth(size.width), blank.getWorkHeight(size.height));
                    for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(root)) {
                        Rectangle2D label = TipLabelGeometry.at(leaf.getAngleIfNeeded(), leaf.getDrawUnit().getCircleRadius(), leaf.getName(), metrics)
                                .boundsAt(leaf.getXSelf(), leaf.getYSelf()).getBounds2D();
                        require(work.contains(label), layout.getClass().getSimpleName() + " label clipped: " + leaf.getName() + " " + label);
                    }
                    require(Arrays.equals(realLengths, allNodes(root).stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray()), "Real lengths unchanged");
                }
            }
            if (layout instanceof RadialPhyloLayout radial) {
                RadialPhyloLayout.Calculation cached = radial.prepareCalculation();
                require(cached.cached() != null, "Cache after completed calculation");
                int[] parents = cached.input().parents();
                double[] lengths = cached.input().lengths();
                double screenScale = -1;
                for (int i = 1; i < parents.length; i++) {
                    if (lengths[i] == 0) continue;
                    GraphicsNode node = cached.nodes()[i];
                    GraphicsNode parent = cached.nodes()[parents[i]];
                    double scale = Point2D.distance(node.getXSelf(), node.getYSelf(), parent.getXSelf(), parent.getYSelf()) / lengths[i];
                    if (screenScale < 0) screenScale = scale;
                    require(Math.abs(scale - screenScale) < 1e-7, "Uniform edge scale for both phylograms and cladograms");
                }
                Object coordinates = cached.cached();
                properties.setRadicalLayoutRotationDeg(37);
                radial.calculateForPainting(1100, 760);
                require(radial.prepareCalculation().cached() == coordinates, "Rotation reuses optimized coordinates");
                GraphicsNode first = (GraphicsNode) root.getChildAt(0);
                first.setCollapse(true);
                require(radial.prepareCalculation().cached() == null, "Collapse invalidates cache");
                first.setCollapse(false);
            }
            Dimension small = layout.getRequiredCanvasSize(100, 100);
            require(small.width > 100 && small.height > 100, "Readable scroll canvas");
            layout.calculateForPainting(small.width, small.height);
            Dimension exportSize = layout.getRequiredCanvasSize(1100, 760);
            layout.calculateForPainting(exportSize.width, exportSize.height);
            BufferedImage image = new BufferedImage(exportSize.width, exportSize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.white); graphics.fillRect(0, 0, exportSize.width, exportSize.height);
            AffineTransform original = graphics.getTransform();
            layout.paintGraphics(graphics);
            require(graphics.getTransform().equals(original), "Painting restores transform");
            graphics.dispose();
            new File("temp").mkdirs();
            ImageIO.write(image, "png", new File("temp/" + layout.getClass().getSimpleName() + (layout == layouts[5] ? "Clado" : "") + ".png"));
        }
        RadialPhyloLayout radial = (RadialPhyloLayout) layouts[0];
        for (GraphicsNode node : allNodes(root)) node.setDisplayedBranchLength(node.getRealBranchLength() * .7);
        double[] displayLengths = allNodes(root).stream().mapToDouble(GraphicsNode::getDisplayedBranchLength).toArray();
        layouts[3].calculateForPainting(1100, 760);
        require(Arrays.equals(displayLengths, allNodes(root).stream().mapToDouble(GraphicsNode::getDisplayedBranchLength).toArray()), "Cladogram preserves custom display lengths");
        radial.calculateForPainting(1100, 760);
        ((GraphicsNode) root.getChildAt(0)).setDisplayedBranchLength(12);
        require(radial.prepareCalculation().cached() == null, "Length changes invalidate cache");
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, false);
        require(radial.getRequiredCanvasSize(100, 100).width < 300, "Hidden labels release space");
        testColumnLabelsAndSlope();
        testDegenerateLayouts();
        testSpiralParameters();
    }

    private static class LabelRenderer extends RectPhyloLayout {
        LabelRenderer(TreeLayoutProperties properties, GraphicsNode root, TreeLayoutHost host) { super(properties, root, host); }
        int measuredWidth() { return getMaxLengthLeafNameWidthAccording2CurrentFont(); }
        BufferedImage label(GraphicsNode leaf) {
            BufferedImage image = new BufferedImage(500, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.white); graphics.fillRect(0, 0, 500, 100);
            maxLeafNameLength = measuredWidth();
            drawEachLeafNameGraphicsIfNeeded(graphics, leaf, 100, 50);
            graphics.dispose();
            return image;
        }
    }

    private static void testColumnLabelsAndSlope() {
        GraphicsNode root = new GraphicsNode("");
        GraphicsNode wide = new GraphicsNode("WWWW");
        GraphicsNode narrow = new GraphicsNode("iiiiiiiiii");
        root.addChild(wide); root.addChild(narrow);
        root.setRealBranchLength(0);
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        properties.setGlobalFont(new Font("Serif", Font.PLAIN, 18));
        properties.setShouldLeafNameRightAlign(true);
        properties.setShowAxisBar(false);
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(properties);
        LabelRenderer renderer = new LabelRenderer(properties, root, host);
        FontMetrics metrics = host.getFontMetrics(properties.getGlobalFont());
        require(renderer.measuredWidth() == metrics.stringWidth(wide.getName()), "Use pixel widths instead of character counts");
        require(Math.abs(rightEdge(renderer.label(wide)) - rightEdge(renderer.label(narrow))) <= 2, "Rectangular column labels remain right-aligned");
        properties.setShouldLeafNameRightAlign(false);
        require(rightEdge(renderer.label(wide)) > rightEdge(renderer.label(narrow)), "Disabling column alignment keeps original behavior");
        wide.setName("中文标签 WWWW");
        narrow.setName("iiiiiiiiii");
        RadialPhyloLayout radial = new RadialPhyloLayout(properties, root, host);
        radial.calculateForPainting(900, 700);
        for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(root)) {
            Rectangle2D bounds = TipLabelGeometry.at(leaf.getAngleIfNeeded(), leaf.getDrawUnit().getCircleRadius(), leaf.getName(), metrics)
                    .boundsAt(leaf.getXSelf(), leaf.getYSelf()).getBounds2D();
            require(new Rectangle(0, 0, 900, 700).contains(bounds), "Mixed Chinese/English label fits");
        }
        BaseLayout slope = new SlopeCladoAlignedLeft(properties, root, host);
        for (int rotation : new int[] {0, 90, 180, 270}) {
            properties.getSlopeLayoutProperety().setSlopeLayoutRotationDeg(rotation);
            slope.calculateForPainting(900, 700);
            BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            AffineTransform before = graphics.getTransform();
            slope.paintGraphics(graphics);
            require(before.equals(graphics.getTransform()), "Slope transform restored for " + rotation);
            graphics.dispose();
        }
    }

    private static int rightEdge(BufferedImage image) {
        int edge = 0;
        for (int x = 110; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++) {
            if (image.getRGB(x, y) != Color.white.getRGB()) edge = Math.max(edge, x);
        }
        return edge;
    }

    private static void testDegenerateLayouts() {
        GraphicsNode root = example();
        for (GraphicsNode node : allNodes(root)) node.setRealBranchLength(0);
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
        properties.setShowScaleBar(true);
        properties.setShowAxisBar(true);
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(properties);
        for (BaseLayout layout : new BaseLayout[] {new RadicalEqualDaylightPhyloLayout(properties, root, host),
                new RadialNBodyLayout(properties, root, host, false), new SpiralPhyloWithAlpha(properties, root, host), new SpiralPhyloWithBeta(properties, root, host)}) {
            layout.calculateForPainting(1100, 760);
            for (GraphicsNode node : allNodes(root)) require(Double.isFinite(node.getXSelf()) && Double.isFinite(node.getYSelf()), "Zero-length tree stays finite");
            BufferedImage image = new BufferedImage(1100, 760, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            layout.paintGraphics(graphics);
            graphics.dispose();
        }
    }

    private static void testSpiralParameters() {
        GraphicsNode root = example();
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        properties.setBlankArea(new BlankArea(30, 55, 95, 25));
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(properties);
        FontMetrics metrics = host.getFontMetrics(properties.getGlobalFont());
        var spiral = properties.getSprialLayoutPropertiy();
        spiral.setGlobalStartDegree(17);
        for (BaseLayout layout : new BaseLayout[] {new SpiralPhyloWithAlpha(properties, root, host), new SpiralPhyloWithBeta(properties, root, host)}) {
            for (int extent : new int[] {360, 630, 1080}) for (int gap : new int[] {0, 50}) for (double beta : new double[] {1, 2.5, 5}) {
                spiral.setGlobalExtendingDegree(extent);
                spiral.setGapSize(gap);
                spiral.setBetaFactor(beta);
                Dimension size = layout.getRequiredCanvasSize(800, 600);
                layout.calculateForPainting(size.width, size.height);
                BlankArea blank = properties.getBlankArea();
                Rectangle2D work = new Rectangle2D.Double(blank.getLeft(), blank.getTop(), blank.getWorkWidth(size.width), blank.getWorkHeight(size.height));
                for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(root)) {
                    Rectangle2D bounds = TipLabelGeometry.at(leaf.getAngleIfNeeded(), leaf.getDrawUnit().getCircleRadius(), leaf.getName(), metrics)
                            .boundsAt(leaf.getXSelf(), leaf.getYSelf()).getBounds2D();
                    require(work.contains(bounds), "Multi-turn spiral labels fit: " + extent + ", gap " + gap + ", beta " + beta);
                }
            }
        }
    }

    public static GraphicsNode example() {
        // The nine-species tree shown in the reported screenshots.
        String newick = "((Arabidopsis thaliana:1.2872340425531914,Oryza sativa:1.2872340425531914)Embryophyta:4.707446808510638,"
                + "(Saccharomyces cerevisiae:4.957446808510638,((Caenorhabditis elegans:3.002659574468085,"
                + "Drosophila melanogaster:3.002659574468085)Protostomia|Ecdysozoa:0.997340425531915,"
                + "(Danio rerio:3.0,(Xenopus:2.0632478457957286,(Homo sapiens:1.0,Mus musculus:1.0)Primates:1.063247845795728)"
                + "Tetrapoda:0.9309888920651775)Vertebrata|Gnathostomata:1.0)Bilateria:0.9574468085106383)Opisthokonta:1.047872340425532)Root:0.0;";
        MTVImportInforBean input = new MTVImportInforBean();
        input.setInput_nwk_string(newick);
        input.setNwkFormat(1);
        try {
            return new TreeParser4MTV().parseTree(input).orElseThrow();
        } catch (Exception ex) {
            throw new AssertionError("Nine-species fixture should parse", ex);
        }
    }

    private static java.util.List<GraphicsNode> allNodes(GraphicsNode root) {
        java.util.List<GraphicsNode> nodes = new java.util.ArrayList<>();
        nodes.add(root);
        for (int i = 0; i < root.getChildCount(); i++) nodes.addAll(allNodes((GraphicsNode) root.getChildAt(i)));
        return nodes;
    }
}
