package module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.TreeExportHandler;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.phylotree.visualization.annotation.DrawPropOutterSidewardAnno;
import module.evolview.phylotree.visualization.annotation.SidewardNodeAnnotation;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import org.slf4j.LoggerFactory;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest.close;
import static module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest.render;
import static module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest.require;

public class ArchLayoutRegressionTest {
    private static GraphicsNode node(String name, double length, GraphicsNode... children) {
        var node = new GraphicsNode(name);
        node.setRealBranchLength(length);
        for (GraphicsNode child : children) node.addChild(child);
        return node;
    }

    private static TreeLayoutProperties properties(GraphicsNode root, boolean labels) {
        var properties = new TreeLayoutProperties(root);
        properties.setBlankArea(new BlankArea(20, 20, 20, 20));
        properties.setMyLayout(TreeLayout.RECT_HALF_CIRCLE);
        properties.setShowTitle(false);
        properties.setShowScaleBar(false);
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, labels);
        return properties;
    }

    private static PhylogeneticTreePanel panel(TreeLayoutProperties properties, int width, int height) {
        var panel = new PhylogeneticTreePanel(properties);
        var viewport = new JViewport();
        viewport.setSize(width, height);
        viewport.setView(panel);
        panel.initializeLeftPanel();
        panel.setSize(panel.getPreferredSize());
        return panel;
    }

    private static void colorAt(BufferedImage image, double x, double y, Color color, String message) {
        int pixelX = (int) Math.round(x), pixelY = (int) Math.round(y);
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (image.getRGB(pixelX + offsetX, pixelY + offsetY) == color.getRGB()) return;
            }
        }
        throw new AssertionError(message + " at " + pixelX + ", " + pixelY);
    }

    private static void balancedNarrowTree() {
        var left = node("left", 1);
        var right = node("right", 1);
        var root = node("root", 0, left, right);
        var panel = panel(properties(root, false), 180, 800);
        var layout = panel.getCurrLayout();
        layout.calculateForPainting(180, 800);
        require(left.getXSelf() >= 20 && right.getXSelf() <= 160, "Endpoints fit the narrow work area");
        close(left.getXSelf() + right.getXSelf(), 180, "Narrow arch is centered horizontally");
        close(left.getYSelf(), right.getYSelf(), "Both open ends have the same baseline");
        var image = render(layout, 180, 800);
        colorAt(image, root.getXSelf(), 430, Color.BLACK, "Both root children meet the spine");
        require(image.getRGB(0, 0) == Color.WHITE.getRGB(), "No stray segment at the origin");
    }

    private static void deepCladeAndStyles() {
        GraphicsNode[] tips = new GraphicsNode[6];
        for (int i = 0; i < tips.length; i++) tips[i] = node("tip-" + i, 1);
        var clade = node("internal-name", 1, tips);
        var root = node("root", 0, node("outgroup-left", 2), clade, node("outgroup-right", 2));
        var properties = properties(root, false);
        var layout = (RectHalfCircleLayout) panel(properties, 600, 500).getCurrLayout();
        clade.getDrawUnit().setLineColor(Color.BLUE);
        clade.getDrawUnit().setStroke(new BasicStroke(4));
        tips[0].getDrawUnit().setLineColor(Color.RED);
        tips[0].getDrawUnit().setStroke(new BasicStroke(8));
        tips[0].getDrawUnit().setCircleRadius(9);
        layout.calculateForPainting(600, 500);

        var image = render(layout, 600, 500);
        double radius = clade.getRadicusIfNeeded();
        double seamY = clade.getYSelf() + radius;
        colorAt(image, tips[0].getXParent(), seamY + 10, Color.BLUE, "Left connector");
        colorAt(image, tips[5].getXParent(), seamY + 10, Color.BLUE, "Right connector");
        colorAt(image, tips[0].getXParent(), seamY, Color.BLUE, "Left seam");
        colorAt(image, tips[5].getXParent(), seamY, Color.BLUE, "Right seam");
        colorAt(image, clade.getXSelf() + radius / Math.sqrt(2), seamY - radius / Math.sqrt(2),
                Color.BLUE, "Cap follows a semicircle");
        colorAt(image, (tips[0].getXSelf() + tips[0].getXParent()) / 2, tips[0].getYSelf() + 2,
                Color.RED, "Child branch color and width");
        colorAt(image, tips[0].getXSelf() - 6, tips[0].getYSelf(), Color.RED, "Custom marker radius");

        var tracing = layout.configurateTracingShape(tips[5], Color.MAGENTA);
        require(new BasicStroke(2).createStrokedShape(tracing.path)
                        .contains(clade.getXSelf() + radius / Math.sqrt(2), seamY - radius / Math.sqrt(2)),
                "Reverse tracing follows the cap");
        require(tracing.path.getCurrentPoint().distance(new Point2D.Double(root.getXSelf(), root.getYSelf())) < 1e-5,
                "Tracing ends at the displayed root");
        tips[5].getDrawUnit().setNodeSelected(false);

        clade.getDrawUnit().setDrawName(false);
        int[] hiddenName = render(layout, 600, 500).getRGB(0, 0, 600, 500, null, 0, 600);
        clade.getDrawUnit().setDrawName(true);
        int[] visibleName = render(layout, 600, 500).getRGB(0, 0, 600, 500, null, 0, 600);
        require(!Arrays.equals(hiddenName, visibleName), "Internal name is independent of the hidden leaf labels");

        clade.setCollapse(true);
        properties.initializeRoot();
        layout.calculateForPainting(600, 500);
        image = render(layout, 600, 500);
        colorAt(image, clade.getXSelf(), clade.getYSelf() - 8, Color.BLUE, "Collapsed clade has an outward triangle");
        require(Arrays.stream(image.getRGB(0, 0, 600, 500, null, 0, 600)).noneMatch(rgb -> rgb == Color.RED.getRGB()),
                "Collapsed descendants are hidden");
    }

    private static void labelsAndExport(Path output) {
        var left = node("Left long species label", 1);
        var top = node("Top species label", 1);
        var right = node("Right long species label", 1);
        var root = node("root", 0, left, top, right);
        var properties = properties(root, true);
        var panel = panel(properties, 360, 900);
        var layout = (RectHalfCircleLayout) panel.getCurrLayout();
        Dimension size = panel.getSize();
        layout.calculateForPainting(size.width, size.height);
        var metrics = panel.getFontMetrics(properties.getGlobalFont());
        var work = workArea(properties, size);
        var leftLabel = layout.getTipLabelGeometry(left, metrics);
        var topLabel = layout.getTipLabelGeometry(top, metrics);
        var rightLabel = layout.getTipLabelGeometry(right, metrics);
        close(left.getAngleIfNeeded(), 180, "Left angle in degrees");
        close(top.getAngleIfNeeded(), 90, "Cap angle in degrees");
        close(right.getAngleIfNeeded(), 0, "Right angle in degrees");
        close(leftLabel.rotation(), 0, "Left text horizontal");
        close(rightLabel.rotation(), 0, "Right text horizontal");
        close(Math.abs(topLabel.rotation()), Math.PI / 2, "Top text vertical");
        for (GraphicsNode leaf : new GraphicsNode[]{left, top, right}) {
            var bounds = layout.getTipLabelGeometry(leaf, metrics).boundsAt(leaf.getXSelf(), leaf.getYSelf()).getBounds2D();
            require(work.contains(bounds), "Label fits configured margins: " + leaf.getName());
            if (leaf == left) require(bounds.getMaxX() < leaf.getXSelf(), "Left text points outward");
            if (leaf == right) require(bounds.getMinX() > leaf.getXSelf(), "Right text points outward");
            if (leaf == top) require(bounds.getMaxY() < leaf.getYSelf(), "Cap text points outward");
        }
        new TreeExportHandler() {
            @Override protected Path chooseDestination(Class<?> owner) { return output; }
            @Override protected void success(Path path) { }
            @Override protected void failure(Path path, Throwable error) { throw new AssertionError("SVG export", error); }
        }.export(panel, ArchLayoutRegressionTest.class);
    }

    private static void zeroLength() {
        var root = node("root", 0, node("left", 0), node("top", 0), node("right", 0));
        var properties = properties(root, false);
        var panel = panel(properties, 600, 500);
        require(properties.getMyLayout() == TreeLayout.RECT_CLADO_ALIGNED_LEFT, "Viewer keeps the zero-range fallback");
        var layout = new RectHalfCircleLayout(properties, root, panel);
        layout.calculateForPainting(600, 500);
        for (var node : PhylogeneticTreePanel.subtreeNodes(root)) {
            close(node.getXSelf(), 300, "Zero-length node on the spine");
            require(Double.isFinite(node.getYSelf()), "Finite zero-length node");
        }
        var image = render(layout, 600, 500);
        colorAt(image, 300, 400, Color.BLACK, "Full zero-length tree draws the spine");
        var singleton = node("singleton", 5);
        var singletonProperties = properties(singleton, false);
        var singletonLayout = new RectHalfCircleLayout(singletonProperties, singleton, new PhylogeneticTreePanel(singletonProperties));
        singletonLayout.calculateForPainting(600, 500);
        require(Double.isFinite(singleton.getXSelf()) && Double.isFinite(singleton.getYSelf()), "Finite singleton");
        require(!singletonLayout.supportsScaleBar(), "Positive root stem does not enable the scale");
        close(singleton.getDisplayedBranchLength(), 5, "Singleton stem remains stored");
        var collapsed = node("positive root stem", 5, node("child", 1), node("other child", 2));
        var collapsedProperties = properties(collapsed, false);
        var collapsedLayout = new RectHalfCircleLayout(collapsedProperties, collapsed, new PhylogeneticTreePanel(collapsedProperties));
        collapsed.setCollapse(true);
        collapsedLayout.calculateForPainting(600, 500);
        require(!collapsedLayout.supportsScaleBar(), "Collapsed root has no visible range");
        require(Arrays.equals(textPixels(g -> collapsedLayout.drawBranchLengthLabel(g, collapsed)),
                textPixels(g -> { })), "Root stem gets no branch label");
        collapsed.setCollapse(false);
        collapsedLayout.calculateForPainting(600, 500);
        require(collapsedLayout.supportsScaleBar(), "Expanding restores the child distance scale");
        close(collapsed.getRealBranchLength(), 5, "Real root length remains stored");
        close(collapsed.getDisplayedBranchLength(), 5, "Displayed root length remains stored");
    }

    private static Rectangle2D workArea(TreeLayoutProperties properties, Dimension size) {
        var margins = properties.getConfiguredBlankArea();
        return new Rectangle2D.Double(margins.getLeft(), margins.getTop(),
                margins.getWorkWidth(size.width), margins.getWorkHeight(size.height));
    }

    private static int[] textPixels(Consumer<Graphics2D> draw) {
        var image = new BufferedImage(800, 700, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setFont(new Font("Dialog", Font.PLAIN, 16));
        graphics.setColor(Color.BLACK);
        draw.accept(graphics);
        graphics.dispose();
        return image.getRGB(0, 0, 800, 700, null, 0, 800);
    }

    private static void asymmetricContoursAndText() {
        var deepest = node("last tip", 8);
        var first = node("first tip", 1);
        var root = node("root", 0, first, node("left clade", 2, node("inner tip", 1),
                node("deeper clade", 1, node("fourth tip", 2), node("fifth tip", 1))), deepest);
        var properties = properties(root, false);
        var layout = (RectHalfCircleLayout) panel(properties, 800, 700).getCurrLayout();
        layout.calculateForPainting(800, 700);
        double radius = deepest.getRadicusIfNeeded(), length = deepest.getDoubleVariable();
        close(first.getRadicusIfNeeded(), radius / 8, "Unequal total distances have unequal radii");
        double arc = Math.PI * radius, side = (length - arc) / 2;
        double[] start = {side / 4, side + arc / 4, side + arc + side / 4};
        double[] end = {side * .75, side + arc * .75, length - side / 4};
        var sample = node("Upright name", 2);
        for (int from = 0; from < start.length; from++) {
            for (int to = from; to < end.length; to++) {
                var path = layout.contour(radius / 2, start[from], end[to]);
                var reverse = layout.contour(radius / 2, end[to], start[from]);
                layout.assignValue(sample, radius / 2, end[to]);
                close(path.getCurrentPoint().distance(sample.getXSelf(), sample.getYSelf()), 0, "Forward contour endpoint");
                layout.assignValue(sample, radius / 2, start[from]);
                close(reverse.getCurrentPoint().distance(sample.getXSelf(), sample.getYSelf()), 0, "Reverse contour endpoint");
                var ink = new BasicStroke(2).createStrokedShape(path);
                for (double position : new double[]{(start[from] + end[to]) / 2, side, side + arc}) {
                    if (position <= start[from] || position >= end[to]) continue;
                    layout.assignValue(sample, radius / 2, position);
                    require(ink.contains(sample.getXSelf(), sample.getYSelf()), "Contour follows every traversed segment and seam");
                }
            }
        }
        double[] angles = {0, Math.PI / 4, -Math.PI / 4};
        double[] positions = {start[0], start[1], end[1]};
        for (int i = 0; i < angles.length; i++) {
            layout.assignValue(sample, radius, positions[i]);
            layout.assignValue4XYParent(sample, radius / 3, positions[i]);
            double angle = angles[i];
            boolean flipped = i < 2;
            int[] branch = textPixels(g -> layout.drawBranchLengthLabel(g, sample));
            require(Arrays.equals(branch, textPixels(g -> {
                String label = "2.0";
                var metrics = g.getFontMetrics();
                g.translate((sample.getXSelf() + sample.getXParent()) / 2, (sample.getYSelf() + sample.getYParent()) / 2);
                g.rotate(angle);
                g.drawString(label, -metrics.stringWidth(label) / 2f,
                        -metrics.getDescent() - sample.getDrawUnit().getStroke().getLineWidth() / 2 - 2);
            })), "Branch text uses both midpoint coordinates and an upright baseline: " + i);
            require(Arrays.equals(textPixels(g -> layout.drawInternalNodeName(g, sample)), textPixels(g -> {
                var metrics = g.getFontMetrics();
                int gap = sample.getDrawUnit().getCircleRadius() + 4;
                g.translate(sample.getXSelf(), sample.getYSelf());
                g.rotate(angle);
                g.drawString(sample.getName(), (float) (flipped ? -gap - metrics.stringWidth(sample.getName()) : gap),
                        (metrics.getAscent() - metrics.getDescent()) / 2f);
            })), "Internal name is upright and outward: " + i);
        }

        Dimension minimum = layout.getRequiredCanvasSize(1, 1);
        layout.calculateForPainting(minimum.width, minimum.height);
        require(deepest.getRadicusIfNeeded() >= 50, "Small viewport reserves a 50 px arch radius");
        layout.calculateForPainting(minimum.width - 100, minimum.height);
        close(deepest.getRadicusIfNeeded(), 0, "Actual multi-tip zero-radius geometry");
        close(deepest.getDoubleVariable(), 2, "Degenerate contour still has two straight sides");
        var offset = layout.contour(5, 0, deepest.getDoubleVariable());
        require(new BasicStroke(1).createStrokedShape(offset).contains(
                deepest.getXSelf(), deepest.getYSelf() - 1), "Zero-radius offset connects the two legs at the seam");
    }

    private static DrawPropOutterSidewardAnno annotation(GraphicsNode target, boolean horizontal) {
        var style = new SidewardNodeAnnotation();
        style.setLineStroke(new BasicStroke(4));
        style.setLineColor(Color.MAGENTA);
        style.setTextString("Outward annotation");
        style.setTextFont(new Font("Dialog", Font.PLAIN, 18));
        style.setTextColor(Color.BLUE);
        style.setDrawTextHorizontal(horizontal);
        var annotation = new DrawPropOutterSidewardAnno(style, target);
        annotation.setJust4annotationNot4lineageTypeTrue();
        return annotation;
    }

    private static Rectangle2D annotationTextBounds(DrawPropOutterSidewardAnno annotation, PhylogeneticTreePanel panel) {
        var style = annotation.getSidewardNodeAnnotation();
        var metrics = panel.getFontMetrics(style.getTextFont());
        var baseline = annotation.getTextLocation();
        var transform = AffineTransform.getTranslateInstance(baseline.x, baseline.y);
        if (!style.isDrawTextHorizontal()) transform.rotate(-Math.PI / 2);
        return transform.createTransformedShape(new Rectangle2D.Double(0, -metrics.getAscent(),
                metrics.stringWidth(style.getTextString()), metrics.getHeight())).getBounds2D();
    }

    private static void annotations() {
        var first = node("Long left-side label", 1);
        var left = node("left clade", 2, first, node("Second left-side label", 2));
        var slanted = node("Slanted species label", 3);
        var folded = node("folded", 3, node("hidden", 1));
        folded.setCollapse(true);
        var root = node("root", 0, left, slanted, node("top", 5), node("right of top", 3), node("right", 4), folded);
        var properties = properties(root, true);
        var panel = panel(properties, 950, 760);
        var layout = (RectHalfCircleLayout) panel.getCurrLayout();
        var leftAnnotation = annotation(left, true);
        var arcAnnotation = annotation(slanted, false);
        var foldedAnnotation = annotation(folded, true);
        var across = annotation(root, true);
        panel.getMainAnnotationsProperties().getOutterSidewardAnnos()
                .addAll(java.util.List.of(leftAnnotation, arcAnnotation, foldedAnnotation, across));
        var lineage = panel.getLinageTypeAnnotationsProperties();
        var categories = new HashMap<Integer, String>();
        for (var tip : GraphicTreePropertyCalculator.getLeaves(root)) categories.put(tip.getID(), "category");
        lineage.setNodeID2categlory(categories);
        lineage.getCateglory2SidewardNodeAnnotation().put("category", across.getSidewardNodeAnnotation());
        lineage.setShowSidewardAnno(true);
        lineage.setShowInternalNode2leafAnno(true);
        Dimension size = layout.getRequiredCanvasSize(950, 760);
        layout.calculateForPainting(size.width, size.height);

        var work = workArea(properties, size);
        var metrics = panel.getFontMetrics(properties.getGlobalFont());
        var leftLabel = layout.getTipLabelGeometry(first, metrics).boundsAt(first.getXSelf(), first.getYSelf()).getBounds2D();
        double leftSide = leftAnnotation.getShape().getBounds2D().getMaxX();
        require(leftSide + 2 < leftLabel.getMinX(), "Sideward stroke clears left leaf text");
        require(annotationTextBounds(leftAnnotation, panel).getMaxX() < leftSide - 2, "Left annotation text stays outward");
        require(foldedAnnotation.getShape().getBounds2D().getMinX() > folded.getXSelf() + 15, "Folded clade short tangent clears the triangle");
        var tangent = arcAnnotation.getShape().getBounds2D();
        require(tangent.getWidth() > 0 && tangent.getHeight() > 0, "Single slanted tip keeps its contour tangent");
        require(across.getShape() instanceof java.awt.geom.Path2D && across.getShape().getBounds2D().getMinX() < first.getXSelf(),
                "Cross-arch ordinary annotation follows the contour");
        for (var annotation : panel.getMainAnnotationsProperties().getOutterSidewardAnnos()) {
            var style = annotation.getSidewardNodeAnnotation();
            require(work.contains(style.getLineStroke().createStrokedShape(annotation.getShape()).getBounds2D()), "Annotation stroke fits");
            require(work.contains(annotationTextBounds(annotation, panel)), "Annotation text fits");
        }
        require(lineage.getOutterSidewardAnnos().getFirst().getShape() instanceof java.awt.geom.Path2D,
                "Lineage sideward follows the arch");
        require(lineage.getInternalNode2leafAnno4LinageTypes().getFirst().getShape() instanceof java.awt.geom.Path2D,
                "Lineage area uses a closed arch band");
        var image = render(layout, size.width, size.height);
        require(Arrays.stream(image.getRGB(0, 0, size.width, size.height, null, 0, size.width))
                .anyMatch(rgb -> rgb == Color.BLUE.getRGB()), "Sideward text paints");

        // Programmatic null text/font and heterogeneous lineage gap/stroke styles stay safe and within margins.
        var nullTextStyle = new SidewardNodeAnnotation();
        nullTextStyle.setLineStroke(new BasicStroke(3));
        nullTextStyle.setLineColor(Color.GREEN);
        var nullTextAnno = new DrawPropOutterSidewardAnno(nullTextStyle, left);
        panel.getMainAnnotationsProperties().getOutterSidewardAnnos().add(nullTextAnno);

        var wideGap = new SidewardNodeAnnotation();
        wideGap.setLineGap(30);
        wideGap.setLineStroke(new BasicStroke(2));
        wideGap.setLineColor(Color.ORANGE);
        var thickStroke = new SidewardNodeAnnotation();
        thickStroke.setLineGap(5);
        thickStroke.setLineStroke(new BasicStroke(20));
        thickStroke.setLineColor(Color.CYAN);
        lineage.getCateglory2SidewardNodeAnnotation().clear();
        lineage.getCateglory2SidewardNodeAnnotation().put("wide", wideGap);
        lineage.getCateglory2SidewardNodeAnnotation().put("thick", thickStroke);
        var leaves = GraphicTreePropertyCalculator.getLeaves(root);
        for (int i = 0; i < leaves.size(); i++) categories.put(leaves.get(i).getID(), i == 0 ? "thick" : "wide");
        size = layout.getRequiredCanvasSize(950, 760);
        layout.calculateForPainting(size.width, size.height);
        work = workArea(properties, size);
        require(nullTextAnno.getShape() != null && Double.isFinite(nullTextAnno.getTextLocation().x),
                "Null text/font sideward annotation calculates cleanly");
        for (var lineageAnno : lineage.getOutterSidewardAnnos()) {
            var style = lineageAnno.getSidewardNodeAnnotation();
            require(work.contains(style.getLineStroke().createStrokedShape(lineageAnno.getShape()).getBounds2D()),
                    "Heterogeneous lineage sideward stroke fits");
        }

        // Empty-named nodes with drawName enabled do not inflate insets.
        var emptyRoot = node("", 0, node("", 1), node("", 1));
        var emptyProps = properties(emptyRoot, false);
        var emptyPanel = panel(emptyProps, 200, 200);
        var emptyLayout = (RectHalfCircleLayout) emptyPanel.getCurrLayout();
        Dimension hiddenSize = emptyLayout.getRequiredCanvasSize(1, 1);
        for (var n : PhylogeneticTreePanel.subtreeNodes(emptyRoot)) n.getDrawUnit().setDrawName(true);
        require(hiddenSize.equals(emptyLayout.getRequiredCanvasSize(1, 1)),
                "Empty names do not reserve extra label space");
    }

    private static void decorationsAndZoom() {
        var folded = node("folded-left", 3, node("hidden", 1));
        folded.setCollapse(true);
        var tip = node("visible tip", 3);
        var root = node("root", 0, folded, tip);
        var properties = properties(root, false);
        properties.setBlankArea(new BlankArea(0, 0, 0, 0));
        folded.getDrawUnit().setStroke(new BasicStroke(8));
        tip.getDrawUnit().setCircleRadius(19);
        tip.getDrawUnit().setStroke(new BasicStroke(10));
        var panel = panel(properties, 200, 200);
        var layout = (RectHalfCircleLayout) panel.getCurrLayout();
        Dimension size = layout.getRequiredCanvasSize(200, 200);
        layout.calculateForPainting(size.width, size.height);
        require(folded.getXSelf() >= 15 && folded.getYSelf() <= size.height - 15, "Hidden-label triangle fits at the open end");
        require(tip.getXSelf() <= size.width - 19 && tip.getYSelf() <= size.height - 19, "Hidden-label marker fits at the open end");
        var image = render(layout, size.width, size.height);
        for (int x = 0; x < size.width; x++) {
            require(image.getRGB(x, size.height - 1) == Color.WHITE.getRGB(), "Decorations do not touch the lower edge");
        }

        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
        var leadingHost = new PhylogeneticTreePanel(properties) {
            @Override public FontMetrics getFontMetrics(Font font) {
                FontMetrics standard = super.getFontMetrics(font);
                return new FontMetrics(font) {
                    @Override public int getAscent() { return standard.getAscent(); }
                    @Override public int getDescent() { return standard.getDescent(); }
                    @Override public int getLeading() { return 30; }
                    @Override public int stringWidth(String text) { return standard.stringWidth(text); }
                };
            }
        };
        var leadingLayout = new RectHalfCircleLayout(properties, root, leadingHost);
        size = leadingLayout.getRequiredCanvasSize(200, 200);
        leadingLayout.calculateForPainting(size.width, size.height);
        var metrics = leadingHost.getFontMetrics(properties.getGlobalFont());
        require(workArea(properties, size).contains(leadingLayout.getTipLabelGeometry(tip, metrics)
                .boundsAt(tip.getXSelf(), tip.getYSelf()).getBounds2D()), "Positive leading is included in the bottom reserve");

        panel.recalculatePreservingZoom();
        double fitted = tip.getRadicusIfNeeded();
        TreeSettings.applyToTree(panel, Map.of("View/Width scale", 180));
        require(panel.getTreePanelScale().equals(new Dimension(180, 180)) && tip.getRadicusIfNeeded() > fitted,
                "A width-only edit zooms both axes and the tree");
        TreeSettings.applyToTree(panel, Map.of("View/Height scale", 130));
        require(panel.getTreePanelScale().equals(new Dimension(130, 130)), "A height-only edit can reduce uniform zoom");
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Files.createDirectories(Path.of("temp")), "mtv-arch-");
        Path svg = directory.resolve("arch.svg");
        SwingUtilities.invokeAndWait(() -> {
            balancedNarrowTree();
            deepCladeAndStyles();
            labelsAndExport(svg);
            zeroLength();
            asymmetricContoursAndText();
            annotations();
            decorationsAndZoom();
        });
        String vector = Files.readString(svg);
        require(vector.contains("<svg") && vector.contains("Left long species label")
                && vector.contains("Top species label") && vector.contains("Right long species label"),
                "MTV SVG contains the outward leaf labels as text");
        LoggerFactory.getLogger(ArchLayoutRegressionTest.class).info("ArchLayoutRegressionTest passed");
    }
}
