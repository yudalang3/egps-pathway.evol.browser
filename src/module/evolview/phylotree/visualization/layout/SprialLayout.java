package module.evolview.phylotree.visualization.layout;

import evoltree.txtdisplay.TreeDrawUnit;
import graphic.engine.guicalculator.GuiCalculator;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.SprialLayoutProperty;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D.Double;

public abstract class SprialLayout extends BaseLayout {

	protected int leafLoop = 0;
	protected double increseDeg;
	protected double biggestCircleRadicus;
	protected int centerX;
	protected int centerY;
	protected int globalStartDegree;

	@Override
	protected void beforeCalculate(int width, int height) {
		super.beforeCalculate(width, height);

		leafLoop = 0;

		int workHeight = blankArea.getWorkHeight((int) currentHeight);
		int workWidth = blankArea.getWorkWidth((int) currentWidth);

		// Fix: Center should be in the middle of the working area, not the full canvas
		// This accounts for asymmetric margins (e.g., top=20, left=40, bottom=80, right=40)
		centerX = blankArea.getLeft() + workWidth / 2;
		centerY = blankArea.getTop() + workHeight / 2;

		SprialLayoutProperty circularLayoutPropertiy = treeLayoutProperties.getSprialLayoutPropertiy();
		globalStartDegree = circularLayoutPropertiy.getGlobalStartDegree();
		double totolDeg = circularLayoutPropertiy.getGlobalExtendingDegree() - globalStartDegree;
		increseDeg = totolDeg / GraphicTreePropertyCalculator.getLeafNumber(rootNode);

		// Note: Spiral layout uses biggestCircleRadicus differently than Circular.
		// The leaf label space reservation is handled in subclasses (Alpha/Beta variants)
		// via maxAlpha/maxBeta calculations.
		biggestCircleRadicus = 0.5 * Math.min(workWidth, workHeight);
	}

	public SprialLayout(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}

	protected double averageValueOfAngleFromChildren(GraphicsNode base) {
		double sum = 0.0;
		int size = base.getChildCount();
		for (int i = 0; i < size; i++) {
			sum += ((GraphicsNode) base.getChildAt(i)).getAngleIfNeeded();
		}
		return sum / size;
	}

	/**
	 * If you want to draw a global spiral line, the {@code startDeg} is 0;
	 * {@code extendDeg} is
	 * 
	 * <pre>
	 * SprialLayoutProperty circularLayoutPropertiy = treeLayoutProperties.getSprialLayoutPropertiy();
	 * double totolDeg = circularLayoutPropertiy.getGlobalExtendingDegree();
	 * double eachDeg = totolDeg / numOfPoints2draw;
	 * </pre>
	 * 
	 * @param startDeg
	 * @param extendDeg
	 * @param alpha
	 * @param beta
	 */
//	protected void produceSprial(double startDeg, double extendDeg, double alpha, double beta) {
//		
//		final int numOfPoints2draw = 1000;
//		GeneralPath generalPath = new GeneralPath(numOfPoints2draw);
//
//		double eachDeg = extendDeg / numOfPoints2draw;
//		for (int i = 0; i < numOfPoints2draw; i++) {
//			double currentDeg = startDeg + eachDeg * i;
//			Double point = GuiCalculator.calculateSpiralLocation(alpha, beta, currentDeg, centerX, centerY);
//			if (i == 0) {
//				generalPath.reset();
//				generalPath.moveTo(point.getX(), point.getY());
//			} else {
//				generalPath.lineTo(point.getX(), point.getY());
//			}
//		}
//		
//		Double point = GuiCalculator.calculateSpiralLocation(alpha, beta, startDeg + extendDeg, centerX, centerY);
//		generalPath.lineTo(point.getX(), point.getY());
//	}

	@Override
	protected TracingPaintingProperty configurateTracingShape(GraphicsNode node, Color color) {
		TracingPaintingProperty tracingPaintingProperty = new TracingPaintingProperty();

		GeneralPath localGeneralPath = new GeneralPath();

		double xSelf = node.getXSelf();
		double ySelf = node.getYSelf();

		TreeDrawUnit drawUnit = node.getDrawUnit();

		localGeneralPath.moveTo(xSelf, ySelf);

		drawUnit.setNodeSelected(true);

		GraphicsNode tempNode = node;
		while (tempNode.getParentCount() > 0) {
			double currentAngle = tempNode.getAngleIfNeeded();

			tempNode = (GraphicsNode) tempNode.getParent();

			double radicus = tempNode.getRadicusIfNeeded();
			double parentAngle = tempNode.getAngleIfNeeded();

			GeneralPath produceSprial = configGneralPath(radicus, Math.max(currentAngle, parentAngle),
					-Math.abs(currentAngle - parentAngle));

			localGeneralPath.append(produceSprial, true);
		}

		tracingPaintingProperty.path = localGeneralPath;
		tracingPaintingProperty.pointOfNode = new Double(node.getXSelf(), node.getYSelf());
		tracingPaintingProperty.fillColor = color;

		return tracingPaintingProperty;
	}

	protected abstract GeneralPath configGneralPath(double radicus, double lowAngle, double extent);

	protected GeneralPath produceSpiral(double startDeg, double extendDeg, double alpha, double beta) {
		GeneralPath generalPath4SprialPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1024);
		final int numOfPoints2draw = 1000;
		double eachDeg = extendDeg / numOfPoints2draw;
		for (int i = 0; i < numOfPoints2draw; i++) {
			double currentDeg = startDeg + eachDeg * i;
			Double point = GuiCalculator.calculateSpiralLocation(alpha, beta, currentDeg, centerX, centerY);
			if (i == 0) {
				generalPath4SprialPath.reset();
				generalPath4SprialPath.moveTo(point.getX(), point.getY());
			} else {
				generalPath4SprialPath.lineTo(point.getX(), point.getY());
			}
		}

		Double point = GuiCalculator.calculateSpiralLocation(alpha, beta, startDeg + extendDeg, centerX, centerY);
		generalPath4SprialPath.lineTo(point.getX(), point.getY());
		return generalPath4SprialPath;
	}

	/**
	 * If you want to draw a global spiral ring.
	 * 
	 * <pre>
	 * SprialLayoutProperty circularLayoutPropertiy = treeLayoutProperties.getSprialLayoutPropertiy();
	 * double totolDeg = circularLayoutPropertiy.getGlobalExtendingDegree();
	 * double eachDeg = totolDeg / numOfPoints2draw;
	 * </pre>
	 * 
	 * @param startDeg  起始的角度
	 * @param extendDeg 延伸的角度
	 * @param minAlpha  最小的螺旋线的alpha
	 * @param minBeta   最小的螺旋线的beta
	 * @param maxAlpha  最大的螺旋线alpha
	 * @param maxBeta   最大的螺旋线beta
	 * @return
	 */
	protected GeneralPath produceSpiralRing(double startDeg, double extendDeg, double minAlpha, double minBeta,
											double maxAlpha, double maxBeta) {

		GeneralPath generalPath4SprialRing = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1024);
		final int numOfPoints2draw = 1000;
		double eachDeg = extendDeg / numOfPoints2draw;
		for (int i = 0; i < numOfPoints2draw; i++) {
			double currentDeg = startDeg + eachDeg * i;
			Double point = GuiCalculator.calculateSpiralLocation(minAlpha, minBeta, currentDeg, centerX, centerY);
			if (i == 0) {
				generalPath4SprialRing.reset();
				generalPath4SprialRing.moveTo(point.getX(), point.getY());
			} else {
				generalPath4SprialRing.lineTo(point.getX(), point.getY());
			}
		}

		for (int i = numOfPoints2draw - 1; i > -1; i--) {
			double currentDeg = startDeg + eachDeg * i;
			Double point = GuiCalculator.calculateSpiralLocation(maxAlpha, maxBeta, currentDeg, centerX, centerY);
			generalPath4SprialRing.lineTo(point.getX(), point.getY());
		}

		generalPath4SprialRing.closePath();

		return generalPath4SprialRing;
	}
}
