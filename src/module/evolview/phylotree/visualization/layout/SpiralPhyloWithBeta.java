package module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.GuiCalculator;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.annotation.*;
import module.evolview.phylotree.visualization.graphics.struct.NodeType;
import module.evolview.phylotree.visualization.graphics.struct.SprialLayoutProperty;
import module.evolview.phylotree.visualization.graphics.struct.TreeDecideUtil;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator.LongestRoot2leafBean;
import module.evolview.phylotree.visualization.util.DrawUtil;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D.Double;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * 注意：RadicusIfNeeded这里不是一个真正的半径，而是介于minBeta与maxBeta之间的值
 *
 */
public class SpiralPhyloWithBeta extends SprialLayout {

	protected double maxBeta;
	private double minBeta;
	/** 多出来的参数！ */
	private double betaFactor = 2.5;
	/** Global alpha */
	private double alpha;
	
	private GeneralPath generalPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1000);

	public SpiralPhyloWithBeta(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}

	@Override
	protected void beforeCalculate(int width, int height) {
		super.beforeCalculate(width, height);

		SprialLayoutProperty circularLayoutPropertiy = treeLayoutProperties.getSprialLayoutPropertiy();

		final int GAP_SIZE = circularLayoutPropertiy.getGapSize();
		betaFactor = circularLayoutPropertiy.getBetaFactor();
		alpha = treeLayoutProperties.getRootTipLength();
		maxBeta = (biggestCircleRadicus - alpha) / Math.toRadians(circularLayoutPropertiy.getGlobalExtendingDegree());
		minBeta = (2 * maxBeta * Math.PI + GAP_SIZE) / (4 * Math.PI);

		LongestRoot2leafBean maxLengthOfRoot2LeafNoOutgroup = GraphicTreePropertyCalculator
				.getMaxLengthOfRoot2Leaf(rootNode);

		double maxLengthOfRoot2Leaf = maxLengthOfRoot2LeafNoOutgroup.getLength();
		canvas2logicRatio = (maxBeta - minBeta) / maxLengthOfRoot2Leaf;

		configurateButtomScaleBarDrawProperty(maxLengthOfRoot2LeafNoOutgroup.getLength(),
				maxLengthOfRoot2LeafNoOutgroup.getLeaf());
	}

	@Override
	public void calculateForPainting(int width, int height) {
		beforeCalculate(width, height);

		GraphicsNode node = rootNode;
		recursiveCalculate(node, 0);
		// set root properties!
		node.setXParent(centerX);
		node.setYParent(centerY);
		Double tt = GuiCalculator.calculateSpiralLocation(alpha, betaFactor * node.getRadicusIfNeeded(),
				node.getAngleIfNeeded(), centerX, centerY);
		node.setXSelf(tt.x);
		node.setYSelf(tt.y);

		scaleBarProperty.setIfDrawScaleBar(true);
		afterCalculation();
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

				double startDeg = firstLeaf.getAngleIfNeeded();
				double lastDeg = lastLeaf.getAngleIfNeeded();
				GeneralPath clone = produceSpiralRing(startDeg, lastDeg - startDeg, alpha,
						betaFactor * currentGraphicsNode.getRadicusIfNeeded(), alpha, maxBeta);

				tt.setDrawShape(clone);
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

				double startDeg = firstLeaf.getAngleIfNeeded();
				double lastDeg = lastLeaf.getAngleIfNeeded();

				GeneralPath clone = produceSpiral(startDeg, lastDeg - startDeg, alpha + 2, maxBeta);

				Double point = GuiCalculator.calculateSpiralLocation(alpha, maxBeta, 0.5 * (startDeg + lastDeg),
						centerX, centerY);
				tt.setTextLocationAndShape(point.getX(), point.getY(), clone);
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
		linageTypeSidewardAnnotationCalculator = new Function<LinkedList<GraphicsNode>, OutterSidewardLocation>() {
			@Override
			public OutterSidewardLocation apply(LinkedList<GraphicsNode> t) {

				GraphicsNode firstLeaf = t.getFirst();
				GraphicsNode lastLeaf = t.getLast();

				double startDeg = firstLeaf.getAngleIfNeeded();
				double endDeg = lastLeaf.getAngleIfNeeded();
				double extentDeg = endDeg - startDeg;

				if (extentDeg < 1) {
					extentDeg = 1;
				}

				GeneralPath clone = produceSpiral(startDeg, extentDeg, alpha + 2, maxBeta);

				Double point = GuiCalculator.calculateSpiralLocation(alpha, maxBeta, 0.5 * (startDeg + endDeg), centerX,
						centerY);

				OutterSidewardLocation ret = new OutterSidewardLocation();
				ret.setShape(clone);
				ret.setTextX((float) point.getX());
				ret.setTextY((float) point.getY());
				return ret;
			}
		};
		linageTypeNode2LeafAnnotationCalculator = new Function<LinkedList<GraphicsNode>, OutterSidewardLocation>() {
			@Override
			public OutterSidewardLocation apply(LinkedList<GraphicsNode> t) {

				GraphicsNode firstLeaf = t.getFirst();
				GraphicsNode lastLeaf = t.getLast();
				GraphicsNode mrca = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(firstLeaf, lastLeaf);

				double startDeg = firstLeaf.getAngleIfNeeded();
				double lastDeg = lastLeaf.getAngleIfNeeded();
				double extendDeg = lastDeg - startDeg;
				if (extendDeg < 1) {
					extendDeg = 1;
					startDeg -= 0.5;
					lastDeg = startDeg + 1;
				}

				GeneralPath clone = produceSpiralRing(startDeg, extendDeg, alpha,
						betaFactor * mrca.getRadicusIfNeeded(), alpha, maxBeta);

				Double point = GuiCalculator.calculateSpiralLocation(alpha, maxBeta, 0.5 * (startDeg + lastDeg),
						centerX, centerY);

				OutterSidewardLocation ret = new OutterSidewardLocation();
				ret.setShape(clone);
				ret.setTextX((float) point.getX());
				ret.setTextY((float) point.getY());
				return ret;
			}
		};
		super.configurateLinageTypeAnnotation(annotationsProperties);
	}

	private void recursiveCalculate(GraphicsNode node, double cumulateBranchLengthExceptCurrent) {

		double branchLen = cumulateBranchLengthExceptCurrent + node.getDisplayedBranchLength();
		double realBranchLenAlsoBeta = minBeta + branchLen * canvas2logicRatio;

		NodeType nodeType = TreeDecideUtil.decideNodeType(node, rootNode);
		switch (nodeType) {
		case LEAF:
			node.setAngleIfNeeded(leafLoop * increseDeg);
			node.setRadicusIfNeeded(realBranchLenAlsoBeta);
			assignLocation(node);
			leafLoop++;
			break;
		default:

			// 这一行要比递归调用早！
			node.setRadicusIfNeeded(realBranchLenAlsoBeta);
			// internal first recursive
			for (int i = 0; i < node.getChildCount(); i++) {
				recursiveCalculate((GraphicsNode) node.getChildAt(i), branchLen);
			}
			// 这一行要比递归调用晚！
			double angle = averageValueOfAngleFromChildren(node);
			node.setAngleIfNeeded(angle);
			if (nodeType != NodeType.ROOT) {
				assignLocation(node);
			}

			break;
		}

	}

	/**
	 * 对于当前的node来说: angle和radicus是这个循环赋值的！
	 */
	protected void assignLocation(GraphicsNode node) {

		double currentNodeAngle = node.getAngleIfNeeded();
		Double tt = GuiCalculator.calculateSpiralLocation(alpha, betaFactor * node.getRadicusIfNeeded(),
				currentNodeAngle, centerX, centerY);
		node.setXSelf(tt.x);
		node.setYSelf(tt.y);

		// System.out.println("Current node angle: "+currentNodeAngle+"\tNode radicus:
		// "+node.getRadicusIfNeeded()+" Point: "+tt.x+" "+tt.y);
		GraphicsNode parent = (GraphicsNode) node.getParent();
		tt = GuiCalculator.calculateSpiralLocation(alpha, betaFactor * parent.getRadicusIfNeeded(), currentNodeAngle,
				centerX, centerY);

		node.setXParent(tt.x);
		node.setYParent(tt.y);

		// System.out.println("Parent radicus: "+parent.getRadicusIfNeeded()+" Point:
		// "+tt.x+" "+tt.y);
		// System.out.println(parent.getRadicusIfNeeded()+"
		// "+node.getRadicusIfNeeded()+"
		// ------------------------------------------------");
	}

	@Override
	protected void specificTreeDrawingProcess(Graphics2D g2d) {
		GeneralPath generalPath = null;
		double rootTipLength = treeLayoutProperties.getRootTipLength();
		SprialLayoutProperty circularLayoutPropertiy = treeLayoutProperties.getSprialLayoutPropertiy();
		double totolDeg = circularLayoutPropertiy.getGlobalExtendingDegree();

		/**
		 * Draw the whole region of the spiral, for debug
		 */
		if(false){
			generalPath = produceSpiralRing(0, totolDeg, rootTipLength, minBeta, rootTipLength, maxBeta);
			g2d.setColor(Color.lightGray);
			g2d.draw(generalPath);
			g2d.setColor(new Color(12, 12, 5, 12));
			g2d.fill(generalPath);
		}

		/**
		 * Draw the time line, for debug
		 */
		if(false){
			generalPath = produceSpiral(0, totolDeg, rootTipLength, minBeta * betaFactor);
			g2d.setColor(Color.red);
			g2d.draw(generalPath);

			generalPath = produceSpiral(0, totolDeg, rootTipLength, 21);
			g2d.setColor(Color.magenta);
			g2d.draw(generalPath);

			generalPath = produceSpiral(0, totolDeg, rootTipLength, 22);
			g2d.setColor(Color.green);
			g2d.draw(generalPath);

			generalPath = produceSpiral(0, totolDeg, rootTipLength, 25);
			g2d.setColor(Color.black);
			g2d.draw(generalPath);

			generalPath = produceSpiral(0, totolDeg, rootTipLength, 33);
			g2d.setColor(Color.cyan);
			g2d.draw(generalPath);

			generalPath = produceSpiral(0, totolDeg, rootTipLength, maxBeta);
			g2d.setColor(Color.orange);
			g2d.draw(generalPath);
		}

		drawBottomAxis(g2d);
	}

	/**
	 * 同 RectPhyloLayout，因为它绘制在底部！
	 * 
	 * @param g2d
	 */
	protected void drawBottomAxis(Graphics2D g2d) {
		if (!is4AnnotationDialog && treeLayoutProperties.isShowAxisBar()) {
			g2d.setFont(treeLayoutProperties.getAxisFont());

			double totalAvailableBeta = maxBeta - minBeta;
//			double leftBlankLength = minBeta;

			g2d.setColor(Color.decode("#E9E4E6"));
			g2d.setStroke(new BasicStroke(1f));

			List<java.lang.Double> displayeDoubles = buttomScaleBarDrawProperty.getDisplayeDoubles();
			List<String> displayedStrings = buttomScaleBarDrawProperty.getDisplayedStrings();
			int count = displayedStrings.size();

			SprialLayoutProperty circularLayoutPropertiy = treeLayoutProperties.getSprialLayoutPropertiy();
			double totolDeg = circularLayoutPropertiy.getGlobalExtendingDegree();
			double rootTipLength = treeLayoutProperties.getRootTipLength();

			java.lang.Double first = displayeDoubles.getFirst();
			java.lang.Double last = displayeDoubles.getLast();
			double range = last - first;
			for (int i = 0; i < count; i++){
				java.lang.Double double1 = displayeDoubles.get(i);
				double currentBeta = (double1 - minBeta) / range * totalAvailableBeta + minBeta;
				GeneralPath generalPath = produceSpiral(globalStartDegree, totolDeg, rootTipLength, currentBeta);
				g2d.draw(generalPath);

				String str = displayedStrings.get(i);

								// Position text at the end of the limited spiral arc
				Double tt = GuiCalculator.calculateSpiralLocation(alpha, currentBeta,
						globalStartDegree + totolDeg, centerX, centerY);

				float xStr = (float) (tt.x + 5);
				float yStr = (float) (tt.y);
				g2d.drawString(str, xStr, yStr);

			}

		}
	}
	
	@Override
	protected void leafLineDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		drawEachLine(g2d, node);
		
	}
	
	@Override
	protected void rootDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		DrawUtil.drawRootTip(g2d, node, treeLayoutProperties);
	}
	
	@Override
	protected void innerNodeDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		drawEachLine(g2d, node);
	}


	private void drawEachLine(Graphics2D g2d, GraphicsNode node) {

		commonDrawEachUnit(g2d, node, true);

		GraphicsNode parent = (GraphicsNode) node.getParent();

		double radicus = parent.getRadicusIfNeeded();
		double currentAngle = node.getAngleIfNeeded();
		double parentAngle = parent.getAngleIfNeeded();

		realConfigure(radicus, Math.min(currentAngle, parentAngle), Math.abs(currentAngle - parentAngle),generalPath);
		g2d.draw(generalPath);

	}

	@Override
	protected GeneralPath configGneralPath(double radicus, double lowAngle, double extent) {
		GeneralPath generalPath2 = new GeneralPath();
		realConfigure(radicus,lowAngle,extent,generalPath2);
		return generalPath2;
	}
	
	private void realConfigure(double radicus, double lowAngle, double extent, GeneralPath generalPath) {
		final int numOfDivider = 100;
		double eachDeg = extent / numOfDivider;

		for (int i = 0; i < numOfDivider; i++) {

			Double pos = GuiCalculator.calculateSpiralLocation(alpha, betaFactor * radicus,
					lowAngle + eachDeg * i, centerX, centerY);
			if (i == 0) {
				generalPath.reset();
				generalPath.moveTo(pos.getX(), pos.getY());
			} else {
				generalPath.lineTo(pos.getX(), pos.getY());
			}
		}

		Double pos = GuiCalculator.calculateSpiralLocation(alpha, betaFactor * radicus, lowAngle + extent,
				centerX, centerY);
		generalPath.lineTo(pos.getX(), pos.getY());
	}

}
