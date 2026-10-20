package module.evolview.phylotree.visualization.layout;

import com.google.common.base.Strings;
import egps2.utils.common.model.datatransfer.TwoTuple;
import evoltree.txtdisplay.TreeDrawUnit;
import graphic.engine.guicalculator.BlankArea;
import module.evolview.model.enums.BranchLengthType;
import module.evolview.model.tree.*;
import module.evolview.phylotree.visualization.annotation.*;
import module.evolview.phylotree.visualization.graphics.struct.CollapseProperty;
import module.evolview.phylotree.visualization.graphics.struct.NodeType;
import module.evolview.phylotree.visualization.graphics.struct.TreeDecideUtil;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import module.evolview.phylotree.visualization.util.DrawUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D.Double;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The baseLayout that store the common part of all layout.
 */
public abstract class BaseLayout implements ITreeLayout {
    protected final TreeLayoutProperties treeLayoutProperties;
    protected final TreeLayoutHost phylogeneticTreePanel;
    protected final ScaleBarProperty scaleBarProperty;

    protected BottomScaleBarDrawProperty buttomScaleBarDrawProperty = new BottomScaleBarDrawProperty();

    protected Font annotationFont;

    protected double currentHeight;
    protected double currentWidth;

    protected final BlankArea blankArea;
    protected double maximumScaleDistance;

    protected boolean is4AnnotationDialog = false;
    protected boolean isShowAnnotation = true;

    /** 比例尺 */
    protected double canvas2logicRatio = 0.0;

    private final GeneralPath generalPath = new GeneralPath();
    protected Line2D.Double lineDrawUtil = new Line2D.Double();

    protected GraphicsNode rootNode;

    protected Function<LinkedList<GraphicsNode>, OutterSidewardLocation> linageTypeSidewardAnnotationCalculator;
    protected Function<LinkedList<GraphicsNode>, OutterSidewardLocation> linageTypeNode2LeafAnnotationCalculator;

    protected String maxLengthLeafName;
    protected int maxLeafNameLength;

    protected boolean[][] hideNodeArray;

    protected Collection<TracingPaintingProperty> tracingPaintingShapes = new LinkedList<>();

    protected int numOfLeavesWithoutOutgroup;

    private BranchLengthType branchLengthType = BranchLengthType.DIVERGENCE;

    public BaseLayout(TreeLayoutProperties controller, GraphicsNode rootNode,
                      TreeLayoutHost phylogeneticTreePanel) {

        this.treeLayoutProperties = controller;
        this.blankArea = controller.getBlankArea();
        this.scaleBarProperty = phylogeneticTreePanel.getScaleBarProperty();
        this.phylogeneticTreePanel = phylogeneticTreePanel;
        setRootNode(rootNode);

        scaleBarProperty.setIfDrawScaleBar(false);
        annotationFont = treeLayoutProperties.getAxisFont();

    }

    public void setRootNode(GraphicsNode rootNode) {
        this.rootNode = rootNode;

        List<GraphicsNode> leavesWithoutOutgroup = GraphicTreePropertyCalculator.getLeaves(rootNode);
        numOfLeavesWithoutOutgroup = leavesWithoutOutgroup.size();

        // 更新最大的叶子名字！
        String maxLengthLeafName = "";
        for (GraphicsNode tt : leavesWithoutOutgroup) {
            if (tt.getName().length() > maxLengthLeafName.length()) {
                maxLengthLeafName = tt.getName();
            }
        }
        this.maxLengthLeafName = maxLengthLeafName;

        tracingPaintingShapes.clear();

    }

    public String getMaxLengthLeafName() {
        return maxLengthLeafName;
    }

    /**
     * 所有layout计算之前的公共部分！现在只是赋值height与width
     */
    protected void beforeCalculate(int width, int height) {
        prepareLabelSpace();
        prepareScaleSpace();
        buttomScaleBarDrawProperty.clear();
        maximumScaleDistance = 0;
        scaleBarProperty.setIfDrawScaleBar(false);
        annotationFont = treeLayoutProperties.getAxisFont();
        currentHeight = height;
        currentWidth = width;

        int realWidth = blankArea.getWorkWidth(width);
        int realHeight = blankArea.getWorkHeight(height);

        double widthCompressFactor = treeLayoutProperties.getWidthCompressFactor();
        double heightCompressFactor = treeLayoutProperties.getHeightCompressFactor();

        int arrayWidth = (int) (widthCompressFactor * realWidth);
        int arrayHeight = (int) (heightCompressFactor * realHeight);

        if (arrayWidth > 0 && arrayHeight > 0) {
            hideNodeArray = new boolean[arrayWidth][arrayHeight];
        } else {
            hideNodeArray = new boolean[1][1];
        }

    }

    protected void afterCalculation() {
        if (isShowAnnotation) {
            configurateMainAnnotation(phylogeneticTreePanel.getMainAnnotationsProperties());
        }

        AnnotationsProperties4LinageType linageTypeAnnotationsProperties = phylogeneticTreePanel
                .getLinageTypeAnnotationsProperties();
        if (linageTypeAnnotationsProperties.hasAnnotation()) {
            configurateLinageTypeAnnotation(linageTypeAnnotationsProperties);
        }

        Map<CGBID, Color> nodes2highlight = treeLayoutProperties.getHighlightProperties().getNodes2highlight();

        tracingPaintingShapes.clear();
        // Tuple<Integer> sizeOfError = new Tuple<Integer>(0);
        recursive2doSth4eachNode(rootNode, node -> {
            // 配置tracing
            Color color = nodes2highlight.get(node.getCGBIDInstance());
            if (color != null) {
                tracingPaintingShapes.add(configurateTracingShape(node, color));
            }

            // 不要隐藏
            if (true) {
                node.setIfHideNode(false);
                return;
            }

            // 下面是优化显示
            // yudalang 优化显示,注意这里的 x 和 y的系数可以自己选择。
            // 越接近于0 ，显示的点约少，显示越快。
            double widthCompressFactor = treeLayoutProperties.getWidthCompressFactor();
            double heightCompressFactor = treeLayoutProperties.getHeightCompressFactor();

            int intXSelf = (int) (widthCompressFactor * (node.getXSelf() - blankArea.getLeft()));
            int intYSelf = (int) (heightCompressFactor * (node.getYSelf() - blankArea.getTop()));

            if (intXSelf < 0) {
                if (node.getParentCount() == 0) {
                    node.setIfHideNode(false);
                }
            } else if (intXSelf >= hideNodeArray.length) {
                node.setIfHideNode(false);
            } else {
                boolean[] bsArray = hideNodeArray[intXSelf];
                int length = bsArray.length;

                if (intYSelf < 0) {

                } else if (intYSelf >= length) {
                    node.setIfHideNode(false);
                } else {
                    if (bsArray[intYSelf]) {
                        node.setIfHideNode(true);
                    } else {
                        node.setIfHideNode(false);
                        bsArray[intYSelf] = true;
                    }
                }

            }

        });

    }

    /**
     * 递归的画每个节点，从而画树
     */
    protected void recursive2doSth4eachNode(GraphicsNode node, Consumer<GraphicsNode> consumer) {

        NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);
        switch (nodeType) {
            case LEAF:
                consumer.accept(node);
                break;
            default:
                for (int i = 0; i < node.getChildCount(); i++) {
                    recursive2doSth4eachNode((GraphicsNode) node.getChildAt(i), consumer);
                }

                // 改成后续遍历
                consumer.accept(node);
                break;
        }
    }

    protected void configurateMainAnnotation(AnnotationsProperties annotationsProperties) {
    }

    protected void configurateLinageTypeAnnotation(AnnotationsProperties4LinageType annotationsProperties) {

        GraphicsNode[] leavesArrayWithoutOutgroup = GraphicTreePropertyCalculator.getLeaves(rootNode)
                .toArray(new GraphicsNode[1]);

        if (annotationsProperties.isShowInsituAnno()) {
            annotationsProperties.clearAllInsituAnnos();
            Function<LinkedList<GraphicsNode>, OutterSidewardLocation> linageTypeNode2leavesAnnotationCalculator = new Function<LinkedList<GraphicsNode>, OutterSidewardLocation>() {
                @Override
                public OutterSidewardLocation apply(LinkedList<GraphicsNode> t) {

                    GraphicsNode first = t.getFirst();
                    GraphicsNode last = t.getLast();
                    GraphicsNode mrca = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(first, last);
                    annotationsProperties.configurateOneInsituAnno(first.getID(), mrca.getXSelf(), mrca.getYSelf());
                    return null;
                }
            };
            annotationsProperties.configurateDrawPropOutterSidewardAnno4LinageTypes(2, leavesArrayWithoutOutgroup,
                    linageTypeNode2leavesAnnotationCalculator);
        }
        // leaf name annotation
        annotationsProperties.configurateLeafNamesAnnotaion();

        if (annotationsProperties.isShowSidewardAnno()) {
            annotationsProperties.configurateDrawPropOutterSidewardAnno4LinageTypes(0, leavesArrayWithoutOutgroup,
                    linageTypeSidewardAnnotationCalculator);
        }

        if (annotationsProperties.isShowInternalNode2leafAnno()) {
            annotationsProperties.configurateDrawPropOutterSidewardAnno4LinageTypes(1, leavesArrayWithoutOutgroup,
                    linageTypeNode2LeafAnnotationCalculator);
        }
    }

    /**
     * 配置底部刻度: maxLengthOfRoot2Leaf 就是实际的刻度， maxLengthLeaf 是具体的叶子， drawWorkingWidth 是实际可以绘制的宽度。
     *
     *
     * @param maxLengthOfRoot2Leaf
     * @param maxLengthLeaf
     * @param drawWorkingWidth
     */
    protected void configureBottomScaleBarDrawProperty(double maxLengthOfRoot2Leaf, GraphicsNode maxLengthLeaf, int drawWorkingWidth) {
        buttomScaleBarDrawProperty.clear();
        maximumScaleDistance = maxLengthOfRoot2Leaf;
        FontMetrics metrics = phylogeneticTreePanel.getHostComponent().getFontMetrics(treeLayoutProperties.getAxisFont());
        int labelWidth = metrics.stringWidth(BranchLengthTicks.format(maxLengthOfRoot2Leaf));
        for (BranchLengthTicks.Tick tick : BranchLengthTicks.calculate(maxLengthOfRoot2Leaf, drawWorkingWidth, labelWidth)) {
            double offset = drawWorkingWidth * (axisDistance(tick.value()) / maxLengthOfRoot2Leaf);
            buttomScaleBarDrawProperty.addElement(tick.label(), tick.value(), offset);
        }
    }

    protected double axisDistance(double value) {
        return treeLayoutProperties.isNeedReverseAxisBar() ? maximumScaleDistance - value : value;
    }

    protected void drawAxisUnit(Graphics2D graphics) {
        String unit = Objects.toString(treeLayoutProperties.getBranchLengthUnit(), "");
        if (unit.isBlank()) return;
        graphics.setFont(treeLayoutProperties.getAxisFont());
        graphics.setColor(Color.black);
        graphics.drawString(unit, blankArea.getLeft(), (float) (currentHeight - 8
                - graphics.getFontMetrics().getDescent() - (treeLayoutProperties.isShowTitle() ? 30 : 0)));
    }

    public void setBranchLengthType(BranchLengthType branchLengthType) {
        this.branchLengthType = branchLengthType;
    }

    public BranchLengthType getBranchLengthType() {
        return branchLengthType;
    }

    public void setIsShowAnnotation(boolean b) {
        this.isShowAnnotation = b;

    }

    protected boolean usesBranchLengths() { return true; }

    protected boolean hasCoordinateAxis() { return false; }

    public boolean supportsScaleBar() {
        return usesBranchLengths() && isLayoutSupportDrawScaleBar() && scaleBarProperty.isIfDrawScaleBar()
                && maximumScaleDistance > 0 && java.lang.Double.isFinite(canvas2logicRatio) && canvas2logicRatio > 0;
    }

    public boolean supportsAxisBar() {
        return usesBranchLengths() && hasCoordinateAxis() && maximumScaleDistance > 0
                && buttomScaleBarDrawProperty.getTickValues().size() >= 2;
    }

    protected boolean isLayoutSupportDrawScaleBar() { return usesBranchLengths(); }

    public String scaleUnavailableReason(boolean axis) {
        if (!usesBranchLengths()) return "Cladograms show topology; branch lengths are not drawn to scale. A branch-length scale or numeric axis is unavailable. Switch to a phylogram to use these controls.";
        if (!axis && !isLayoutSupportDrawScaleBar()) return "Branch-length pixel scale varies with angle in the Beta spiral. A single scale bar cannot represent the whole tree. Use its numeric spiral reference axis instead.";
        if (axis && !hasCoordinateAxis()) return "This layout has no global coordinate axis. Use the branch-length scale bar instead.";
        return "No valid positive branch-length range is available in the current view.";
    }

    protected final void drawScaleBar(Graphics2D g2d) {
        if (!is4AnnotationDialog && treeLayoutProperties.isShowScaleBar() && supportsScaleBar())
            drawScaleBar(g2d, scaleBarProperty);
    }

    public void setIs4AnnotationDialog(boolean is4AnnotationDialog) {
        this.is4AnnotationDialog = is4AnnotationDialog;
    }

    /**
     * 给选中的节点加一个选中了的表示框 这个方法的作用就是说：
     *
     * <pre>
     *
     *  ----.  对于这个Node，调用后的结果就是：
     *
     *        -
     *   ----|.| 加一个框框
     *        -
     * </pre>
     *
     * @param g2d
     * @param x
     * @param y
     */
    private void paintSelectedRectangularSignatureDashLine(Graphics2D g2d, double x, double y) {
        int xx = (int) x;
        int yy = (int) y;
        Component selpan = LineLabel.getInstance().getRendLabel();
        treeLayoutProperties.getLeafLabelPan().paintComponent(g2d, selpan, phylogeneticTreePanel.getHostComponent(), xx - 7, yy - 7, 14,
                14, false);
    }

    /**
     *
     * 当现实叶子名称的时候经常需要得到最长的叶子名称所占的长度。
     *
     * @title getMaxLengthLeafNameWidthAccording2CurrentGlobalFont
     * @createdDate 2020-11-06 14:06
     * @lastModifiedDate 2020-11-06 14:06
     * @author yudalang
     * @since 1.7
     *
     * @return int
     */
    protected int getMaxLengthLeafNameWidthAccording2CurrentFont() {
        FontMetrics fontMetrics = phylogeneticTreePanel.getHostComponent()
                .getFontMetrics(treeLayoutProperties.getGlobalFont());
        int width = 0;
        for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(rootNode)) {
            if ((!usesTipLabels() || leaf.getDrawUnit().isDrawName()) && leaf.getName() != null) {
                width = Math.max(width, fontMetrics.stringWidth(leaf.getName()));
            }
        }
        return width;
    }

    protected boolean usesTipLabels() { return false; }

    public void prepareLabelSpace() {
        treeLayoutProperties.synchronizeColumnLabelReserve(0);
    }

    private void prepareScaleSpace() {
        FontMetrics metrics = phylogeneticTreePanel.getHostComponent().getFontMetrics(treeLayoutProperties.getAxisFont());
        boolean axis = treeLayoutProperties.isShowAxisBar() && usesBranchLengths() && hasCoordinateAxis();
        int bottom = axis ? 30 + metrics.getHeight() + metrics.getDescent() + (treeLayoutProperties.isShowTitle() ? 30 : 0) : 0;
        int labelWidth = axis ? metrics.stringWidth(BranchLengthTicks.format(BranchLengthDomain.maximum(rootNode, true))) : 0;
        int unitWidth = axis ? metrics.stringWidth(Objects.toString(treeLayoutProperties.getBranchLengthUnit(), "")) : 0;
        treeLayoutProperties.synchronizeScaleMargins(0, axis ? labelWidth / 2 + 8 : 0, bottom,
                axis ? labelWidth / 2 + unitWidth + 18 : 0);
    }

    protected double minimumTreeRadius() { return 50; }

    protected double getTipLabelReserve() {
        FontMetrics metrics = phylogeneticTreePanel.getHostComponent().getFontMetrics(treeLayoutProperties.getGlobalFont());
        int radius = 0;
        Deque<GraphicsNode> pending = new ArrayDeque<>();
        pending.push(rootNode);
        while (!pending.isEmpty()) {
            GraphicsNode node = pending.pop();
            radius = Math.max(radius, node.getDrawUnit().getCircleRadius());
            if (!node.getCollapse()) {
                for (int i = 0; i < node.getChildCount(); i++) pending.push((GraphicsNode) node.getChildAt(i));
            }
        }
        return radius + 4 + (treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel()
                ? getMaxLengthLeafNameWidthAccording2CurrentFont() + metrics.getHeight() : 0);
    }

    /** Preserve readable fonts on small viewports by giving the scroll pane a larger canvas. */
    public Dimension getRequiredCanvasSize(int width, int height) {
        prepareLabelSpace();
        prepareScaleSpace();
        width = Math.max(width, blankArea.getLeft() + blankArea.getRight() + 100);
        height = Math.max(height, blankArea.getTop() + blankArea.getBottom() + 100);
        if (!usesTipLabels()) return new Dimension(width, height);
        int diameter = (int) Math.ceil(2 * (minimumTreeRadius() + getTipLabelReserve()));
        return new Dimension(Math.max(width, diameter + blankArea.getLeft() + blankArea.getRight()),
                Math.max(height, diameter + blankArea.getTop() + blankArea.getBottom()));
    }

    /**
     *
     * 我们再来回顾一下五大Layout: 矩形，环形，螺旋形，倾斜型，无根树型
     *
     * <pre>
     *
     *    (x1,y1)   (x2,y2)
     *     .|------ . 这一段是一个节点应该绘制的。
     *     |
     * ----|
     * </pre>
     *
     *
     *
     * @title commonDrawEachUnit
     * @createdDate 2020-11-06 15:32
     * @lastModifiedDate 2020-11-06 15:32
     * @author yudalang
     * @since 1.7
     *
     * @param g2d
     * @param node
     * @param drawLine
     * @return void
     */
    protected void commonDrawEachUnit(Graphics2D g2d, GraphicsNode node, boolean drawLine) {
        double x1 = node.getXParent();
        double y1 = node.getYParent();

        double x2 = node.getXSelf();
        double y2 = node.getYSelf();

        TreeDrawUnit drawUnit = node.getDrawUnit();
        g2d.setColor(drawUnit.getLineColor());
        g2d.setStroke(drawUnit.getStroke());

        if (drawLine) {
            lineDrawUtil.setLine(x1, y1, x2, y2);
            g2d.draw(lineDrawUtil);
        }

        // 处理Collapse
        if (node.getCollapse()) {
            int triangleSize = 15;
            CollapseProperty collapseProperty = getCollapseProporty(node);
            if (collapseProperty != null) {
                triangleSize = collapseProperty.getTriangleSize();
                g2d.setColor(collapseProperty.getColor());
            }
            generalPath.reset();
            generalPath.moveTo(x2, y2);
            generalPath.lineTo(x2 + triangleSize, y2 + triangleSize);
            generalPath.lineTo(x2 + triangleSize, y2 - triangleSize);
            generalPath.closePath();

            double angleIfNeeded = node.getAngleIfNeeded();
            if (angleIfNeeded != 0) {
                double radians = Math.toRadians(angleIfNeeded);
                AffineTransform originalTransform = g2d.getTransform();
                g2d.rotate(-radians, x2, y2);
                g2d.fill(generalPath);
                g2d.setTransform(originalTransform);
            } else {
                g2d.fill(generalPath);
            }

        }


        if (treeLayoutProperties.isShowNodeBranchLength()) {
            g2d.setColor(Color.black);
            g2d.setFont(treeLayoutProperties.getGlobalFont());

            FontMetrics fontMetrics = g2d.getFontMetrics();
            int height = fontMetrics.getAscent();
            double angleIfNeeded = node.getAngleIfNeeded();
            String drawString = String.valueOf(node.getRealBranchLength());
            int stringWidth = fontMetrics.stringWidth(drawString);

            double x_parent = node.getXParent();
            int xx = (int) (0.5 * (x2 + x_parent - stringWidth));
            int yy = (int) (y2 - 0.5 * height);

            if (angleIfNeeded != 0) {// 如果存在旋转角，就旋转
                double radians = Math.toRadians(angleIfNeeded);
                AffineTransform originalTransform = g2d.getTransform();
                g2d.rotate(-radians, x2, y2);
                g2d.drawString(drawString, xx, yy);
                g2d.setTransform(originalTransform);
            } else {
                g2d.drawString(drawString, xx, yy);
            }
        }

    }

    /**
     *
     * 因为CGB2.0和CGB1.0 collapseProportyMaps的记录位置不同，故将此部分分离出来
     *
     * @title getCollapseProporty
     * @createdDate 2022-11-30 11:18
     * @lastModifiedDate 2022-11-30 11:18
     * @author yjn
     * @since 1.7
     *
     * @param node
     * @return
     * @return CollapseProperty
     */
    protected CollapseProperty getCollapseProporty(GraphicsNode node) {
        Map<Integer, CollapseProperty> collapseProportyMaps = treeLayoutProperties.getCollapsePropertyMaps();
        CollapseProperty collapseProperty = collapseProportyMaps.get(node.getID());
        return collapseProperty;
    }

    protected void drawInnerNodeProperties(Graphics2D g2d, GraphicsNode node) {
        TreeDrawUnit drawUnit = node.getDrawUnit();
        int circleRadius = drawUnit.getCircleRadius();

        double x2 = node.getXSelf();
        double y2 = node.getYSelf();

        g2d.setColor(drawUnit.getLineColor());

        DrawUtil.drawRoundClassifiedColor(g2d, (int) x2, (int) y2, circleRadius);

        if (drawUnit.isDrawName() && !node.isNameBootstrap()) {
            g2d.setColor(Color.black);
            g2d.setFont(treeLayoutProperties.getGlobalFont());

            FontMetrics fontMetrics = g2d.getFontMetrics();
            int height = fontMetrics.getAscent();
            double angleIfNeeded = node.getAngleIfNeeded();

            int xx = (int) (x2 + circleRadius + 4);
            int yy = (int) (y2 + 0.5 * height);

            String drawString = node.getName();
            if (!Strings.isNullOrEmpty(drawString)) {
                if (angleIfNeeded != 0) {// 如果存在旋转角，就旋转
                    double radians = Math.toRadians(angleIfNeeded);
                    AffineTransform originalTransform = g2d.getTransform();
                    g2d.rotate(-radians, x2, y2);
                    g2d.drawString(drawString, xx, yy);
                    g2d.setTransform(originalTransform);
                } else {
                    g2d.drawString(drawString, xx, yy);
                }
            }
        }

    }

    /**
     * 如果需要的话给叶子加上标签。 Just draw the name
     *
     * @param g2d
     * @param node
     * @param x2
     * @param y2
     */
    protected void drawEachLeafNameGraphicsIfNeeded(Graphics2D g2d, GraphicsNode node, double x2, double y2) {

        TreeDrawUnit drawUnit = node.getDrawUnit();
        g2d.setColor(drawUnit.getLineColor());
        int circleRadius = drawUnit.getCircleRadius();

        // 处理折叠
        if (!node.getCollapse()) {
            DrawUtil.drawRoundClassifiedColor(g2d, (int) x2, (int) y2, circleRadius);
        }

        if (!drawUnit.isDrawName()) {
            return;
        }

        if (node.getChildCount() != 0) {
            return;
        }
        // 如果isShowLeaveLabel()为true就显示leave label
        String drawString = node.getName();

        if (Strings.isNullOrEmpty(drawString)) {
            return;
        }
        // 为什么还要判定 getChildCount()呢？因为这里有可能是折叠的节点
        // 如果是叶子节点才画leave label

        AnnotationsProperties annotationsProperties = phylogeneticTreePanel.getMainAnnotationsProperties();
        Map<Integer, Color> leafNameRenderMap = annotationsProperties.getLeafNameRenderMap();

        AnnotationsProperties4LinageType linageTypeAnnotationsProperties = phylogeneticTreePanel
                .getLinageTypeAnnotationsProperties();
        Map<Integer, Color> leafNameRenderMap2 = linageTypeAnnotationsProperties.getLeafNameRenderMap();

        g2d.setColor(Color.black);

        g2d.setFont(treeLayoutProperties.getGlobalFont());

        if (usesTipLabels()) {
            TipLabelGeometry placement = TipLabelGeometry.at(node.getAngleIfNeeded(), circleRadius,
                    drawString, g2d.getFontMetrics());
            Graphics2D labelGraphics = (Graphics2D) g2d.create();
            try {
                labelGraphics.translate(x2, y2);
                labelGraphics.rotate(placement.rotation());
                Color background = leafNameRenderMap.get(node.getID());
                if (background == null) background = leafNameRenderMap2.get(node.getID());
                if (background != null) {
                    labelGraphics.setColor(background);
                    labelGraphics.fill(placement.box());
                }
                labelGraphics.setColor(Color.black);
                labelGraphics.drawString(drawString, (float) placement.x(), (float) placement.baseline());
            } finally {
                labelGraphics.dispose();
            }
            return;
        }

        boolean shouldLeafNameRightAlign = treeLayoutProperties.isShouldLeafNameRightAlign();

        FontMetrics fontMetrics = g2d.getFontMetrics();
        int height = fontMetrics.getAscent();
        double angleIfNeeded = node.getAngleIfNeeded();
        int xx = (int) (x2 + circleRadius + 4);

        if (shouldLeafNameRightAlign) {
            xx = xx + maxLeafNameLength - fontMetrics.stringWidth(drawString);
        }

        int yy = (int) (y2 + 0.5 * height);

        // Special case: angle = 180 means draw text on LEFT side (for 180° rotated Slant layout)
        if (angleIfNeeded == 180 || angleIfNeeded == -180) {
            int stringWidth = fontMetrics.stringWidth(drawString);
            int xxLeft = (int) (x2 - circleRadius - 4 - stringWidth);
            g2d.drawString(drawString, xxLeft, yy);

            Color color = leafNameRenderMap.get(node.getID());
            if (color != null) {
                g2d.setColor(color);
                g2d.fillRect(xxLeft, yy - height, stringWidth, height);
            } else {
                color = leafNameRenderMap2.get(node.getID());
                if (color != null) {
                    g2d.setColor(color);
                    g2d.fillRect(xxLeft, yy - height, stringWidth, height);
                }
            }
        } else if (angleIfNeeded != 0) {// 如果存在旋转角，就旋转
            double radians = Math.toRadians(angleIfNeeded);
            AffineTransform originalTransform = g2d.getTransform();
            g2d.rotate(-radians, x2, y2);
            g2d.drawString(drawString, xx, yy);
            Color color = leafNameRenderMap.get(node.getID());
            if (color != null) {
                int stringWidth = fontMetrics.stringWidth(drawString);
                g2d.setColor(color);
                g2d.fillRect(xx, yy - height, stringWidth, height);
            } else {
                color = leafNameRenderMap2.get(node.getID());
                if (color != null) {
                    int stringWidth = fontMetrics.stringWidth(drawString);
                    g2d.setColor(color);
                    g2d.fillRect(xx, yy - height, stringWidth, height);
                }
            }

            g2d.setTransform(originalTransform);
        } else {
            g2d.drawString(drawString, xx, yy);

            Color color = leafNameRenderMap.get(node.getID());
            if (color != null) {
                int stringWidth = fontMetrics.stringWidth(drawString);
                g2d.setColor(color);
                g2d.fillRect(xx, yy - height, stringWidth, height);
            } else {
                color = leafNameRenderMap2.get(node.getID());
                if (color != null) {
                    int stringWidth = fontMetrics.stringWidth(drawString);
                    g2d.setColor(color);
                    g2d.fillRect(xx, yy - height, stringWidth, height);
                }
            }

        }

    }

    /**
     * 左上角的比例尺
     * @param g2d
     * @param scaleBarProperty
     */
    private void drawScaleBar(Graphics2D g2d, ScaleBarProperty scaleBarProperty) {

        g2d.setFont(treeLayoutProperties.getAxisFont());
        FontMetrics metrics = g2d.getFontMetrics();
        double target = Math.min(80, Math.max(1, blankArea.getWorkWidth((int) currentWidth)));
        double value = BranchLengthTicks.niceFloor(Math.min(maximumScaleDistance, target / canvas2logicRatio));
        if (!(value > 0)) return;
        String unit = Objects.toString(treeLayoutProperties.getBranchLengthUnit(), "");
        String label = BranchLengthTicks.format(value) + (unit.isBlank() ? "" : " " + unit);
        scaleBarProperty.setDefaultLocation(Math.max(12, treeLayoutProperties.getConfiguredBlankArea().getLeft()),
                treeLayoutProperties.isShowWidthAndHeightString() ? 57 : 12);
        scaleBarProperty.configure(value, value * canvas2logicRatio, label, metrics, (int) currentWidth, (int) currentHeight);
        java.awt.geom.Rectangle2D bounds = scaleBarProperty.getBounds();
        g2d.setColor(phylogeneticTreePanel.getHostComponent().getBackground());
        g2d.fill(bounds);
        g2d.setColor(Color.black);
        g2d.setStroke(new BasicStroke(2.0f));
        double x = bounds.getX() + (bounds.getWidth() - scaleBarProperty.getPixelLength()) / 2;
        double y = bounds.getY() + 2;
        double endX = x + scaleBarProperty.getPixelLength();
        g2d.draw(new Line2D.Double(x, y, x, y + 10));
        g2d.draw(new Line2D.Double(endX, y, endX, y + 10));
        g2d.draw(new Line2D.Double(x, y + 5, endX, y + 5));
        g2d.drawString(label, (float) (bounds.getCenterX() - metrics.stringWidth(label) / 2.0),
                (float) (y + 12 + metrics.getAscent()));
    }

    /**
     *
     * <p>
     * Title: paintGraphics
     * </p>
     * <p>
     * Description: 这里的抽象类，提供一个基础的实现，绘制的时候会有开头与结尾有一些公共的绘制流程。
     * </p>
     *
     * @param g2d
     * @see ITreeLayout#paintGraphics(Graphics2D)
     *
     */
    @Override
    public void paintGraphics(Graphics2D g2d) {
        specificTreeDrawingProcess(g2d);
        firstRecursiveDraw(g2d, rootNode);

        maxLeafNameLength = getMaxLengthLeafNameWidthAccording2CurrentFont();
        lastRecursiveDraw(g2d, rootNode);
        // yjn:选中节点的绘制得放在第三次遍历中得进行，否则会被点绘制覆盖一些
        recursiveDrawSelectedNode(g2d, rootNode);
        commonTailInDrawingProcess(g2d);
        drawBootstrapLabels(g2d, rootNode);
        drawScaleBar(g2d);
    }

    /**
     *
     * 每个子类只需要绘制公共需要绘制的东西。
     *
     * @title treeDrawingProcess
     * @createdDate 2020-11-05 21:13
     * @lastModifiedDate 2020-11-05 21:13
     * @author yudalang
     * @param g2d
     *
     * @since 1.7
     *
     * @return void
     */
    protected abstract void specificTreeDrawingProcess(Graphics2D g2d);

    /**
     * 20220915 yjn:这个方法里只进行叶子节点的线的绘制。叶子节点的点的绘制为方法drawEachLeafNameGraphicsIfNeeded()
     */
    protected abstract void leafLineDrawingProcess(Graphics2D g2d, GraphicsNode node);

    protected abstract void rootDrawingProcess(Graphics2D g2d, GraphicsNode node);

    protected abstract void innerNodeDrawingProcess(Graphics2D g2d, GraphicsNode node);

    /**
     * 递归的画每个节点，从而画树
     *
     * @param node
     */
    private void firstRecursiveDraw(Graphics2D g2d, GraphicsNode node) {
        TreeDrawUnit drawUnit = node.getDrawUnit();

        NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);
        switch (nodeType) {
            case LEAF:
                boolean shouldDraw = false;
                if (drawUnit.isCollapse()) {
                    shouldDraw = true;
                } else {
                    // Draw the node if node not hide!
                    shouldDraw = !node.hideNode();
                }
                if (shouldDraw) {
                    // 这里先只画叶子节点的线，叶子节点的点再lastRecursiveDraw()里绘制，以防止后绘制的节点的线覆盖先绘制节点的点
                    leafLineDrawingProcess(g2d, node);
                }
                break;
            default:
                if (nodeType == NodeType.ROOT) {
                    rootDrawingProcess(g2d, node);
                } else {
                    shouldDraw = false;
                    if (drawUnit.isCollapse()) {
                        shouldDraw = true;
                    } else {
                        // Draw the node if node not hide!
                        shouldDraw = !node.hideNode();
                    }
                    if (shouldDraw) {
                        innerNodeDrawingProcess(g2d, node);
                    }
                }

                for (int i = 0; i < node.getChildCount(); i++) {
                    firstRecursiveDraw(g2d, (GraphicsNode) node.getChildAt(i));
                }
                break;
        }
    }

    protected TracingPaintingProperty configurateTracingShape(GraphicsNode node, Color color) {

        TracingPaintingProperty tracingPaintingProperty = new TracingPaintingProperty();
        GeneralPath generalPath = new GeneralPath();

        double xSelf = node.getXSelf();
        double ySelf = node.getYSelf();

        TreeDrawUnit drawUnit = node.getDrawUnit();

        generalPath.moveTo(xSelf, ySelf);

        drawUnit.setNodeSelected(true);

        GraphicsNode tempNode = node;
        while (tempNode.getParentCount() > 0) {
            generalPath.lineTo(tempNode.getXParent(), tempNode.getYParent());
            tempNode = (GraphicsNode) tempNode.getParent();
            generalPath.lineTo(tempNode.getXSelf(), tempNode.getYSelf());
        }

        tracingPaintingProperty.path = generalPath;
        tracingPaintingProperty.pointOfNode = new Point2D.Double(node.getXSelf(), node.getYSelf());
        tracingPaintingProperty.fillColor = color;

        return tracingPaintingProperty;
    }

    /**
     * 画顶上显示样本量的title 现在移到底部了，一个数据说明
     *
     * @param g2d
     */
    public void drawFixedTitle(Graphics2D g2d, int width, int height) {
        if (!is4AnnotationDialog && treeLayoutProperties.isShowTitle()) {
            g2d.setColor(new Color(255, 235, 205, 200));
            int threeDRectHeight = 30;

            g2d.fill3DRect(0, (int) (height - threeDRectHeight), (int) width, threeDRectHeight, true);

            Font titleFont = treeLayoutProperties.getTitleFont();
            g2d.setFont(titleFont);
            g2d.setColor(Color.darkGray);

            DecimalFormat df = new DecimalFormat(",###,###");

            String titleString = treeLayoutProperties.getTitleString();
            if (titleString == null){
                return;
            }
            String drawString = MessageFormat.format(titleString, df.format(numOfLeavesWithoutOutgroup));

            FontMetrics fontMetrics = g2d.getFontMetrics();

            int startXAxis = (int) (0.5 * width - 0.5 * fontMetrics.stringWidth(drawString));
            g2d.drawString(drawString, startXAxis,
                    (int) (height - 0.5 * threeDRectHeight + 0.5 * fontMetrics.getHeight() - 3));

        }
    }

    /**
     * Now drawing the annotation and lineage tracing highlight!
     *
     * @param g2d
     */
    private void commonTailInDrawingProcess(Graphics2D g2d) {

        if (isShowAnnotation) {
            AnnotationsProperties annotationsProperties = phylogeneticTreePanel.getMainAnnotationsProperties();

            List<DrawPropInternalNode2LeafAnno> internalNode2LeafAnnos = annotationsProperties
                    .getInternalNode2LeafAnnos();
            for (DrawPropInternalNode2LeafAnno tt : internalNode2LeafAnnos) {
                if (tt.shouldConfigurateAndPaint()) {
                    tt.drawAnnotation(g2d);
                }
            }

            List<DrawPropOutterSidewardAnno> outterSidewardAnnos = annotationsProperties.getOutterSidewardAnnos();
            for (DrawPropOutterSidewardAnno tt : outterSidewardAnnos) {
                if (tt.shouldConfigurateAndPaint()) {
                    tt.drawAnnotation(g2d);
                }
            }
            List<DrawPropInternalNodeInsituAnno> internalNode2LeafInsituAnnos = annotationsProperties
                    .getInternalNode2LeafInsituAnnos();
            for (DrawPropInternalNodeInsituAnno tt : internalNode2LeafInsituAnnos) {
                if (tt.shouldConfigurateAndPaint()) {
                    tt.drawAnnotation(g2d);
                }
            }
        }

        if (isShowAnnotation) {
            AnnotationsProperties4LinageType linageTypeAnnotationsProperties = phylogeneticTreePanel
                    .getLinageTypeAnnotationsProperties();
            // In-situ annotations
            List<DrawPropInternalNodeInsituAnno> linageTypeinternalNode2LeafInsituAnnos = linageTypeAnnotationsProperties
                    .getInternalNode2LeafInsituAnnos();
            for (DrawPropInternalNodeInsituAnno tt : linageTypeinternalNode2LeafInsituAnnos) {
                tt.drawAnnotation(g2d);
            }
            // Side ward annotation
            List<DrawPropOutterSidewardAnno4LinageType> outterSidewardAnnos = linageTypeAnnotationsProperties
                    .getOutterSidewardAnnos();
            for (DrawPropOutterSidewardAnno4LinageType tt : outterSidewardAnnos) {
                tt.drawAnnotation(g2d);
            }
            // node2leaf annotation
            List<DrawPropOutterSidewardAnno4LinageType> internalNode2leafAnno4LinageTypes = linageTypeAnnotationsProperties
                    .getInternalNode2leafAnno4LinageTypes();
            for (DrawPropOutterSidewardAnno4LinageType tt : internalNode2leafAnno4LinageTypes) {
                tt.drawAnnotation(g2d);
            }

            g2d.setStroke(new BasicStroke(1f));
            g2d.setFont(annotationFont);
            Map<String, Color> categ2colorMap = linageTypeAnnotationsProperties.getCateg2colorMap();
            if (linageTypeAnnotationsProperties.getLeafNameRenderMap().size() > 0 && categ2colorMap.size() > 0) {
                List<TwoTuple<String, Color>> list = new ArrayList<TwoTuple<String, Color>>();

                for (Entry<String, Color> entry : categ2colorMap.entrySet()) {
                    list.add(new TwoTuple<String, Color>(entry.getKey(), entry.getValue()));
                }
                DrawUtil.drawLegend4Annotations(currentWidth, currentHeight, list, g2d);
            }
            // 这两者应该互斥
            Map<String, SidewardNodeAnnotation> categlory2SidewardNodeAnnotation = linageTypeAnnotationsProperties
                    .getCateglory2SidewardNodeAnnotation();
            if (categlory2SidewardNodeAnnotation.size() > 0) {
                List<TwoTuple<String, Color>> list = new ArrayList<TwoTuple<String, Color>>();
                for (Entry<String, SidewardNodeAnnotation> entry : categlory2SidewardNodeAnnotation.entrySet()) {
                    SidewardNodeAnnotation value = entry.getValue();
                    Color lineColor = value.getLineColor();
                    list.add(new TwoTuple<String, Color>(entry.getKey(), lineColor));
                }
                DrawUtil.drawLegend4Annotations(currentWidth, currentHeight, list, g2d);
            }

        }

        // highlight lineages

        if (!tracingPaintingShapes.isEmpty()) {
            BasicStroke basicStroke = new BasicStroke(2f);
            g2d.setStroke(basicStroke);

            for (TracingPaintingProperty bs : tracingPaintingShapes) {
                g2d.setColor(bs.fillColor);
                g2d.draw(bs.path);

                int circleRadius = DrawUtil.size;
                Double double1 = new Double(bs.pointOfNode.x - circleRadius,
                        bs.pointOfNode.y - circleRadius, 2 * circleRadius, 2 * circleRadius);
                g2d.fill(double1);

//				g2d.setColor(Color.black);
//				g2d.draw(double1);

            }
        }

    }

    protected Rectangle bootstrapLabelBounds(Graphics2D graphics, GraphicsNode node, String label) {
        FontMetrics metrics = graphics.getFontMetrics(treeLayoutProperties.getGlobalFont());
        int radius = node.getDrawUnit().getCircleRadius();
        int gap = radius + 5;
        double dx = node.getXSelf() - node.getXParent();
        double dy = node.getYSelf() - node.getYParent();
        double length = Math.hypot(dx, dy);
        double nx = length == 0 ? 0 : -dy / length;
        double ny = length == 0 ? -1 : dx / length;
        if (ny > 0) { nx = -nx; ny = -ny; }
        boolean named = node.getDrawUnit().isDrawName() && !node.isNameBootstrap()
                && node.getName() != null && !node.getName().isEmpty();
        int rows = (named ? 1 : 0) + (treeLayoutProperties.isShowNodeBranchLength() ? 1 : 0);
        double offset = gap + (rows + .5) * metrics.getHeight();
        int width = metrics.stringWidth(label);
        int x = (int) Math.round(node.getXSelf() + nx * offset - width / 2.0);
        int y = (int) Math.round(node.getYSelf() + ny * offset - metrics.getHeight() / 2.0);
        x = Math.max(1, Math.min(x, (int) currentWidth - width - 1));
        y = Math.max(1, Math.min(y, (int) currentHeight - metrics.getHeight() - 1));
        return new Rectangle(x, y, width, metrics.getHeight());
    }

    private void drawBootstrapLabels(Graphics2D graphics, GraphicsNode node) {
        if (node.getChildCount() == 0) return;
        if (treeLayoutProperties.getShowInnerNodePropertiesInfo().isShowInternalNodeBootstrap()
                && node.hasBootstrapValue() && !node.hideNode()) {
            String label = java.math.BigDecimal.valueOf(node.getBootstrapValue()).stripTrailingZeros().toPlainString();
            Graphics2D text = (Graphics2D) graphics.create();
            try {
                text.setFont(treeLayoutProperties.getGlobalFont());
                Rectangle box = bootstrapLabelBounds(text, node, label);
                int baseline = box.y + text.getFontMetrics().getAscent();
                Shape outline = new java.awt.font.TextLayout(label, text.getFont(), text.getFontRenderContext())
                        .getOutline(AffineTransform.getTranslateInstance(box.x, baseline));
                text.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                text.setColor(Color.white);
                text.draw(outline);
                text.setColor(Color.black);
                text.drawString(label, box.x, baseline);
            } finally { text.dispose(); }
        }
        if (!node.getCollapse()) {
            for (int i = 0; i < node.getChildCount(); i++) drawBootstrapLabels(graphics, (GraphicsNode) node.getChildAt(i));
        }
    }

    private void lastRecursiveDraw(Graphics2D g2d, GraphicsNode node) {
        NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);

        if (nodeType == NodeType.LEAF) {
            if (!node.hideNode()) {
                drawEachLeafNameGraphicsIfNeeded(g2d, node, node.getXSelf(), node.getYSelf());
            }
            // 这里要return掉，不然collapse掉的点还会递归下去。collapse后用NodeType判定会判定为LEAF
            return;
        } else {
            // Internal node
            drawInnerNodeProperties(g2d, node);

        }

        for (int i = 0; i < node.getChildCount(); i++) {
            lastRecursiveDraw(g2d, (GraphicsNode) node.getChildAt(i));
        }

    }

    private void recursiveDrawSelectedNode(Graphics2D g2d, GraphicsNode node) {

        double x1 = node.getXParent();
        double y1 = node.getYParent();

        double x2 = node.getXSelf();
        double y2 = node.getYSelf();

        TreeDrawUnit drawUnit = node.getDrawUnit();

        if (drawUnit.isBranchSelected()) {
            paintSelectedRectangularSignatureDashLine(g2d, x1, y1);
            paintSelectedRectangularSignatureDashLine(g2d, x2, y2);
        }

        if (drawUnit.isNodeSelected()) {
            DrawUtil.paintSelectedRectangularSignatureSolidLine(g2d, x2, y2);
        }

        NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);

        if (nodeType == NodeType.LEAF) {
            // collapse后用NodeType判定会判定为LEAF。这里要return掉，这样collapse掉的内节点不会递归下去。
            return;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            recursiveDrawSelectedNode(g2d, (GraphicsNode) node.getChildAt(i));
        }

    }

    public double getCanvas2logicRatio() {
        return canvas2logicRatio;
    }

    public BlankArea getBlankArea() {
        return blankArea;
    }
}
