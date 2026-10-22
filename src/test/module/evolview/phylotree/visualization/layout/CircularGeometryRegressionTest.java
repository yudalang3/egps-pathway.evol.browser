package module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Expectations come from tree paths and ordered tips, independently of layout helper methods. */
public final class CircularGeometryRegressionTest {
    private static final Path OUTPUT = Path.of("temp/circular-geometry");
    private static final TreeLayout[] TYPES = {TreeLayout.OUTER_CIR_PHYLO, TreeLayout.OUTER_CIR_PHYLO_ALIGNED,
            TreeLayout.OUTER_CIR_CLADO_EQUAL, TreeLayout.OUTER_CIR_CLADO_ALIGNED};
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void equal(double actual, double expected, String message) {
        require(Double.isFinite(actual) && Math.abs(actual - expected) < 1e-7, message + ": " + actual + " != " + expected);
    }
    private static GraphicsNode parse(String text) throws Exception {
        var input = new MTVImportInforBean(); input.setInput_nwk_string(text); input.setNwkFormat(1);
        return new TreeParser4MTV().parseTree(input).orElseThrow();
    }
    private static List<GraphicsNode> all(GraphicsNode root) {
        List<GraphicsNode> result = new ArrayList<>(); result.add(root);
        for (int i = 0; i < root.getChildCount(); i++) result.addAll(all((GraphicsNode) root.getChildAt(i)));
        return result;
    }
    private static CircularPhylo layout(TreeLayout type, TreeLayoutProperties p, PhylogeneticTreePanel host) {
        return switch (type) {
            case OUTER_CIR_PHYLO -> new CircularPhylo(p, p.getOriginalRootNode(), host);
            case OUTER_CIR_PHYLO_ALIGNED -> new CircularPhyloAlign2Tip(p, p.getOriginalRootNode(), host);
            case OUTER_CIR_CLADO_EQUAL -> new CircularCladoEqual(p, p.getOriginalRootNode(), host);
            case OUTER_CIR_CLADO_ALIGNED -> new CircularCladoAlign2Tip(p, p.getOriginalRootNode(), host);
            default -> throw new AssertionError(type);
        };
    }
    private static TreeLayoutProperties properties(GraphicsNode root) {
        var p = new TreeLayoutProperties(root);
        p.setBlankArea(new BlankArea(31, 53, 79, 27));
        p.setShowAxisBar(false); p.setShowScaleBar(false); p.setShowTitle(false);
        LeafLabelStateInitializer.applyInitialLeafLabelState(p, false);
        return p;
    }
    private static void rootRegression() throws Exception {
        var root = parse("((A:1,B:2):1,C:3):5;");
        var p = properties(root); var host = new PhylogeneticTreePanel(p);
        var circle = layout(TreeLayout.OUTER_CIR_PHYLO, p, host);
        circle.calculateForPainting(1100, 760);
        equal(root.getRadicusIfNeeded(), p.getRootTipLength(), "Input root branch must not enter radial distances");
        equal(root.getRealBranchLength(), 5, "Keep real root branch");
        equal(root.getDisplayedBranchLength(), 5, "Keep displayed root branch");
    }
    private static void angleRegression() throws Exception {
        for (String text : List.of("((A:1):1,B:2):0;", "((A:1,B:2,C:3):1,D:2):0;")) {
            var root = parse(text); var p = properties(root);
            var circle = layout(TreeLayout.OUTER_CIR_PHYLO, p, new PhylogeneticTreePanel(p));
            circle.calculateForPainting(1100, 760);
            GraphicsNode clade = (GraphicsNode) root.getChildAt(0);
            equal(clade.getAngleIfNeeded(), (((GraphicsNode) clade.getChildAt(0)).getAngleIfNeeded()
                    + ((GraphicsNode) clade.getChildAt(clade.getChildCount() - 1)).getAngleIfNeeded()) / 2,
                    "Internal angle bisects first/last child for every arity");
        }
    }
    private static void walk(GraphicsNode node, double distance, boolean cladogram,
                             Map<GraphicsNode, Double> distances, List<GraphicsNode> tips) {
        distances.put(node, distance);
        if (node.getChildCount() == 0 || node.getCollapse()) { tips.add(node); return; }
        for (int i = 0; i < node.getChildCount(); i++) {
            GraphicsNode child = (GraphicsNode) node.getChildAt(i);
            walk(child, distance + (cladogram ? 1 : child.getDisplayedBranchLength()), cladogram, distances, tips);
        }
    }
    private static double angle(GraphicsNode node, Map<GraphicsNode, Double> angles) {
        if (angles.containsKey(node)) return angles.get(node);
        double first = angle((GraphicsNode) node.getChildAt(0), angles);
        double last = angle((GraphicsNode) node.getChildAt(node.getChildCount() - 1), angles);
        double result = (first + last) / 2; angles.put(node, result); return result;
    }
    private static Point2D.Double point(double angle, double radius, double cx, double cy) {
        return new Point2D.Double(cx + radius * Math.cos(Math.toRadians(angle)), cy - radius * Math.sin(Math.toRadians(angle)));
    }
    private static void verify(TreeLayout type, TreeLayoutProperties p, PhylogeneticTreePanel host,
                               CircularPhylo circle, Dimension requested, String screenshot) throws Exception {
        verify(type, p, host, circle, requested, screenshot, true);
    }
    private static void verify(TreeLayout type, TreeLayoutProperties p, PhylogeneticTreePanel host,
                               CircularPhylo circle, Dimension requested, String screenshot, boolean recalculate) throws Exception {
        GraphicsNode root = p.getOriginalRootNode();
        Map<GraphicsNode, Double> distances = new LinkedHashMap<>(); List<GraphicsNode> tips = new ArrayList<>();
        walk(root, 0, type.isCladogram(), distances, tips);
        double maximum = distances.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        var nodes = all(root);
        double[] real = nodes.stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray();
        double[] displayed = nodes.stream().mapToDouble(GraphicsNode::getDisplayedBranchLength).toArray();
        Dimension canvas = recalculate ? circle.getRequiredCanvasSize(requested.width, requested.height) : requested;
        if (recalculate) circle.calculateForPainting(canvas.width, canvas.height);
        var b = p.getBlankArea();
        Rectangle2D work = new Rectangle2D.Double(b.getLeft(), b.getTop(), b.getWorkWidth(canvas.width), b.getWorkHeight(canvas.height));
        double cx = b.getLeft() + b.getWorkWidth(canvas.width) / 2;
        double cy = b.getTop() + b.getWorkHeight(canvas.height) / 2;
        double start = p.getRootTipLength(), radius = circle.biggestCircleRadicus;
        require(circle.getCanvas2logicRatio() >= 0, "Outward tree has a nonnegative scale");
        double step = p.getCircularLayoutPropertiy().getGlobalExtendDegree()
                / (double) Math.max(1, tips.size() - (p.getCircularLayoutPropertiy().getGlobalExtendDegree() < 360 ? 1 : 0));
        Map<GraphicsNode, Double> angles = new IdentityHashMap<>();
        for (int i = 0; i < tips.size(); i++) angles.put(tips.get(i), p.getCircularLayoutPropertiy().getGlobalStartDegree() + i * step);
        angle(root, angles);
        for (var entry : distances.entrySet()) {
            GraphicsNode node = entry.getKey();
            double expectedRadius = start + (maximum > 0 ? entry.getValue() / maximum * (radius - start) : 0);
            if (type == TreeLayout.OUTER_CIR_CLADO_ALIGNED && node.getChildCount() == 0 && node != root) expectedRadius = radius;
            equal(node.getRadicusIfNeeded(), expectedRadius, type + " independently calculated radius: " + node.getName());
            equal(node.getAngleIfNeeded(), angles.get(node), type + " independently calculated angle");
            Point2D.Double expected = point(angles.get(node), expectedRadius, cx, cy);
            equal(node.getXSelf(), expected.x, "Actual branch-end X"); equal(node.getYSelf(), expected.y, "Actual branch-end Y");
            require(expectedRadius >= 0 && expectedRadius <= radius + 1e-7 && work.contains(expected), "Real branch and its parent-radius arc fit working area");
            Point2D.Double endpoint = type == TreeLayout.OUTER_CIR_PHYLO_ALIGNED && node.getChildCount() == 0
                    ? point(angles.get(node), radius, cx, cy) : expected;
            require(circle.getDisplayedNodePosition(node).distance(endpoint) < 1e-7, "Displayed terminal agrees with independent geometry");
            if (node.getChildCount() == 0 && p.getShowLeafPropertiesInfo().isShowLeafLabel())
                require(work.contains(TipLabelGeometry.at(angles.get(node), node.getDrawUnit().getCircleRadius(), node.getName(),
                        host.getFontMetrics(p.getGlobalFont())).boundsAt(endpoint.x, endpoint.y).getBounds2D()), "Label fits work area");
            if (node != root) {
                GraphicsNode parent = (GraphicsNode) node.getParent();
                Point2D.Double junction = point(angles.get(node), parent.getRadicusIfNeeded(), cx, cy);
                equal(node.getXParent(), junction.x, "Branch joins parent circle X");
                equal(node.getYParent(), junction.y, "Branch joins parent circle Y");
            }
        }
        require(Arrays.equals(real, nodes.stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray()), "Keep all real lengths");
        require(Arrays.equals(displayed, nodes.stream().mapToDouble(GraphicsNode::getDisplayedBranchLength).toArray()), "Keep all custom displayed lengths");
        if (screenshot != null) {
            BufferedImage image = new BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, canvas.width, canvas.height);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            circle.paintGraphics(g); g.dispose();
            for (int y = 0; y < canvas.height; y++) for (int x = 0; x < canvas.width; x++)
                if (!work.contains(x, y)) require((image.getRGB(x, y) & 0xffffff) == 0xffffff, "No tree ink outside working area");
            ImageIO.write(image, "png", OUTPUT.resolve(screenshot + ".png").toFile());
        }
    }
    private static void viewTransitions() throws Exception {
        var root = parse(Files.readString(Path.of("src/module/evolview/moderntreeviewer/examples/02-primate-support.nwk")));
        var p = properties(root); p.setMyLayout(TreeLayout.OUTER_CIR_PHYLO_ALIGNED);
        var host = new PhylogeneticTreePanel(p);
        var viewport = new JViewport(); viewport.setSize(1100, 760); viewport.setView(host);
        host.initializeLeftPanel(); viewport.doLayout();
        for (boolean labels : new boolean[]{false, true, false}) for (int zoom : new int[]{100, 150}) {
            host.setLeafLabelsVisible(labels);
            host.setTreePanelScale(zoom, zoom);
            host.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT);
            for (TreeLayout type : TYPES) {
                host.updateViewAccording2TreeLayout(TreeLayout.INNER_CIR_CLADO);
                host.updateViewAccording2TreeLayout(type);
                verify(type, p, host, (CircularPhylo) host.getCurrLayout(), host.getTreePanelDimension(), null, false);
                var clade = (GraphicsNode) root.getChildAt(0);
                clade.setCollapse(true); host.recalculatePreservingZoom();
                verify(type, p, host, (CircularPhylo) host.getCurrLayout(), host.getTreePanelDimension(), null, false);
                clade.setCollapse(false); host.recalculatePreservingZoom();
                require(host.getTreePanelScale().equals(new Dimension(zoom, zoom)), "Ring transitions preserve zoom");
                verify(type, p, host, (CircularPhylo) host.getCurrLayout(), host.getTreePanelDimension(), null, false);
            }
        }
    }
    private static void matrix() throws Exception {
        String primates = Files.readString(Path.of("src/module/evolview/moderntreeviewer/examples/02-primate-support.nwk"));
        for (String text : List.of(primates, "((A:1):1,B:2):5;", "((A:1,B:2,C:3,D:0):1,E:2,F:4):5;"))
            for (TreeLayout type : TYPES) {
                GraphicsNode root = parse(text); var p = properties(root); var host = new PhylogeneticTreePanel(p);
                var circle = layout(type, p, host);
                for (boolean labels : new boolean[]{false, true}) for (int start : new int[]{0, 90, 180, 285})
                    for (int extent : new int[]{180, 270, 360}) {
                        LeafLabelStateInitializer.applyInitialLeafLabelState(p, labels);
                        p.getCircularLayoutPropertiy().setGlobalStartDegree(start); p.getCircularLayoutPropertiy().setGlobalExtendDegree(extent);
                        verify(type, p, host, circle, new Dimension(1100, 760), text.equals(primates) && start == 285 && extent == 360
                                ? type + (labels ? "-labels" : "-no-labels") : null);
                    }
                p.setShowRoot(true); p.setRootTipLength(200);
                verify(type, p, host, circle, new Dimension(180, 170), null);
                ((GraphicsNode) root.getChildAt(0)).setCollapse(true);
                verify(type, p, host, circle, new Dimension(701, 650), null);
                ((GraphicsNode) root.getChildAt(0)).setCollapse(false);
                p.setRootTipLength(0);
                for (GraphicsNode node : all(root)) node.setDisplayedBranchLength(node.getRealBranchLength() * .7);
                verify(type, p, host, circle, new Dimension(1100, 760), null);
            }
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (args.length == 0 || args[0].equals("root")) rootRegression();
                if (args.length == 0 || args[0].equals("angles")) angleRegression();
                if (args.length == 0) { matrix(); viewTransitions(); }
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        LoggerFactory.getLogger(CircularGeometryRegressionTest.class).info("CircularGeometryRegressionTest passed");
    }
}
