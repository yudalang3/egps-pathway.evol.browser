package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties4LinageType;
import module.evolview.phylotree.visualization.annotation.OutterSidewardLocation;
import module.evolview.phylotree.visualization.annotation.SidewardNodeAnnotation;
import module.evolview.phylotree.visualization.graphics.struct.NodeType;
import module.evolview.phylotree.visualization.graphics.struct.TreeDecideUtil;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.List;

public class RectHalfCircleLayout extends RectPhyloLayout {
    private static final double RADIUS_FRACTION = 0.3;
    private static final double SINGLE_TIP_HALF_SPAN = 3;

    private record DecorationInsets(int outer, int bottom) { }

    private int archRadius;
    private int sideLength;
    private double arcEnd;
    private double perimeterLength;
    private double centerX;
    private double centerY;
    private double bottomY;
    private double maximumVisibleDistance;

    public RectHalfCircleLayout(TreeLayoutProperties controller, GraphicsNode rootNode,
            TreeLayoutHost phylogeneticTreePanel) {
        super(controller, rootNode, phylogeneticTreePanel);
    }

    @Override
    protected boolean hasCoordinateAxis() { return false; }

    @Override
    protected boolean usesTipLabels() { return true; }

    @Override
    public void prepareLabelSpace() {
        treeLayoutProperties.synchronizeColumnLabelReserve(0);
    }

    @Override
    public Dimension getRequiredCanvasSize(int width, int height) {
        prepareLabelSpace();
        treeLayoutProperties.synchronizeScaleMargins(0, 0, 0, 0);
        DecorationInsets insets = decorationInsets();
        int minimumHeight = (int) Math.ceil(minimumTreeRadius() / RADIUS_FRACTION);
        int minimumWidth = (int) Math.ceil(2 * RADIUS_FRACTION * minimumHeight);
        return new Dimension(Math.max(width, minimumWidth + blankArea.getLeft() + blankArea.getRight() + 2 * insets.outer()),
                Math.max(height, minimumHeight + blankArea.getTop() + blankArea.getBottom() + insets.outer() + insets.bottom()));
    }

    private DecorationInsets decorationInsets() {
        if (!phylogeneticTreePanel.useArchDecorationInsets()) return new DecorationInsets(0, 0);

        FontMetrics metrics = phylogeneticTreePanel.getHostComponent().getFontMetrics(treeLayoutProperties.getGlobalFont());
        double outer = 0, bottom = 0;
        var visible = new ArrayDeque<GraphicsNode>();
        visible.push(rootNode);
        while (!visible.isEmpty()) {
            var node = visible.pop();
            double marker = decorationExtent(node);
            outer = Math.max(outer, marker);
            bottom = Math.max(bottom, marker);
            if (node.getCollapse()) continue;
            if (node.getDrawUnit().isDrawName() && !node.isNameBootstrap() && node.getName() != null && !node.getName().isEmpty()) {
                var box = TipLabelGeometry.at(0, node.getDrawUnit().getCircleRadius(), node.getName(), metrics).box();
                double vertical = Math.max(-box.getMinY(), box.getMaxY());
                outer = Math.max(outer, Math.hypot(box.getMaxX(), vertical));
                bottom = Math.max(bottom, vertical);
            }
            for (int i = 0; i < node.getChildCount(); i++) visible.push((GraphicsNode) node.getChildAt(i));
        }

        double annotationOuter = 0, annotationBottom = 0;
        for (var annotation : phylogeneticTreePanel.getMainAnnotationsProperties().getOutterSidewardAnnos()) {
            if (!annotation.shouldConfigurateAndPaint()) continue;
            var style = annotation.getSidewardNodeAnnotation();
            Font textFont = style.getTextFont() == null ? treeLayoutProperties.getGlobalFont() : style.getTextFont();
            String text = style.getTextString() == null ? "" : style.getTextString();
            FontMetrics label = phylogeneticTreePanel.getHostComponent().getFontMetrics(textFont);
            double stroke = style.getLineStroke().getLineWidth();
            double width = label.stringWidth(text), height = text.isEmpty() ? 0 : label.getHeight();
            annotationOuter = Math.max(annotationOuter, sidewardGap(style) + stroke / 2
                    + (text.isEmpty() ? 0 : style.getTextGap() + Math.hypot(width, height)) + SINGLE_TIP_HALF_SPAN);
            annotationBottom = Math.max(annotationBottom,
                    Math.max(SINGLE_TIP_HALF_SPAN + stroke, (style.isDrawTextHorizontal() ? height : width) / 2));
        }
        var lineage = phylogeneticTreePanel.getLinageTypeAnnotationsProperties();
        if (lineage.hasAnnotation() && lineage.isShowSidewardAnno()) {
            double maxGap = 0, maxStroke = 0;
            for (var style : lineage.getCateglory2SidewardNodeAnnotation().values()) {
                maxGap = Math.max(maxGap, sidewardGap(style));
                maxStroke = Math.max(maxStroke, style.getLineStroke().getLineWidth());
            }
            if (maxGap > 0 || maxStroke > 0) {
                annotationOuter = Math.max(annotationOuter, maxGap + maxStroke / 2);
                annotationBottom = Math.max(annotationBottom, SINGLE_TIP_HALF_SPAN + maxStroke);
            }
        }
        return new DecorationInsets((int) Math.ceil(outer + annotationOuter + 2),
                (int) Math.ceil(Math.max(bottom, annotationBottom) + 2));
    }

    private double decorationExtent(GraphicsNode node) {
        double radius = node.getDrawUnit().getCircleRadius();
        if (node.getCollapse()) {
            var collapse = getCollapseProporty(node);
            int size = collapse == null ? 15 : collapse.getTriangleSize();
            radius = Math.max(radius, Math.sqrt(2) * size);
        }
        var stroke = node.getDrawUnit().getStroke();
        double capExtent = stroke.getEndCap() == BasicStroke.CAP_SQUARE ? Math.sqrt(2) : 1;
        return radius + capExtent * stroke.getLineWidth() / 2;
    }

    @Override
    protected void configureLayoutGeometry(int width, int height) {
        DecorationInsets insets = decorationInsets();
        int availableHeight = Math.max(0, blankArea.getWorkHeight(height));
        int topReserve = Math.min(availableHeight, insets.outer());
        int workWidth = Math.max(0, blankArea.getWorkWidth(width) - 2 * insets.outer());
        int workHeight = Math.max(0, availableHeight - topReserve - insets.bottom());
        int fittedHeight = Math.min(workHeight, (int) Math.floor(workWidth / (2 * RADIUS_FRACTION)));
        int top = blankArea.getTop() + topReserve + (workHeight - fittedHeight) / 2;

        archRadius = (int) (RADIUS_FRACTION * fittedHeight);
        sideLength = fittedHeight - archRadius;
        arcEnd = sideLength + Math.PI * archRadius;
        perimeterLength = arcEnd + sideLength;
        centerX = blankArea.getLeft() + 0.5 * blankArea.getWorkWidth(width);
        centerY = top + archRadius;
        bottomY = top + fittedHeight;

        loop = 0;
        maximumVisibleDistance = BranchLengthDomain.maximum(rootNode, true);
        canvas2logicRatio = maximumVisibleDistance > 0 ? archRadius / maximumVisibleDistance : 0;
        eachYGapLength = perimeterLength / Math.max(1.0, GraphicTreePropertyCalculator.getLeafNumber(rootNode) - 1.0);
        configureBottomScaleBarDrawProperty(maximumVisibleDistance, null, workWidth);
    }

    @Override
    public void calculateForPainting(int width, int height) {
        beforeCalculate(width, height);
        recursiveCalculate(rootNode, 0);
        scaleBarProperty.setIfDrawScaleBar(maximumVisibleDistance > 0 && archRadius > 0);
        afterCalculation();
    }

    @Override
    protected void recursiveCalculate(GraphicsNode node, double parentDepth) {
        double depth = node == rootNode ? 0 : parentDepth + node.getDisplayedBranchLength();
        double radius = radialDistance(depth);
        node.setRadicusIfNeeded(radius);

        double perimeter;
        if (TreeDecideUtil.decideNodeType(node, rootNode) == NodeType.LEAF) {
            perimeter = loop++ * eachYGapLength;
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                recursiveCalculate((GraphicsNode) node.getChildAt(i), depth);
            }
            perimeter = treeLayoutProperties.averageValueOfChildrenForParent(node);
        }
        node.setDoubleVariable(perimeter);
        assignValue(node, radius, perimeter);
        assignValue4XYParent(node, radialDistance(parentDepth), perimeter);
    }

    private double radialDistance(double depth) {
        return maximumVisibleDistance > 0 ? archRadius * (depth / maximumVisibleDistance) : 0;
    }

    public void assignValue(GraphicsNode node, double radius, double perimeter) {
        Point2D.Double point = pointOnPerimeter(radius, perimeter);
        node.setAngleIfNeeded(outwardAngle(perimeter));
        node.setXSelf(point.x);
        node.setYSelf(point.y);
    }

    public void assignValue4XYParent(GraphicsNode node, double radius, double perimeter) {
        Point2D.Double point = pointOnPerimeter(radius, perimeter);
        node.setXParent(point.x);
        node.setYParent(point.y);
    }

    private double arcAngle(double perimeter) {
        return 180 - Math.toDegrees((perimeter - sideLength) / archRadius);
    }

    private double outwardAngle(double perimeter) {
        return perimeter < sideLength ? 180 : perimeter < arcEnd ? arcAngle(perimeter) : 0;
    }

    private Point2D.Double pointOnPerimeter(double radius, double perimeter) {
        if (perimeter < sideLength) {
            return new Point2D.Double(centerX - radius, bottomY - perimeter);
        }
        if (perimeter < arcEnd) {
            double angle = (perimeter - sideLength) / archRadius;
            return new Point2D.Double(centerX - radius * Math.cos(angle), centerY - radius * Math.sin(angle));
        }
        return new Point2D.Double(centerX + radius, bottomY - (perimeterLength - perimeter));
    }

    Path2D contour(double radius, double start, double end) {
        Path2D path = new Path2D.Double();
        Point2D first = pointOnPerimeter(radius, start);
        path.moveTo(first.getX(), first.getY());
        if (start == end) return path;

        boolean forward = start < end;
        double entry = forward ? sideLength : arcEnd;
        double exit = forward ? arcEnd : sideLength;
        double arcStart = forward ? Math.max(start, entry) : Math.min(start, entry);
        double arcStop = forward ? Math.min(end, exit) : Math.max(end, exit);
        if (forward ? arcStart < arcStop : arcStart > arcStop) {
            Point2D seam = pointOnPerimeter(radius, arcStart);
            path.lineTo(seam.getX(), seam.getY());
            if (radius > 0) {
                double angle = arcAngle(arcStart);
                Arc2D arc = new Arc2D.Double();
                arc.setArcByCenter(centerX, centerY, radius, angle, arcAngle(arcStop) - angle, Arc2D.OPEN);
                path.append(arc, true);
            }
        } else if (archRadius == 0 && Math.min(start, end) < sideLength && Math.max(start, end) >= sideLength) {
            // An offset contour can still have width when the reference arch has collapsed.
            path.lineTo(centerX + (forward ? -radius : radius), centerY);
            path.lineTo(centerX + (forward ? radius : -radius), centerY);
        }
        Point2D last = pointOnPerimeter(radius, end);
        path.lineTo(last.getX(), last.getY());
        return path;
    }

    @Override
    protected void configurateMainAnnotation(AnnotationsProperties properties) {
        for (var annotation : properties.getInternalNode2LeafAnnos()) {
            if (!annotation.shouldConfigurateAndPaint()) continue;
            var node = annotation.getCurrentGraphicsNode();
            annotation.setDrawShape(cladeRegion(GraphicTreePropertyCalculator.getLeaves(node), node));
        }
        properties.configurateLeafNamesAnnotaion(false);

        double outer = properties.getOutterSidewardAnnos().isEmpty() ? 0 : outerDecorationRadius();
        for (var annotation : properties.getOutterSidewardAnnos()) {
            if (!annotation.shouldConfigurateAndPaint()) continue;
            var leaves = GraphicTreePropertyCalculator.getLeaves(annotation.getCurrentGraphicsNode());
            double start = leaves.getFirst().getDoubleVariable(), end = leaves.getLast().getDoubleVariable();
            var style = annotation.getSidewardNodeAnnotation();
            double radius = outer + sidewardGap(style);
            annotation.setShape(sidewardShape(radius, start, end));
            annotation.setTextLocation(sidewardTextBaseline(radius, (start + end) / 2, style));
        }
        for (var annotation : properties.getInternalNode2LeafInsituAnnos()) {
            if (annotation.shouldConfigurateAndPaint()) annotation.configurate();
        }
    }

    @Override
    protected boolean useDefaultSingletonSideward() { return false; }

    @Override
    protected void configureLineageGeometry(AnnotationsProperties4LinageType properties) {
        if (properties.isShowSidewardAnno()) {
            double gap = properties.getCateglory2SidewardNodeAnnotation().values().stream()
                    .mapToDouble(RectHalfCircleLayout::sidewardGap).max().orElse(0);
            double radius = outerDecorationRadius() + gap;
            linageTypeSidewardAnnotationCalculator = leaves -> {
                var location = new OutterSidewardLocation();
                location.setShape(sidewardShape(radius, leaves.getFirst().getDoubleVariable(), leaves.getLast().getDoubleVariable()));
                return location;
            };
        }
        if (properties.isShowInternalNode2leafAnno()) {
            linageTypeNode2LeafAnnotationCalculator = leaves -> {
                var location = new OutterSidewardLocation();
                var ancestor = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(leaves.getFirst(), leaves.getLast());
                location.setShape(cladeRegion(leaves, ancestor));
                return location;
            };
        }
    }

    private Shape cladeRegion(List<GraphicsNode> leaves, GraphicsNode ancestor) {
        var first = leaves.getFirst();
        double start = first.getDoubleVariable(), end = leaves.getLast().getDoubleVariable();
        if (start == end) {
            return new Ellipse2D.Double(first.getXSelf() - 3, first.getYSelf() - 3, 6, 6);
        }
        double outer = leaves.stream().mapToDouble(GraphicsNode::getRadicusIfNeeded).max().orElse(0);
        Path2D region = contour(outer, start, end);
        region.append(contour(ancestor.getRadicusIfNeeded(), end, start), true);
        region.closePath();
        return region;
    }

    private double outerDecorationRadius() {
        FontMetrics metrics = phylogeneticTreePanel.getHostComponent().getFontMetrics(treeLayoutProperties.getGlobalFont());
        double outer = 0;
        double[] point = new double[2];
        for (var leaf : GraphicTreePropertyCalculator.getLeaves(rootNode)) {
            outer = Math.max(outer, leaf.getRadicusIfNeeded() + decorationExtent(leaf));
            if (leaf.getCollapse() || !leaf.getDrawUnit().isDrawName() || leaf.getName() == null || leaf.getName().isEmpty()) continue;
            var bounds = getTipLabelGeometry(leaf, metrics).boundsAt(leaf.getXSelf(), leaf.getYSelf());
            for (var corners = bounds.getPathIterator(null); !corners.isDone(); corners.next()) {
                if (corners.currentSegment(point) != PathIterator.SEG_CLOSE) {
                    outer = Math.max(outer, Math.hypot(point[0] - centerX, Math.max(0, centerY - point[1])));
                }
            }
        }
        return outer;
    }

    private static double sidewardGap(SidewardNodeAnnotation style) {
        return style.getLineGap() + style.getLineStroke().getLineWidth() / 2;
    }

    private Shape sidewardShape(double radius, double start, double end) {
        if (start != end) return contour(radius, start, end);

        var point = pointOnPerimeter(radius, start);
        double angle = Math.toRadians(outwardAngle(start));
        double dx = SINGLE_TIP_HALF_SPAN * Math.sin(angle);
        double dy = SINGLE_TIP_HALF_SPAN * Math.cos(angle);
        return new Line2D.Double(point.x - dx, point.y - dy, point.x + dx, point.y + dy);
    }

    private Point2D.Double sidewardTextBaseline(double radius, double perimeter, SidewardNodeAnnotation style) {
        Font textFont = style.getTextFont() == null ? treeLayoutProperties.getGlobalFont() : style.getTextFont();
        String text = style.getTextString() == null ? "" : style.getTextString();
        FontMetrics metrics = phylogeneticTreePanel.getHostComponent().getFontMetrics(textFont);
        double width = metrics.stringWidth(text), height = metrics.getHeight();
        double centerToBaseline = metrics.getAscent() - height / 2;
        double angle = Math.toRadians(outwardAngle(perimeter));
        double nx = Math.cos(angle), ny = -Math.sin(angle);
        boolean horizontal = style.isDrawTextHorizontal();
        double support = (Math.abs(nx) * (horizontal ? width : height)
                + Math.abs(ny) * (horizontal ? height : width)) / 2;
        double centerRadius = radius + style.getLineStroke().getLineWidth() / 2 + style.getTextGap() + support;
        var center = pointOnPerimeter(centerRadius, perimeter);
        return horizontal ? new Point2D.Double(center.x - width / 2, center.y + centerToBaseline)
                : new Point2D.Double(center.x + centerToBaseline, center.y + width / 2);
    }

    @Override
    protected void drawBranchLengthLabel(Graphics2D graphics, GraphicsNode node) {
        if (node == rootNode) return;

        double dx = node.getXSelf() - node.getXParent(), dy = node.getYSelf() - node.getYParent();
        double angle = dx == 0 && dy == 0 ? -Math.toRadians(node.getAngleIfNeeded()) : Math.atan2(dy, dx);
        if (angle > Math.PI / 2) angle -= Math.PI;
        else if (angle < -Math.PI / 2) angle += Math.PI;

        String text = String.valueOf(node.getRealBranchLength());
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.translate(node.getXParent() + dx / 2, node.getYParent() + dy / 2);
        graphics.rotate(angle);
        float gap = node.getDrawUnit().getStroke().getLineWidth() / 2 + 2;
        graphics.drawString(text, -metrics.stringWidth(text) / 2f, -metrics.getDescent() - gap);
    }

    @Override
    protected void drawInternalNodeName(Graphics2D graphics, GraphicsNode node) {
        var placement = getTipLabelGeometry(node, graphics.getFontMetrics());
        Graphics2D label = (Graphics2D) graphics.create();
        try {
            label.translate(node.getXSelf(), node.getYSelf());
            label.rotate(placement.rotation());
            label.drawString(node.getName(), (float) placement.x(), (float) placement.baseline());
        } finally {
            label.dispose();
        }
    }

    @Override
    protected void leafLineDrawingProcess(Graphics2D graphics, GraphicsNode node) {
        commonDrawEachUnit(graphics, node, node != rootNode);
    }

    @Override
    protected void rootDrawingProcess(Graphics2D graphics, GraphicsNode node) {
        drawChildContour(graphics, node);
    }

    @Override
    protected void innerNodeDrawingProcess(Graphics2D graphics, GraphicsNode node) {
        commonDrawEachUnit(graphics, node, true);
        drawChildContour(graphics, node);
    }

    private void drawChildContour(Graphics2D graphics, GraphicsNode node) {
        if (node.getChildCount() < 2) return;
        double start = node.getChildAt(0).getDoubleVariable();
        double end = node.getChildAt(node.getChildCount() - 1).getDoubleVariable();
        var style = node.getDrawUnit();
        graphics.setColor(style.getLineColor());
        graphics.setStroke(style.getStroke());
        graphics.draw(contour(node.getRadicusIfNeeded(), start, end));
    }

    @Override
    protected TracingPaintingProperty configurateTracingShape(GraphicsNode node, Color color) {
        var tracing = new TracingPaintingProperty();
        tracing.path = new GeneralPath();
        tracing.path.moveTo(node.getXSelf(), node.getYSelf());
        node.getDrawUnit().setNodeSelected(true);

        GraphicsNode current = node;
        while (current != rootNode && current.getParent() instanceof GraphicsNode parent) {
            tracing.path.lineTo(current.getXParent(), current.getYParent());
            tracing.path.append(contour(parent.getRadicusIfNeeded(), current.getDoubleVariable(), parent.getDoubleVariable()), true);
            current = parent;
        }
        tracing.pointOfNode = getDisplayedNodePosition(node);
        tracing.fillColor = color;
        return tracing;
    }
}
