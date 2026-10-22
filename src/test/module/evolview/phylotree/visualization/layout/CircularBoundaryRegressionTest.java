package module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.phylotree.visualization.graphics.struct.CollapseProperty;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Rendered boundaries supplement the independent branch geometry tests. */
public final class CircularBoundaryRegressionTest {
    private static final Path OUTPUT = Path.of("temp/circular-boundaries");
    private static final TreeLayout[] TYPES = {TreeLayout.OUTER_CIR_PHYLO, TreeLayout.OUTER_CIR_PHYLO_ALIGNED,
            TreeLayout.OUTER_CIR_CLADO_EQUAL, TreeLayout.OUTER_CIR_CLADO_ALIGNED, TreeLayout.INNER_CIR_CLADO};

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void close(double actual, double expected, String message) {
        require(Double.isFinite(actual) && Math.abs(actual - expected) < 1e-7, message + ": " + actual + " != " + expected);
    }
    private static GraphicsNode parse(String text) throws Exception {
        var bean = new MTVImportInforBean(); bean.setInput_nwk_string(text); bean.setNwkFormat(1);
        return new TreeParser4MTV().parseTree(bean).orElseThrow();
    }
    private static List<GraphicsNode> nodes(GraphicsNode root) {
        List<GraphicsNode> result = new ArrayList<>(); result.add(root);
        for (int i = 0; i < root.getChildCount(); i++) result.addAll(nodes((GraphicsNode) root.getChildAt(i)));
        return result;
    }
    private static TreeLayoutProperties properties(GraphicsNode root, TreeLayout type) {
        var p = new TreeLayoutProperties(root); p.setMyLayout(type);
        p.setGlobalFont(new Font("Dialog", Font.PLAIN, 14));
        p.setBlankArea(new BlankArea(0, 0, 0, 0));
        p.setShowAxisBar(false); p.setShowScaleBar(false); p.setShowTitle(false);
        nodes(root).forEach(n -> n.getDrawUnit().setCircleRadius(0));
        return p;
    }
    private static CircularPhylo layout(TreeLayout type, TreeLayoutProperties p, PhylogeneticTreePanel host) {
        return switch (type) {
            case OUTER_CIR_PHYLO -> new CircularPhylo(p, p.getOriginalRootNode(), host);
            case OUTER_CIR_PHYLO_ALIGNED -> new CircularPhyloAlign2Tip(p, p.getOriginalRootNode(), host);
            case OUTER_CIR_CLADO_EQUAL -> new CircularCladoEqual(p, p.getOriginalRootNode(), host);
            case OUTER_CIR_CLADO_ALIGNED -> new CircularCladoAlign2Tip(p, p.getOriginalRootNode(), host);
            case INNER_CIR_CLADO -> new CircularInnerCladoAligned(p, p.getOriginalRootNode(), host);
            default -> throw new AssertionError(type);
        };
    }
    private static BufferedImage render(BaseLayout layout, Dimension size) {
        var image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, size.width, size.height);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        layout.paintGraphics(graphics); graphics.dispose(); return image;
    }
    private static void noOutsideInk(BufferedImage image, Rectangle work) {
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            if (!work.contains(x, y)) require((image.getRGB(x, y) & 0xffffff) == 0xffffff, "Tree ink outside work area at " + x + "," + y);
    }
    private static Rectangle work(TreeLayoutProperties p, Dimension size) {
        var b = p.getBlankArea();
        return new Rectangle(b.getLeft(), b.getTop(), b.getWorkWidth(size.width), b.getWorkHeight(size.height));
    }
    private static void singles() throws Exception {
        for (String text : List.of("Single:1;", "Single:0;", "(Single:0):0;")) for (TreeLayout type : TYPES)
            for (boolean labels : new boolean[]{false, true}) for (boolean showRoot : new boolean[]{false, true})
                for (double ratio : new double[]{.2, .5, 1}) {
                    var root = parse(text); var p = properties(root, type);
                    nodes(root).forEach(n -> n.getDrawUnit().setCircleRadius(3));
                    LeafLabelStateInitializer.applyInitialLeafLabelState(p, labels); p.setShowRoot(showRoot);
                    p.getCircularLayoutPropertiy().setInnerRadiusRatio(ratio);
                    var host = new PhylogeneticTreePanel(p); var circle = layout(type, p, host);
                    double[] real = nodes(root).stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray();
                    double[] displayed = nodes(root).stream().mapToDouble(GraphicsNode::getDisplayedBranchLength).toArray();
                    Dimension size = circle.getRequiredCanvasSize(700, 700); circle.calculateForPainting(size.width, size.height);
                    BufferedImage image = render(circle, size);
                    if (root.getChildCount() == 0) {
                        require(!circle.supportsAxisBar() && !circle.supportsScaleBar(), "Root input length is not a singleton distance scale");
                        close(circle.getCanvas2logicRatio(), 0, "Singleton has no non-root distance to scale");
                    }
                    for (GraphicsNode node : nodes(root)) {
                        require(Double.isFinite(node.getXSelf()) && Double.isFinite(node.getYSelf()), "Finite singleton geometry");
                        if (node.getChildCount() != 0) continue;
                        double expected = type == TreeLayout.INNER_CIR_CLADO ? circle.biggestCircleRadicus * ratio
                                : type == TreeLayout.OUTER_CIR_PHYLO_ALIGNED || type == TreeLayout.OUTER_CIR_CLADO_ALIGNED
                                ? circle.biggestCircleRadicus : node.getRadicusIfNeeded();
                        close(circle.getDisplayedNodePosition(node).distance(circle.centerX, circle.centerY), expected, "Singleton terminal radius " + type);
                    }
                    require(Arrays.equals(real, nodes(root).stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray()), "Singleton real lengths unchanged");
                    require(Arrays.equals(displayed, nodes(root).stream().mapToDouble(GraphicsNode::getDisplayedBranchLength).toArray()), "Singleton display lengths unchanged");
                    noOutsideInk(image, work(p, size));
                }
        // Use the actual view for click and box-selection on an imported single-node cladogram.
        for (TreeLayout type : new TreeLayout[]{TreeLayout.OUTER_CIR_CLADO_ALIGNED, TreeLayout.INNER_CIR_CLADO}) {
            var root = parse("Single:1;"); var p = properties(root, type); var host = new PhylogeneticTreePanel(p);
            var viewport = new JViewport(); viewport.setSize(700, 700); viewport.setView(host);
            host.initializeLeftPanel(); viewport.doLayout();
            var point = host.getCurrLayout().getDisplayedNodePosition(root);
            require(host.getTreeListener().judgeSelectedNode(new Point((int) point.x, (int) point.y)).orElseThrow() == root, "Singleton terminal is clickable");
            int ctrl = InputEvent.CTRL_DOWN_MASK;
            host.dispatchEvent(new MouseEvent(host, MouseEvent.MOUSE_PRESSED, 0, ctrl, (int) point.x - 15, (int) point.y - 15, 1, false, MouseEvent.BUTTON1));
            host.dispatchEvent(new MouseEvent(host, MouseEvent.MOUSE_DRAGGED, 0, ctrl, (int) point.x + 15, (int) point.y + 15, 0, false, MouseEvent.NOBUTTON));
            host.dispatchEvent(new MouseEvent(host, MouseEvent.MOUSE_RELEASED, 0, ctrl, (int) point.x + 15, (int) point.y + 15, 1, false, MouseEvent.BUTTON1));
            require(p.getSelectedNodes().contains(root), "Singleton terminal is box selectable");
        }
    }
    private static void collapse() throws Exception {
        for (TreeLayout type : TYPES) for (int size : new int[]{15, 20, 65})
            for (int angle : new int[]{0, 45, 90, 135, 180, 225, 270, 315}) for (boolean wholeRoot : new boolean[]{false, true}) {
                var root = parse("((A:1,B:1)Clade:10,C:1):5;"); var p = properties(root, type);
                LeafLabelStateInitializer.applyInitialLeafLabelState(p, false);
                var target = wholeRoot ? root : (GraphicsNode) root.getChildAt(0); target.setCollapse(true);
                if (size != 15) { var property = new CollapseProperty(); property.setTriangleSize(size); p.getCollapsePropertyMaps().put(target.getID(), property); }
                p.getCircularLayoutPropertiy().setGlobalStartDegree(angle);
                if (angle % 90 != 0) p.setBlankArea(new BlankArea(31, 53, 79, 27));
                var host = new PhylogeneticTreePanel(p); var circle = layout(type, p, host);
                Dimension canvas = circle.getRequiredCanvasSize(180, 170); circle.calculateForPainting(canvas.width, canvas.height);
                Rectangle work = work(p, canvas);
                if (wholeRoot) require(!circle.supportsAxisBar() && !circle.supportsScaleBar(), "A collapsed root has no visible non-root distance scale");
                double theta = Math.toRadians(target.getAngleIfNeeded());
                for (int side : new int[]{-1, 1}) {
                    double x = target.getXSelf() + size * Math.cos(theta) + side * size * Math.sin(theta);
                    double y = target.getYSelf() - size * Math.sin(theta) + side * size * Math.cos(theta);
                    require(work.contains(x, y), "Rotated collapse vertex inside work area: " + type + " size=" + size + " angle=" + angle);
                }
                var image = render(circle, canvas); noOutsideInk(image, work);
                if (size == 65 && angle == 45 && !wholeRoot)
                    ImageIO.write(image, "png", OUTPUT.resolve(type + "-collapsed.png").toFile());
            }
        var root = parse("((A:1,B:1)Clade:10,C:1):5;"); var p = properties(root, TreeLayout.OUTER_CIR_PHYLO_ALIGNED);
        var host = new PhylogeneticTreePanel(p); var viewport = new JViewport(); viewport.setSize(240, 220); viewport.setView(host);
        host.initializeLeftPanel(); viewport.doLayout(); host.setTreePanelScale(150, 150);
        var clade = (GraphicsNode) root.getChildAt(0); clade.setCollapse(true);
        var property = new CollapseProperty(); p.getCollapsePropertyMaps().put(clade.getID(), property);
        for (int size : new int[]{20, 65, 15}) {
            property.setTriangleSize(size); host.recalculatePreservingZoom();
            require(host.getTreePanelScale().equals(new Dimension(150, 150)), "Changing collapse size preserves zoom");
            noOutsideInk(render(host.getCurrLayout(), host.getTreePanelDimension()), work(p, host.getTreePanelDimension()));
        }
        clade.setCollapse(false); host.recalculatePreservingZoom();
        require(host.getTreePanelScale().equals(new Dimension(150, 150)), "Expanding preserves zoom");
    }
    private static void ticks() throws Exception {
        for (String family : List.of("Dialog", "Times New Roman")) for (int fontSize : new int[]{14, 72, 144, 200})
            for (double maximum : new double[]{.001, 1, 1000}) for (int width : new int[]{260, 1200})
                for (boolean reverse : new boolean[]{false, true}) {
                    var root = parse("(A:" + maximum + ",B:" + maximum / 2 + "):5;");
                    var p = properties(root, TreeLayout.OUTER_CIR_PHYLO_ALIGNED);
                    p.setAxisFont(new Font(family, Font.PLAIN, fontSize)); p.setShowAxisBar(true); p.setNeedReverseAxisBar(reverse);
                    LeafLabelStateInitializer.applyInitialLeafLabelState(p, false);
                    var host = new PhylogeneticTreePanel(p); var circle = layout(p.getMyLayout(), p, host);
                    Dimension canvas = circle.getRequiredCanvasSize(width, width); circle.calculateForPainting(canvas.width, canvas.height);
                    var values = circle.buttomScaleBarDrawProperty.getTickValues();
                    var labels = circle.buttomScaleBarDrawProperty.getDisplayedStrings();
                    var metrics = host.getFontMetrics(p.getAxisFont()); Rectangle2D previous = null;
                    for (int i = 0; i < values.size(); i++) {
                        double radial = p.getRootTipLength() + (reverse ? maximum - values.get(i) : values.get(i)) * circle.canvas2logicRatio;
                        var box = new Rectangle2D.Double(circle.centerX - metrics.stringWidth(labels.get(i)) / 2.0,
                                circle.centerY + Math.round(radial) - metrics.getAscent(), metrics.stringWidth(labels.get(i)), metrics.getHeight());
                        if (previous != null) {
                            require(!previous.intersects(box), "Actual radial axis text boxes do not overlap: " + family + " " + fontSize);
                            require(Math.abs(box.getY() - previous.getY()) >= metrics.getHeight() + 4, "Radial font-height spacing");
                        }
                        previous = box;
                    }
                    require(circle.supportsAxisBar() == (values.size() >= 2), "Insufficient radial space disables reference axis");
                    if (family.equals("Times New Roman") && fontSize == 144 && maximum == 1 && width == 1200 && !reverse)
                        ImageIO.write(render(circle, canvas), "png", OUTPUT.resolve("large-axis-font.png").toFile());
                }
    }
    private static final class InnerLabels extends CircularInnerCladoAligned {
        InnerLabels(TreeLayoutProperties p, GraphicsNode root, PhylogeneticTreePanel host) { super(p, root, host); }
        BufferedImage labels(Dimension size) {
            var image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, size.width, size.height);
            for (GraphicsNode node : GraphicTreePropertyCalculator.getLeaves(rootNode))
                drawEachLeafNameGraphicsIfNeeded(graphics, node, node.getXSelf(), node.getYSelf());
            graphics.dispose(); return image;
        }
    }
    private static void inwardLabels() throws Exception {
        for (double ratio : new double[]{.2, .5, 1}) for (int start : new int[]{0, 90, 180, 285})
            for (int extent : new int[]{180, 270, 360}) {
                var root = parse("(Short:1,Long_leaf_name_with_WWW:2,Third:3):5;");
                var p = properties(root, TreeLayout.INNER_CIR_CLADO); p.setBlankArea(new BlankArea(31, 53, 79, 27));
                LeafLabelStateInitializer.applyInitialLeafLabelState(p, true);
                p.getCircularLayoutPropertiy().setInnerRadiusRatio(ratio);
                p.getCircularLayoutPropertiy().setGlobalStartDegree(start); p.getCircularLayoutPropertiy().setGlobalExtendDegree(extent);
                var host = new PhylogeneticTreePanel(p); var circle = new InnerLabels(p, root, host);
                Dimension canvas = circle.getRequiredCanvasSize(701, 650); circle.calculateForPainting(canvas.width, canvas.height);
                double inner = circle.biggestCircleRadicus * ratio;
                for (GraphicsNode node : nodes(root)) {
                    if (node.getChildCount() == 0 || ratio == 1) close(node.getRadicusIfNeeded(), inner, "Inward radius including 100 percent");
                    else require(node.getRadicusIfNeeded() >= inner, "Actual branches stay outside the label hole");
                    if (node.getChildCount() == 0) {
                        var placement = circle.getTipLabelGeometry(node, host.getFontMetrics(p.getGlobalFont()));
                        require(Math.abs(placement.rotation()) <= Math.PI / 2, "Inward text stays upright");
                        var outline = placement.boundsAt(node.getXSelf(), node.getYSelf()).getPathIterator(null);
                        double[] vertex = new double[6];
                        while (!outline.isDone()) {
                            if (outline.currentSegment(vertex) != PathIterator.SEG_CLOSE)
                                require(Point2D.distance(vertex[0], vertex[1], circle.centerX, circle.centerY) < inner,
                                        "Full inward text box fits the empty disk: " + node.getName() + " ratio=" + ratio + " start=" + start + " extent=" + extent
                                                + " radius=" + inner + " cornerRadius=" + Point2D.distance(vertex[0], vertex[1], circle.centerX, circle.centerY) + " placement=" + placement);
                            outline.next();
                        }
                    }
                }
                // Paint only actual labels, then independently inspect every ink pixel against the inner disk.
                var labels = circle.labels(canvas); int ink = 0;
                for (int y = 0; y < canvas.height; y++) for (int x = 0; x < canvas.width; x++)
                    if ((labels.getRGB(x, y) & 0xffffff) != 0xffffff) {
                        ink++; require(Point2D.distance(x, y, circle.centerX, circle.centerY) < inner - 1,
                                "Inward text is inside the empty disk, clear of branches: ratio=" + ratio + " angle=" + start);
                    }
                require(ink > 50, "Inward labels are actually painted");
                if (start == 285 && extent == 360) ImageIO.write(render(circle, canvas), "png", OUTPUT.resolve("inner-" + ratio + ".png").toFile());
            }
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (args.length == 0 || args[0].equals("single")) singles();
                if (args.length == 0 || args[0].equals("collapse")) collapse();
                if (args.length == 0 || args[0].equals("ticks")) ticks();
                if (args.length == 0 || args[0].equals("inner")) inwardLabels();
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        LoggerFactory.getLogger(CircularBoundaryRegressionTest.class).info("CircularBoundaryRegressionTest passed");
    }
}
