package test.module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import module.evolview.phylotree.visualization.layout.*;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Covers display anchors independently of the underlying branch-length coordinates. */
public final class AlignedTreeLayoutRegressionTest {
    private static final Path OUTPUT = Path.of("temp/aligned-tree-layout");
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void equal(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 1e-7, message + ": " + actual + " != " + expected);
    }

    private static GraphicsNode primates() throws Exception {
        MTVImportInforBean input = new MTVImportInforBean();
        input.setInput_nwk_string(Files.readString(Path.of("src/module/evolview/moderntreeviewer/examples/02-primate-support.nwk")));
        input.setNwkFormat(0);
        return new TreeParser4MTV().parseTree(input).orElseThrow();
    }

    private static List<GraphicsNode> nodes(GraphicsNode root) {
        List<GraphicsNode> result = new ArrayList<>();
        result.add(root);
        for (int i = 0; i < root.getChildCount(); i++) result.addAll(nodes((GraphicsNode) root.getChildAt(i)));
        return result;
    }

    private static final class RectProbe extends RectPhyloAlign2Tip {
        int labels;
        RectProbe(TreeLayoutProperties p, GraphicsNode root, TreeLayoutHost host) { super(p, root, host); }
        @Override protected void drawEachLeafNameGraphicsIfNeeded(Graphics2D graphics, GraphicsNode node, double x, double y) {
            equal(x, getDisplayedNodePosition(node).x, "Rectangle painter uses displayed X");
            equal(y, getDisplayedNodePosition(node).y, "Rectangle painter uses displayed Y");
            labels++;
            super.drawEachLeafNameGraphicsIfNeeded(graphics, node, x, y);
        }
    }
    private static final class CircleProbe extends CircularPhyloAlign2Tip {
        int labels;
        CircleProbe(TreeLayoutProperties p, GraphicsNode root, TreeLayoutHost host) { super(p, root, host); }
        Point2D.Double center() { return new Point2D.Double(centerX, centerY); }
        @Override protected void drawEachLeafNameGraphicsIfNeeded(Graphics2D graphics, GraphicsNode node, double x, double y) {
            require(getDisplayedNodePosition(node).distance(x, y) < 1e-7, "Circular painter uses displayed anchor");
            labels++;
            super.drawEachLeafNameGraphicsIfNeeded(graphics, node, x, y);
        }
    }
    private static final class InnerProbe extends CircularInnerCladoAligned {
        InnerProbe(TreeLayoutProperties p, GraphicsNode root, TreeLayoutHost host) { super(p, root, host); }
        double outer() { return biggestCircleRadicus; }
        Point2D.Double center() { return new Point2D.Double(centerX, centerY); }
    }

    private static BufferedImage render(BaseLayout layout, int width, int height, String file) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        var before = graphics.getTransform();
        layout.paintGraphics(graphics);
        require(before.equals(graphics.getTransform()), "Painting restores transform");
        graphics.dispose();
        ImageIO.write(image, "png", OUTPUT.resolve(file + ".png").toFile());
        return image;
    }

    private static void alignment(boolean circular) throws Exception {
        GraphicsNode root = primates();
        TreeLayoutProperties p = new TreeLayoutProperties(root);
        p.setGlobalFont(new Font("Dialog", Font.PLAIN, 14));
        p.setShowAxisBar(false); p.setShowTitle(false);
        p.setBlankArea(new BlankArea(31, 53, 79, 27));
        LeafLabelStateInitializer.applyInitialLeafLabelState(p, true);
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(p);
        List<GraphicsNode> all = nodes(root);
        double[] lengths = all.stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray();
        BaseLayout plain = circular ? new CircularPhylo(p, root, host) : new RectPhyloLayout(p, root, host);
        BaseLayout aligned = circular ? new CircleProbe(p, root, host) : new RectProbe(p, root, host);
        for (boolean labels : new boolean[]{true, false}) for (boolean right : new boolean[]{true, false}) {
            LeafLabelStateInitializer.applyInitialLeafLabelState(p, labels);
            p.setShouldLeafNameRightAlign(right);
            for (int start : circular ? new int[]{0, 90, 180, 285} : new int[]{0}) {
                p.getCircularLayoutPropertiy().setGlobalStartDegree(start);
                plain.calculateForPainting(1100, 760);
                List<Point2D.Double> coordinates = all.stream().map(n -> new Point2D.Double(n.getXSelf(), n.getYSelf())).toList();
                aligned.calculateForPainting(1100, 760);
                for (int i = 0; i < all.size(); i++) {
                    require(coordinates.get(i).distance(all.get(i).getXSelf(), all.get(i).getYSelf()) < 1e-7, "Alignment preserves real branch coordinates");
                    equal(all.get(i).getRealBranchLength(), lengths[i], "Alignment preserves lengths");
                }
                Double common = null;
                for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(root)) {
                    Point2D.Double endpoint = aligned.getDisplayedNodePosition(leaf);
                    double value = circular ? endpoint.distance(((CircleProbe) aligned).center()) : endpoint.x;
                    if (common == null) common = value;
                    equal(value, common, "All displayed terminals aligned");
                    if (circular && labels) {
                        var bounds = TipLabelGeometry.at(leaf.getAngleIfNeeded(), leaf.getDrawUnit().getCircleRadius(), leaf.getName(), host.getFontMetrics(p.getGlobalFont()))
                                .boundsAt(endpoint.x, endpoint.y).getBounds2D();
                        require(new Rectangle(0, 0, 1100, 760).contains(bounds), "Aligned label fits canvas");
                    }
                }
                render(aligned, 1100, 760, (circular ? "circular" : "rectangular") + "-" + labels + "-" + right + "-" + start);
            }
        }
        require((circular ? ((CircleProbe) aligned).labels : ((RectProbe) aligned).labels) > 0, "Actual label painter exercised");
        GraphicsNode hidden = GraphicTreePropertyCalculator.getLeaves(root).stream()
                .filter(n -> aligned.getDisplayedNodePosition(n).distance(n.getXSelf(), n.getYSelf()) > 40).findFirst().orElseThrow();
        hidden.setIfHideNode(true);
        BufferedImage hiddenImage = render(aligned, 1100, 760, (circular ? "circular" : "rectangular") + "-hidden");
        Point2D.Double end = aligned.getDisplayedNodePosition(hidden);
        int ink = 0;
        for (double t = .3; t < .7; t += .01) {
            int x = (int) Math.round(hidden.getXSelf() + t * (end.x - hidden.getXSelf()));
            int y = (int) Math.round(hidden.getYSelf() + t * (end.y - hidden.getYSelf()));
            if ((hiddenImage.getRGB(x, y) & 0xffffff) != 0xffffff) ink++;
        }
        require(ink == 0, "Hidden leaves have no alignment extension");
        hidden.setIfHideNode(false);
        GraphicsNode clade = (GraphicsNode) root.getChildAt(0);
        clade.setCollapse(true);
        aligned.calculateForPainting(1100, 760);
        require(aligned.getDisplayedNodePosition(clade).distance(clade.getXSelf(), clade.getYSelf()) == 0, "Collapsed clade is not moved");
        render(aligned, 1100, 760, (circular ? "circular" : "rectangular") + "-collapsed");
        clade.setCollapse(false);
        LeafLabelStateInitializer.applyInitialLeafLabelState(p, true);
        p.setShowAxisBar(true); p.getShowInnerNodePropertiesInfo().setShowInternalNodeBootstrap(true);
        aligned.calculateForPainting(1100, 760);
        GraphicTreePropertyCalculator.getLeaves(root).forEach(n -> n.getDrawUnit().setCircleRadius(3));
        aligned.calculateForPainting(1100, 760);
        BufferedImage finalImage = render(aligned, 1100, 760, circular ? "circular-aligned" : "rectangular-aligned");
        for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(root)) {
            Point2D.Double terminal = aligned.getDisplayedNodePosition(leaf);
            if (terminal.distance(leaf.getXSelf(), leaf.getYSelf()) < 30) continue;
            int extensionInk = 0;
            for (double t = .3; t < .7; t += .01) {
                int x = (int) Math.round(leaf.getXSelf() + t * (terminal.x - leaf.getXSelf()));
                int y = (int) Math.round(leaf.getYSelf() + t * (terminal.y - leaf.getYSelf()));
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
                    if ((finalImage.getRGB(x + dx, y + dy) & 0xffffff) != 0xffffff) extensionInk++;
            }
            require(extensionInk > 0, "Visible alignment extension: " + leaf.getName());
        }
    }

    private static void innerRadius(GraphicsNode root, String name) throws Exception {
        TreeLayoutProperties p = new TreeLayoutProperties(root);
        p.setBlankArea(new BlankArea(31, 53, 79, 27));
        p.setShowAxisBar(false); p.setShowTitle(false);
        LeafLabelStateInitializer.applyInitialLeafLabelState(p, true);
        PhylogeneticTreePanel host = new PhylogeneticTreePanel(p);
        InnerProbe layout = new InnerProbe(p, root, host);
        double[] lengths = nodes(root).stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray();
        for (double ratio : new double[]{.2, .5, 1}) for (int width : new int[]{701, 1100}) for (int extent : new int[]{180, 360}) {
            p.getCircularLayoutPropertiy().setInnerRadiusRatio(ratio);
            p.getCircularLayoutPropertiy().setGlobalExtendDegree(extent);
            p.getCircularLayoutPropertiy().setGlobalStartDegree(171);
            Dimension size = layout.getRequiredCanvasSize(width, 760);
            layout.calculateForPainting(size.width, size.height);
            for (GraphicsNode node : nodes(root)) {
                require(Double.isFinite(node.getXSelf()) && Double.isFinite(node.getYSelf()), "Finite inner coordinates");
                if (node.getChildCount() == 0 || ratio == 1)
                    equal(layout.center().distance(node.getXSelf(), node.getYSelf()), layout.outer() * ratio, "Inner ratio geometry");
            }
            require(Arrays.equals(lengths, nodes(root).stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray()), "Inward layout preserves real lengths");
            render(layout, size.width, size.height, "inner-" + name + "-" + ratio + "-" + width + "-" + extent);
        }
        p.getCircularLayoutPropertiy().setInnerCircleRadicus(35);
        for (int width : new int[]{701, 1100}) {
            layout.calculateForPainting(width, 760);
            for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(root))
                equal(layout.center().distance(leaf.getXSelf(), leaf.getYSelf()), layout.outer() * .2, "Legacy pixel input resets to 20 percent and scales with the canvas");
        }
    }

    private static void selection(TreeLayout type) throws Exception {
        TreeLayoutProperties p = new TreeLayoutProperties(primates());
        p.setMyLayout(type); p.setShowAxisBar(false); p.setShowTitle(false);
        PhylogeneticTreePanel tree = new PhylogeneticTreePanel(p);
        JViewport view = new JViewport(); view.setSize(1100, 760); view.setView(tree);
        tree.initializeLeftPanel(); view.doLayout();
        BaseLayout layout = tree.getCurrLayout();
        GraphicsNode leaf = GraphicTreePropertyCalculator.getLeaves(p.getOriginalRootNode()).stream()
                .filter(n -> layout.getDisplayedNodePosition(n).distance(n.getXSelf(), n.getYSelf()) > 35).findFirst().orElseThrow();
        Point2D.Double point = layout.getDisplayedNodePosition(leaf);
        require(tree.getTreeListener().judgeSelectedNode(new Point((int) point.x, (int) point.y)).orElseThrow() == leaf, "Displayed terminal is clickable");
        require(tree.getTreeListener().judgeSelectedNode(new Point((int) leaf.getXSelf(), (int) leaf.getYSelf())).orElseThrow() == leaf, "Real branch end remains clickable");
        int ctrl = InputEvent.CTRL_DOWN_MASK;
        tree.dispatchEvent(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, ctrl, (int) point.x - 15, (int) point.y - 15, 1, false, MouseEvent.BUTTON1));
        tree.dispatchEvent(new MouseEvent(tree, MouseEvent.MOUSE_DRAGGED, 0, ctrl, (int) point.x + 15, (int) point.y + 15, 0, false, MouseEvent.NOBUTTON));
        tree.dispatchEvent(new MouseEvent(tree, MouseEvent.MOUSE_RELEASED, 0, ctrl, (int) point.x + 15, (int) point.y + 15, 1, false, MouseEvent.BUTTON1));
        require(p.getSelectedNodes().contains(leaf), "Box selects displayed terminal");
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        SwingUtilities.invokeAndWait(() -> {
            try {
                alignment(false); alignment(true);
                innerRadius(primates(), "primates");
                GraphicsNode root = new GraphicsNode("Root"); root.addChild(new GraphicsNode("Single leaf"));
                innerRadius(root, "single-zero");
                GraphicsNode zero = primates(); nodes(zero).forEach(n -> n.setRealBranchLength(0));
                innerRadius(zero, "zero");
                selection(TreeLayout.RECT_PHYLO_ALIGN2TIP); selection(TreeLayout.OUTER_CIR_PHYLO_ALIGNED);
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        LoggerFactory.getLogger(AlignedTreeLayoutRegressionTest.class).info("AlignedTreeLayoutRegressionTest passed");
    }
}
