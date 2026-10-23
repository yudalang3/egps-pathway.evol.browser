package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.NodeType;
import module.evolview.phylotree.visualization.graphics.struct.TreeDecideUtil;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator.LongestRoot2leafBean;
import org.apache.commons.math3.util.FastMath;

import java.awt.*;
import java.awt.geom.*;

public class RectHalfCircleLayout extends RectPhyloLayout {
    @Override
    protected boolean hasCoordinateAxis() { return false; }

	private int halfWorkWidth;
	private int halfWorkRadius;
	private int halfWorkHeight;
	private Point2D.Double topCircleCenter = new Point2D.Double();
	private Point2D.Double canvasCenter = new Point2D.Double();

	private double globalRadicus;
	private double maxJudgeAngle;
	// 第一个矩形边的分割点
	private int rectH;
	// 矩形边加上半圆
	private double highBound;
	private double totalAvaliableLength;

	private Line2D.Double lineDrawUtil = new Line2D.Double();
	Arc2D.Double arc2d = new Arc2D.Double();

	public RectHalfCircleLayout(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}

	@Override
	protected void beforeCalculate(int width, int height) {
		super.beforeCalculate(width, height);

		// 计算方式

		int workWidth = blankArea.getWorkWidth((int) currentWidth);
		int workHeight = blankArea.getWorkHeight((int) currentHeight);
		globalRadicus = 0.5 * workHeight;

		canvasCenter.setLocation(blankArea.getLeft() + 0.5 * workWidth, blankArea.getTop() + 0.5 * workHeight);
		// 黄金比例
//		halfWorkRadius = (int) (0.382 * workHeight);
		halfWorkRadius = (int) (0.3 * workHeight);


		halfWorkWidth = halfWorkRadius;
		rectH = workHeight - halfWorkRadius;
		halfWorkHeight = (int) (0.5 * rectH);
		totalAvaliableLength = rectH + FastMath.PI * halfWorkRadius;
		highBound = rectH + FastMath.PI * halfWorkRadius;
		totalAvaliableLength += rectH;

//		System.out.println(rectH + "\t" + (FastMath.PI * halfWorkRadius));

		topCircleCenter.setLocation(canvasCenter.getX(), halfWorkRadius + blankArea.getTop());

		maxJudgeAngle = FastMath.atan2(halfWorkRadius, globalRadicus);
//		startAngleInRadian = maxJudgeAngle - FastMath.PI;
//		endAngleInRadian = FastMath.PI - maxJudgeAngle;
//		totalAngleInRadian = endAngleInRadian + endAngleInRadian;

		loop = 0;
		// x axis
		LongestRoot2leafBean maxLengthOfRoot2LeafBean = GraphicTreePropertyCalculator.getMaxLengthOfRoot2Leaf(rootNode);
		double maxLengthOfRoot2Leaf = maxLengthOfRoot2LeafBean.getLength();

		canvas2logicRatio = maxLengthOfRoot2Leaf > 0 ? halfWorkRadius / maxLengthOfRoot2Leaf : 0;
		// y axis
		eachYGapLength = totalAvaliableLength / Math.max(1.0, GraphicTreePropertyCalculator.getLeafNumber(rootNode) - 1.0);

		configureBottomScaleBarDrawProperty(maxLengthOfRoot2LeafBean.getLength(), maxLengthOfRoot2LeafBean.getLeaf(),workWidth);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		beforeCalculate(width, height);

		GraphicsNode root = rootNode;
		// 将root的branchLength设置为0；否则会影响可视化
//		root.setDisplayedBranchLength(0);
		double storedRootLength = root.getDisplayedBranchLength();
        root.setDisplayedBranchLength(0);
        try { recursiveCalculate(root, 0); } finally { root.setDisplayedBranchLength(storedRootLength); }

		this.scaleBarProperty.setIfDrawScaleBar(true);

		afterCalculation();
	}

	protected void recursiveCalculate(GraphicsNode node, double cumulateBranchLengthExceptCurrent) {
		double parentLineageLength = cumulateBranchLengthExceptCurrent * canvas2logicRatio;
		double lineageLength = parentLineageLength + node.getDisplayedBranchLength() * canvas2logicRatio;

		// 这个 Radicus 用来存储 lineageLength
		node.setRadicusIfNeeded(lineageLength);

		NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);
		switch (nodeType) {
		case LEAF:
			double currYAxisLength = loop * eachYGapLength;
			node.setDoubleVariable(currYAxisLength);
			assignValue(node, lineageLength, currYAxisLength);
			assignValue4XYParent(node, parentLineageLength, currYAxisLength);
//			System.out.println(node.getYSelf() + "\t" + node.getYParent());

			loop++;
			break;
		default:
			for (int i = 0; i < node.getChildCount(); i++) {
				recursiveCalculate((GraphicsNode) node.getChildAt(i),
						cumulateBranchLengthExceptCurrent + node.getDisplayedBranchLength());
			}
			currYAxisLength = treeLayoutProperties.averageValueOfChildrenForParent(node);
			node.setDoubleVariable(currYAxisLength);

			assignValue(node, lineageLength, currYAxisLength);
			assignValue4XYParent(node, parentLineageLength, currYAxisLength);

			if (nodeType == NodeType.ROOT) {
				node.setXParent(canvasCenter.getX());
			}
			break;
		}
	}

	public void assignValue(GraphicsNode node, double lineageLength, double currYAxisLength) {
        Point2D.Double point = pointOnPerimeter(lineageLength, currYAxisLength);
        if (currYAxisLength >= rectH && currYAxisLength < highBound)
            node.setAngleIfNeeded((currYAxisLength - rectH) / halfWorkRadius);
        node.setXSelf(point.x);
        node.setYSelf(point.y);
	}

	public void assignValue4XYParent(GraphicsNode node, double lineageLength, double currYAxisLength) {
        Point2D.Double point = pointOnPerimeter(lineageLength, currYAxisLength);
        node.setXParent(point.x);
        node.setYParent(point.y);
	}

    private Point2D.Double pointOnPerimeter(double radius, double perimeter) {
        if (perimeter < rectH)
            return new Point2D.Double(canvasCenter.x-radius, currentHeight-blankArea.getBottom()-perimeter);
        if (perimeter < highBound) {
            double angle=(perimeter-rectH)/halfWorkRadius;
            return new Point2D.Double(topCircleCenter.x-radius*Math.cos(angle), topCircleCenter.y-radius*Math.sin(angle));
        }
        return new Point2D.Double(canvasCenter.x+radius, currentHeight-blankArea.getBottom()-(totalAvaliableLength-perimeter));
    }
    private Path2D annotationPath(double radius, double start, double end) {
        Path2D path=new Path2D.Double();
        int steps=Math.max(1,(int)Math.ceil(Math.abs(end-start)/3));
        for(int i=0;i<=steps;i++) {
            Point2D point=pointOnPerimeter(radius,start+(end-start)*i/steps);
            if(i==0) path.moveTo(point.getX(),point.getY()); else path.lineTo(point.getX(),point.getY());
        }
        return path;
    }
    @Override
    protected void configurateMainAnnotation(module.evolview.phylotree.visualization.annotation.AnnotationsProperties properties) {
        super.configurateMainAnnotation(properties);
        for(var annotation:properties.getInternalNode2LeafAnnos()) {
            if(!annotation.shouldConfigurateAndPaint()) continue;
            var node=annotation.getCurrentGraphicsNode();
            var leaves=GraphicTreePropertyCalculator.getLeaves(node);
            double start=leaves.getFirst().getDoubleVariable(), end=leaves.getLast().getDoubleVariable();
            if(start==end) { annotation.setDrawShape(new Ellipse2D.Double(node.getXSelf()-3,node.getYSelf()-3,6,6)); continue; }
            double outer=leaves.stream().mapToDouble(GraphicsNode::getRadicusIfNeeded).max().orElse(0);
            Path2D region=annotationPath(outer,start,end);
            region.append(annotationPath(node.getRadicusIfNeeded(),end,start),true);
            region.closePath(); annotation.setDrawShape(region);
        }
        for(var annotation:properties.getOutterSidewardAnnos()) {
            if(!annotation.shouldConfigurateAndPaint()) continue;
            var leaves=GraphicTreePropertyCalculator.getLeaves(annotation.getCurrentGraphicsNode());
            double start=leaves.getFirst().getDoubleVariable(), end=leaves.getLast().getDoubleVariable();
            if(start==end) { start-=3; end+=3; }
            double radius=leaves.stream().mapToDouble(GraphicsNode::getRadicusIfNeeded).max().orElse(0)+10;
            Point2D point=pointOnPerimeter(radius+10,(start+end)/2);
            annotation.setTextLocationAndShape(point.getX(),point.getY(),annotationPath(radius,start,end));
        }
    }

	@Override
	public void paintGraphics(Graphics2D g2d) {
		g2d.setColor(rootNode.getDrawUnit().getLineColor());
		g2d.setFont(treeLayoutProperties.getGlobalFont());



		drawAnnotationBackgrounds(g2d);
		recursiveDraw(rootNode, g2d);
		commonTailInDrawingProcess(g2d);
        drawScaleBar(g2d);
	}

	private void recursiveDraw(GraphicsNode node, Graphics2D g2d) {
		NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);
		switch (nodeType) {
		case LEAF:
			double xSelf = node.getXSelf();
			double ySelf = node.getYSelf();
			g2d.fillOval((int) (xSelf - 2), (int) (ySelf - 2), 4, 4);

			if (treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel()) {
				drawEachLeafNameGraphicsIfNeeded(g2d, node, xSelf, ySelf);
			}

			lineDrawUtil.setLine(xSelf, ySelf, node.getXParent(), node.getYParent());
			g2d.draw(lineDrawUtil);
			break;
		default:

			xSelf = node.getXSelf();
			ySelf = node.getYSelf();
			g2d.fillOval((int) (xSelf - 2), (int) (ySelf - 2), 4, 4);
			if (treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel()) {
				g2d.drawString(node.getName(), (int) (xSelf - 2), (int) (ySelf - 2));
			}

			lineDrawUtil.setLine(xSelf, ySelf, node.getXParent(), node.getYParent());
			g2d.draw(lineDrawUtil);
			
			GraphicsNode firstNode = null;
			GraphicsNode lastNode = null;
			for (int i = 0; i < node.getChildCount(); i++) {
				GraphicsNode child = (GraphicsNode) node.getChildAt(i);

				recursiveDraw(child, g2d);
				if (firstNode == null) {
					firstNode = child;
				}
				lastNode = child;
			}

			// 这个 Radicus 用来存储 lineageLength
			drawLineOfSiblingConnection(firstNode, lastNode, node.getRadicusIfNeeded(), g2d);

			if (nodeType == NodeType.ROOT) {

			}
			break;
		}
	}

	private void drawLineOfSiblingConnection(GraphicsNode firstNode, GraphicsNode lastNode,
			double parentCurrYAxisLength, Graphics2D g2d) {
		double firstNodeXParent = firstNode.getXParent();
		double firstNodeYParent = firstNode.getYParent();
		double lastNodeXParent = lastNode.getXParent();
		double lastNodeYParent = lastNode.getYParent();

		double x1 = 0, y1 = 0, x2 = 0, y2 = 0;

		double firstCurrYAxisLength = firstNode.getDoubleVariable();
		double lastCurrYAxisLength = lastNode.getDoubleVariable();

		if (firstCurrYAxisLength < rectH) {
			if (lastCurrYAxisLength < rectH) {
				// 1
				x1 = firstNodeXParent;
				y1 = firstNodeYParent;
				x2 = lastNodeXParent;
				y2 = lastNodeYParent;
			} else if (lastCurrYAxisLength < highBound) {
				// 2
				x1 = firstNodeXParent;
				y1 = firstNodeYParent;
				x2 = firstNodeXParent;
				y2 = halfWorkRadius + blankArea.getTop();
				// others
				arc2d.setFrameFromCenter(topCircleCenter.getX(), topCircleCenter.getY(),
						topCircleCenter.getX() - parentCurrYAxisLength, topCircleCenter.getY() - parentCurrYAxisLength);

				arc2d.setAngles(lastNodeXParent, lastNodeYParent, x2, y2);
				g2d.draw(arc2d);
			} else {
				// 3

			}
		} else if (firstCurrYAxisLength < highBound) {
			if (lastCurrYAxisLength < highBound) {
				// 4
				arc2d.setFrameFromCenter(topCircleCenter.getX(), topCircleCenter.getY(),
						topCircleCenter.getX() - parentCurrYAxisLength, topCircleCenter.getY() - parentCurrYAxisLength);

				arc2d.setAngles(lastNodeXParent, lastNodeYParent, firstNodeXParent, firstNodeYParent);
				g2d.draw(arc2d);
			} else {
				// 5
				x1 = lastNodeXParent;
				y1 = halfWorkRadius + blankArea.getTop();
				x2 = lastNodeXParent;
				y2 = lastNodeYParent;
				// others
				arc2d.setFrameFromCenter(topCircleCenter.getX(), topCircleCenter.getY(),
						topCircleCenter.getX() - parentCurrYAxisLength, topCircleCenter.getY() - parentCurrYAxisLength);

				arc2d.setAngles(lastNodeXParent, y1, firstNodeXParent, firstNodeYParent);
				g2d.draw(arc2d);
			}
		} else {
			// 6
			x1 = firstNodeXParent;
			y1 = firstNodeYParent;
			x2 = lastNodeXParent;
			y2 = lastNodeYParent;
		}

		lineDrawUtil.setLine(x1, y1, x2, y2);
		g2d.draw(lineDrawUtil);
	}

}
