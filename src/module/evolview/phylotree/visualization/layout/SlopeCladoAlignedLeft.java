package module.evolview.phylotree.visualization.layout;

import evoltree.struct.util.EvolNodeUtil;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.annotation.*;
import module.evolview.phylotree.visualization.graphics.struct.NodeType;
import module.evolview.phylotree.visualization.graphics.struct.SlopeLayoutProperty;
import module.evolview.phylotree.visualization.graphics.struct.TreeDecideUtil;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import module.evolview.phylotree.visualization.util.DrawUtil;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D.Double;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * 这种layout和枝长没有关系！
 *
 */
public class SlopeCladoAlignedLeft extends RectangularLayout {

	private double realHeight;
	private double realWidth;
	private double reciprocalOfLeafNumbers;
	private double blankMarginRatio;

	public SlopeCladoAlignedLeft(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}
	
	protected boolean isLayoutSupportDrawScaleBar() {
		return false;
	}

	@Override
	public void calculateForPainting(int width, int height) {
		SlopeLayoutProperty slopeLayoutProperety = treeLayoutProperties.getSlopeLayoutProperety();
		int slopeLayoutRotationDeg = slopeLayoutProperety.getSlopeLayoutRotationDeg();

		// For 90° and 270° rotations, use a square bounding box based on the smaller dimension
		// This ensures the tree fits within bounds after rotation
		int calcWidth, calcHeight;
		if (slopeLayoutRotationDeg == 90 || slopeLayoutRotationDeg == 270) {
			int minDim = Math.min(width, height);
			calcWidth = minDim;
			calcHeight = minDim;
		} else {
			calcWidth = width;
			calcHeight = height;
		}

		beforeCalculate(calcWidth, calcHeight);

		rootNode.setDisplayedBranchLength(0);

		if (rootNode.getSize() < 4) {
			EvolNodeUtil.initializeSize(rootNode);
		}
		List<GraphicsNode> leaves = GraphicTreePropertyCalculator.getLeaves(rootNode);
		int leafNumbers = leaves.size();
		reciprocalOfLeafNumbers = 1.0 / leafNumbers;

		blankMarginRatio = slopeLayoutProperety.getBlankMarginRatio();
		double leafLocationRightMostRatio = slopeLayoutProperety
				.getLeafLocationRightMostRatio();
		// first deal with leaves
		int index = 0;
		for (int i = 0; i < leafNumbers; i++) {
			GraphicsNode n = leaves.get(i);
			double yAxis = (index + 0.5) * reciprocalOfLeafNumbers;
			n.setXSelf(leafLocationRightMostRatio);
			n.setYSelf(yAxis);
			n.setAngleIfNeeded(0.0);
			index++;
		}

		prepareStep2AllocateLocationRatio(rootNode);

		realHeight = blankArea.getWorkHeight((int) currentHeight);
		realWidth = blankArea.getWorkWidth((int) currentWidth) - blankMarginRatio * currentWidth;
		iterateTurnDrawRatio2DrawLength(rootNode);

		afterCalculation();

		// For 90°/270°, first translate to center of actual canvas, then rotate
		if (slopeLayoutRotationDeg == 90 || slopeLayoutRotationDeg == 270) {
			// Offset to center the square tree in the actual canvas
			double offsetX = (width - calcWidth) / 2.0;
			double offsetY = (height - calcHeight) / 2.0;
			translateTree(rootNode, offsetX, offsetY);
		}

		// Apply rotation transform around the center of the actual canvas
		DrawUtil.rotationTransform(rootNode, slopeLayoutRotationDeg, width, height);

		// For 180° rotation, reset text angles to 0 so text remains readable
		// (otherwise text would be upside down)
		if (slopeLayoutRotationDeg == 180) {
			setTextAnglesForLeftSide(rootNode);
		}

		// 下面是一些绘图注释的功能，现在先不用
		// transform annotations
		Double dest = new Double();

		if (isShowAnnotation) {
			AnnotationsProperties annotationsProperties = phylogeneticTreePanel.getMainAnnotationsProperties();

			List<DrawPropInternalNode2LeafAnno> internalNode2LeafAnnos = annotationsProperties
					.getInternalNode2LeafAnnos();
			for (DrawPropInternalNode2LeafAnno tt : internalNode2LeafAnnos) {
				if (tt.shouldConfigurateAndPaint()) {
					Shape shape = tt.getDrawShape();
					Shape transformed = DrawUtil.affineTransform.createTransformedShape(shape);
					tt.setDrawShape(transformed);
				}
			}

			List<DrawPropOutterSidewardAnno> outterSidewardAnnos = annotationsProperties.getOutterSidewardAnnos();
			for (DrawPropOutterSidewardAnno tt : outterSidewardAnnos) {
				if (tt.shouldConfigurateAndPaint()) {
					Shape shape = tt.getShape();
					Shape transformed = DrawUtil.affineTransform.createTransformedShape(shape);
					tt.setShape(transformed);

					Double textLocation = tt.getTextLocation();
					DrawUtil.affineTransform.transform(textLocation, dest);
					tt.setTextLocation(dest);
				}
			}
			List<DrawPropInternalNodeInsituAnno> internalNode2LeafInsituAnnos = annotationsProperties
					.getInternalNode2LeafInsituAnnos();
			for (DrawPropInternalNodeInsituAnno tt : internalNode2LeafInsituAnnos) {
				if (tt.shouldConfigurateAndPaint()) {
					Shape shape = tt.getDrawShape();
					Shape transformed = DrawUtil.affineTransform.createTransformedShape(shape);
					tt.setDrawShape(transformed);
				}
			}
		}

		AnnotationsProperties4LinageType linageTypeAnnotationsProperties = phylogeneticTreePanel
				.getLinageTypeAnnotationsProperties();
		if (linageTypeAnnotationsProperties.hasAnnotation()) {
			List<DrawPropOutterSidewardAnno4LinageType> internalNode2leafAnno4LinageTypes = linageTypeAnnotationsProperties
					.getInternalNode2leafAnno4LinageTypes();
			for (DrawPropOutterSidewardAnno4LinageType tt : internalNode2leafAnno4LinageTypes) {
				Shape shape = tt.getShape();
				Shape transformed = DrawUtil.affineTransform.createTransformedShape(shape);
				tt.setShape(transformed);

				Double textLocation = tt.getTextLocation();
				DrawUtil.affineTransform.transform(textLocation, dest);
				tt.setTextLocation(dest);
			}

			List<DrawPropOutterSidewardAnno4LinageType> outterSidewardAnnos = linageTypeAnnotationsProperties
					.getOutterSidewardAnnos();
			for (DrawPropOutterSidewardAnno4LinageType tt : outterSidewardAnnos) {
				Shape shape = tt.getShape();
				Shape transformed = DrawUtil.affineTransform.createTransformedShape(shape);
				tt.setShape(transformed);

				Double textLocation = tt.getTextLocation();
				DrawUtil.affineTransform.transform(textLocation, dest);
				tt.setTextLocation(dest);
				// System.out.println(textLocation+" "+dest);
			}

			List<DrawPropInternalNodeInsituAnno> internalNode2LeafInsituAnnos = linageTypeAnnotationsProperties
					.getInternalNode2LeafInsituAnnos();

			for (DrawPropInternalNodeInsituAnno tt : internalNode2LeafInsituAnnos) {
				Shape shape = tt.getDrawShape();
				Shape transformed = DrawUtil.affineTransform.createTransformedShape(shape);
				tt.setDrawShape(transformed);
			}
		}

	}

	private void iterateTurnDrawRatio2DrawLength(GraphicsNode node) {

		node.setXSelf(blankArea.getLeft() + node.getXSelf() * realWidth);
		node.setYSelf(blankArea.getTop() + node.getYSelf() * realHeight);

		NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);
		switch (nodeType) {
		case LEAF:
			break;
		default:

			// Internal nodes but not root!
			int count = node.getChildCount();
			for (int i = 0; i < count; i++) {
				GraphicsNode child = (GraphicsNode) node.getChildAt(i);
				child.setXParent(node.getXSelf());
				child.setYParent(node.getYSelf());
				iterateTurnDrawRatio2DrawLength(child);
			}
			break;
		}

	}

	@Override
	protected void configurateMainAnnotation(AnnotationsProperties annotationsProperties) {
		List<DrawPropInternalNode2LeafAnno> internalNode2LeafAnnos = annotationsProperties.getInternalNode2LeafAnnos();
		for (DrawPropInternalNode2LeafAnno tt : internalNode2LeafAnnos) {
			if (tt.shouldConfigurateAndPaint()) {
				GraphicsNode currentGraphicsNode = tt.getCurrentGraphicsNode();
				List<GraphicsNode> leaves = GraphicTreePropertyCalculator.getLeaves(currentGraphicsNode);
				GraphicsNode firstLeaf = leaves.get(0);
				GraphicsNode lastLeaf = leaves.get(leaves.size() - 1);

				double xSelf = currentGraphicsNode.getXSelf();
				double ySelf = currentGraphicsNode.getYSelf();

				GeneralPath triganlePath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 2);
				triganlePath.moveTo(xSelf, ySelf);
				triganlePath.lineTo(firstLeaf.getXSelf(), firstLeaf.getYSelf());
				triganlePath.lineTo(lastLeaf.getXSelf(), lastLeaf.getYSelf());
				triganlePath.closePath();

				tt.setDrawShape(triganlePath);
			}
		}
		// 这个注释要特殊处理
		// List<DrawPropLeafNameAnno> leafNameAnnos =
		// annotationsProperties.getLeafNameAnnos();
		annotationsProperties.configurateLeafNamesAnnotaion(false);

		List<DrawPropOutterSidewardAnno> outterSidewardAnnos = annotationsProperties.getOutterSidewardAnnos();
		for (DrawPropOutterSidewardAnno tt : outterSidewardAnnos) {
			if (tt.shouldConfigurateAndPaint()) {
				GraphicsNode currentGraphicsNode = tt.getCurrentGraphicsNode();
				List<GraphicsNode> leaves = GraphicTreePropertyCalculator.getLeaves(currentGraphicsNode);
				GraphicsNode firstLeaf = leaves.get(0);
				GraphicsNode lastLeaf = leaves.get(leaves.size() - 1);

				double topMostY = firstLeaf.getYSelf();
				double buttomMostY = lastLeaf.getYSelf();

				double xx = getRightMostXLocation(leaves) + 5;
				Line2D.Double rect = new Line2D.Double(xx, topMostY, xx, buttomMostY);
				tt.setTextLocationAndShape(xx, 0.5 * (topMostY + buttomMostY), rect);
			}
		}
		List<DrawPropInternalNodeInsituAnno> internalNode2LeafInsituAnnos = annotationsProperties
				.getInternalNode2LeafInsituAnnos();
		for (DrawPropInternalNodeInsituAnno tt : internalNode2LeafInsituAnnos) {
			if (tt.shouldConfigurateAndPaint()) {
				tt.configurate();
			}
		}

	}

	@Override
	protected void configurateLinageTypeAnnotation(AnnotationsProperties4LinageType annotationsProperties) {

		final double rightMostX = getRightMostXLocation(GraphicTreePropertyCalculator.getLeaves(rootNode)) + 5;

		linageTypeSidewardAnnotationCalculator = new Function<LinkedList<GraphicsNode>, OutterSidewardLocation>() {
			@Override
			public OutterSidewardLocation apply(LinkedList<GraphicsNode> t) {

				GraphicsNode first = t.getFirst();
				GraphicsNode last = t.getLast();

				double y1 = first.getYSelf();
				double y2 = last.getYSelf();
				if (y1 == y2) {
					y1 -= 1;
					y2 += 1;
				}
				Line2D.Double temp = new Line2D.Double(rightMostX, y1, rightMostX, y2);

				OutterSidewardLocation ret = new OutterSidewardLocation();

				ret.setShape(temp);
				ret.setTextX((float) rightMostX);
				ret.setTextY((float) (0.5 * (y1 + y2)));

				return ret;
			}
		};
		linageTypeNode2LeafAnnotationCalculator = new Function<LinkedList<GraphicsNode>, OutterSidewardLocation>() {
			@Override
			public OutterSidewardLocation apply(LinkedList<GraphicsNode> t) {

				GraphicsNode firstLeaf = t.getFirst();
				GraphicsNode lastLeaf = t.getLast();

				GraphicsNode mrca = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(firstLeaf, lastLeaf);
				double xSelf = mrca.getXSelf();
				double ySelf = mrca.getYSelf();

				GeneralPath triganlePath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 2);
				triganlePath.moveTo(xSelf, ySelf);
				triganlePath.lineTo(firstLeaf.getXSelf(), firstLeaf.getYSelf());
				triganlePath.lineTo(lastLeaf.getXSelf(), lastLeaf.getYSelf());
				triganlePath.closePath();

				OutterSidewardLocation ret = new OutterSidewardLocation();

				ret.setShape(triganlePath);
				ret.setTextX((float) rightMostX);
				ret.setTextY((float) (0.5 * (firstLeaf.getYSelf() + lastLeaf.getYSelf())));

				return ret;
			}
		};
		super.configurateLinageTypeAnnotation(annotationsProperties);
	}

	private void prepareStep2AllocateLocationRatio(GraphicsNode node) {

		NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);
		switch (nodeType) {
		case LEAF:
			break;
		default:
			// Internal nodes
			int count = node.getChildCount();
			for (int i = 0; i < count; i++) {
				prepareStep2AllocateLocationRatio((GraphicsNode) node.getChildAt(i));
			}

			GraphicsNode firstChild = (GraphicsNode) node.getFirstChild();
			GraphicsNode lastChild = (GraphicsNode) node.getLastChild();

			double firstChildLeafNumber = firstChild.getSize();
			double lastChildLeafNumber = lastChild.getSize();
			double maxLeafNumber = Math.max(firstChildLeafNumber, lastChildLeafNumber);

			double firstChildNewY = firstChild.getYSelf()
					+ 0.5 * reciprocalOfLeafNumbers * (maxLeafNumber - firstChildLeafNumber);
			double lastChildNewY = lastChild.getYSelf()
					- 0.5 * reciprocalOfLeafNumbers * (maxLeafNumber - lastChildLeafNumber);
			double unscaledY = 0.5 * (firstChildNewY + lastChildNewY);
			
			double unscaledX = obetainXPosition(node);
			node.setXSelf(unscaledX);
			node.setYSelf(unscaledY);
			node.setAngleIfNeeded(0.0);
			break;
		}
	}

	private double obetainXPosition(GraphicsNode n) {
		int numOfLeaves = n.getSize();
		double a = 1 - reciprocalOfLeafNumbers * (numOfLeaves - 1.0);
		return a;
	}

	/**
	 * Translate all node coordinates by the given offset.
	 */
	private void translateTree(GraphicsNode node, double offsetX, double offsetY) {
		node.setXSelf(node.getXSelf() + offsetX);
		node.setYSelf(node.getYSelf() + offsetY);
		node.setXParent(node.getXParent() + offsetX);
		node.setYParent(node.getYParent() + offsetY);

		int count = node.getChildCount();
		for (int i = 0; i < count; i++) {
			translateTree((GraphicsNode) node.getChildAt(i), offsetX, offsetY);
		}
	}

	/**
	 * Reset text angles to 0 for all nodes so text remains readable after 180° rotation.
	 * Use angle = 180 as a signal to draw text on the LEFT side.
	 */
	private void setTextAnglesForLeftSide(GraphicsNode node) {
		// Use 180 as a special signal: text should be on LEFT side, but not rotated
		node.setAngleIfNeeded(180.0);

		int count = node.getChildCount();
		for (int i = 0; i < count; i++) {
			setTextAnglesForLeftSide((GraphicsNode) node.getChildAt(i));
		}
	}

	@Override
	protected void specificTreeDrawingProcess(Graphics2D g2d) {
	}

	@Override
	protected void leafLineDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		drawEachLine(g2d, node);
		// 有点奇怪，为什么这一行注释掉反而正常显示。
		// ydl 确实不用，因为叶子名称的绘制在 lastRecursiveDraw中自动会绘制
//		drawEachLeafNameGraphicsIfNeeded(g2d, node, node.getXSelf(), node.getYSelf());
	}

	@Override
	protected void rootDrawingProcess(Graphics2D g2d, GraphicsNode node) {

	}

	@Override
	protected void innerNodeDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		drawEachLine(g2d, node);
	}

	private void drawEachLine(Graphics2D g2d, final GraphicsNode node) {
		commonDrawEachUnit(g2d, node, true);
	}

}
