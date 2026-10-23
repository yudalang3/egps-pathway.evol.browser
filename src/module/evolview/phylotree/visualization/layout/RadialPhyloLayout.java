package module.evolview.phylotree.visualization.layout;

import graphic.engine.guicalculator.GuiCalculator;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.annotation.*;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D.Double;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.Function;

public class RadialPhyloLayout extends RadicalLayout {

	/* the circleCenter of the circle */
	protected Point circleCenter = new Point(0, 0);
	private Calculation cachedCalculation;
	private UnrootedLayoutCalculator.Result cachedCoordinates;

	public record Calculation(GraphicsNode[] nodes, UnrootedLayoutCalculator.Input input,
			UnrootedLayoutCalculator.Result cached) { }

	protected UnrootedLayoutCalculator.Algorithm algorithm() {
		return UnrootedLayoutCalculator.Algorithm.EQUAL_ANGLE;
	}

	protected boolean isCladogram() { return false; }

    @Override
    protected boolean usesBranchLengths() { return !isCladogram(); }

	/** Capture on the EDT; numerical workers only read the resulting arrays. */
	public Calculation prepareCalculation() {
		record Pending(GraphicsNode node, int parent) { }
		List<GraphicsNode> nodes = new ArrayList<>();
		List<Integer> parents = new ArrayList<>();
		List<java.lang.Double> lengths = new ArrayList<>();
		ArrayDeque<Pending> pending = new ArrayDeque<>();
		pending.push(new Pending(rootNode, -1));
		while (!pending.isEmpty()) {
			Pending next = pending.pop();
			int index = nodes.size();
			nodes.add(next.node());
			parents.add(next.parent());
			lengths.add(next.parent() < 0 ? 0 : isCladogram() ? 1 : next.node().getDisplayedBranchLength());
			if (!next.node().getCollapse()) {
				for (int i = next.node().getChildCount() - 1; i >= 0; i--) {
					pending.push(new Pending((GraphicsNode) next.node().getChildAt(i), index));
				}
			}
		}
		var input = new UnrootedLayoutCalculator.Input(parents.stream().mapToInt(Integer::intValue).toArray(),
				lengths.stream().mapToDouble(java.lang.Double::doubleValue).toArray());
		GraphicsNode[] capturedNodes = nodes.toArray(GraphicsNode[]::new);
		boolean cached = cachedCalculation != null && input.sameTree(cachedCalculation.input())
				&& Arrays.equals(capturedNodes, cachedCalculation.nodes());
		return new Calculation(capturedNodes, input, cached ? cachedCoordinates : null);
	}

	public UnrootedLayoutCalculator.Result compute(Calculation calculation) {
		return calculation.cached() != null ? calculation.cached()
				: UnrootedLayoutCalculator.calculate(calculation.input(), algorithm());
	}

	/** Publish complete coordinates before rebuilding dependent annotations. */
	public void applyCalculation(Calculation calculation, UnrootedLayoutCalculator.Result result, int width, int height) {
		cachedCalculation = calculation;
		cachedCoordinates = result;
		beforeCalculate(width, height);
		double rotation = Math.toRadians(treeLayoutProperties.getRadicalLayoutRotationDeg());
		double cos = Math.cos(rotation), sin = Math.sin(rotation);
		double[] x = new double[result.x().length], y = new double[x.length];
		double minX = java.lang.Double.POSITIVE_INFINITY, minY = minX;
		double maxX = java.lang.Double.NEGATIVE_INFINITY, maxY = maxX;
		for (int i = 0; i < x.length; i++) {
			x[i] = cos * result.x()[i] - sin * result.y()[i];
			y[i] = sin * result.x()[i] + cos * result.y()[i];
			minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]);
			minY = Math.min(minY, y[i]); maxY = Math.max(maxY, y[i]);
		}
		double reserve = getTipLabelReserve();
		double rangeX = Math.max(1e-12, maxX - minX), rangeY = Math.max(1e-12, maxY - minY);
		double scale = Math.min(Math.max(1, blankArea.getWorkWidth(width) - 2 * reserve) / rangeX,
				Math.max(1, blankArea.getWorkHeight(height) - 2 * reserve) / rangeY);
		int[] parents = calculation.input().parents();
		double midX = (minX + maxX) / 2, midY = (minY + maxY) / 2;
		for (int i = 0; i < x.length; i++) {
			GraphicsNode node = calculation.nodes()[i];
			int parent = Math.max(0, parents[i]);
			node.setXSelf(circleCenter.x + (x[i] - midX) * scale);
			node.setYSelf(circleCenter.y + (y[i] - midY) * scale);
			node.setXParent(circleCenter.x + (x[parent] - midX) * scale);
			node.setYParent(circleCenter.y + (y[parent] - midY) * scale);
			node.setAngleIfNeeded(-Math.toDegrees(result.angles()[i] + rotation));
		}
		double longest = Arrays.stream(calculation.input().lengths()).max().orElse(0);
		canvas2logicRatio = longest > 0 ? scale / longest : 1;
		scaleBarProperty.setIfDrawScaleBar(!isCladogram() && longest > 0);
		if (!isCladogram() && longest > 0) configureBottomScaleBarDrawProperty(100 / canvas2logicRatio, rootNode, 100);
		afterCalculation();
	}

	public RadialPhyloLayout(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);

		linageTypeSidewardAnnotationCalculator = new Function<LinkedList<GraphicsNode>, OutterSidewardLocation>() {
			@Override
			public OutterSidewardLocation apply(LinkedList<GraphicsNode> t) {

				GraphicsNode firstLeaf = t.getFirst();
				GraphicsNode lastLeaf = t.getLast();
				GraphicsNode mrca = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(firstLeaf, lastLeaf);
				double centerX = mrca.getXSelf();
				double centerY = mrca.getYSelf();

				double lastXSelf = 0;
				double lastYSelf = 0;
				double firstXSelf = 0;
				double firstYSelf = 0;
				if (firstLeaf == lastLeaf) {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = lastXSelf + 5;
					firstYSelf = lastYSelf + 5;
				} else {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = firstLeaf.getXSelf();
					firstYSelf = firstLeaf.getYSelf();
				}

				double radicusForAnnotation = getRadicusForAnnotation(t.toArray(new GraphicsNode[1]), centerX, centerY);

				Arc2D.Double arc1 = new Arc2D.Double();
				arc1.setFrameFromCenter(centerX, centerY, centerX + radicusForAnnotation,
						centerY + radicusForAnnotation);

				arc1.setAngles(lastXSelf, lastYSelf, firstXSelf, firstYSelf);

				double startDeg = arc1.getAngleStart();
				double extentDeg = arc1.getAngleExtent();

				Double point = GuiCalculator.calculateCircularLocation(startDeg + 0.5 * extentDeg - 1,
						radicusForAnnotation + 20, centerX, centerY);

				OutterSidewardLocation ret = new OutterSidewardLocation();

				ret.setShape(arc1);
				ret.setTextX((float) point.x);
				ret.setTextY((float) point.y);
				return ret;
			}
		};

		linageTypeNode2LeafAnnotationCalculator = new Function<LinkedList<GraphicsNode>, OutterSidewardLocation>() {
			@Override
			public OutterSidewardLocation apply(LinkedList<GraphicsNode> t) {

				GraphicsNode firstLeaf = t.getFirst();
				GraphicsNode lastLeaf = t.getLast();
				GraphicsNode mrca = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(firstLeaf, lastLeaf);
				double centerX = mrca.getXSelf();
				double centerY = mrca.getYSelf();

				double lastXSelf = 0;
				double lastYSelf = 0;
				double firstXSelf = 0;
				double firstYSelf = 0;
				if (firstLeaf == lastLeaf) {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = lastXSelf + 5;
					firstYSelf = lastYSelf + 5;
				} else {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = firstLeaf.getXSelf();
					firstYSelf = firstLeaf.getYSelf();
				}

				double radicusForAnnotation = getRadicusForAnnotation(t.toArray(new GraphicsNode[1]), centerX, centerY);

				Arc2D.Double arc1 = new Arc2D.Double(Arc2D.PIE);
				arc1.setFrameFromCenter(centerX, centerY, centerX + radicusForAnnotation,
						centerY + radicusForAnnotation);

				arc1.setAngles(lastXSelf, lastYSelf, firstXSelf, firstYSelf);

				double startDeg = arc1.getAngleStart();
				double extentDeg = arc1.getAngleExtent();

				// arc1.setArcByCenter(centerX, centerY, radicusForAnnotation, startDeg - 5,
				// extentDeg+5, Arc2D.PIE);
				arc1.setAngleStart(startDeg - 5);
				arc1.setAngleExtent(extentDeg + 10);

				Double point = GuiCalculator.calculateCircularLocation(startDeg + 0.5 * extentDeg - 1,
						radicusForAnnotation + 20, centerX, centerY);

				OutterSidewardLocation ret = new OutterSidewardLocation();

				ret.setShape(arc1);
				ret.setTextX((float) point.x);
				ret.setTextY((float) point.y);
				return ret;
			}
		};
	}

	@Override
	protected void beforeCalculate(int width, int height) {
		super.beforeCalculate(width, height);

//		if (treeLayoutProperties.isShowOutgroup()) {
//			rightHorizontalBlank = rightHorizontalBlank + 80;
//		}
		circleCenter.setLocation(blankArea.getLeft() + blankArea.getWorkWidth(width) / 2.0,
				blankArea.getTop() + blankArea.getWorkHeight(height) / 2.0);
	}

	@Override
	public void calculateForPainting(int width, int height) {

		Calculation calculation = prepareCalculation();
		applyCalculation(calculation, compute(calculation), width, height);
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
                if (firstLeaf == lastLeaf) {
                    tt.setDrawShape(new java.awt.geom.Ellipse2D.Double(firstLeaf.getXSelf()-3, firstLeaf.getYSelf()-3, 6, 6));
                    continue;
                }

				GraphicsNode mrca = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(firstLeaf, lastLeaf);
				double centerX = mrca.getXSelf();
				double centerY = mrca.getYSelf();

				double lastXSelf = 0;
				double lastYSelf = 0;
				double firstXSelf = 0;
				double firstYSelf = 0;
				if (firstLeaf == lastLeaf) {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = lastXSelf + 5;
					firstYSelf = lastYSelf + 5;
				} else {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = firstLeaf.getXSelf();
					firstYSelf = firstLeaf.getYSelf();
				}

				double radicusForAnnotation = getRadicusForAnnotation(leaves.toArray(new GraphicsNode[1]), centerX,
						centerY);

				Arc2D.Double arc1 = new Arc2D.Double(Arc2D.PIE);
				arc1.setFrameFromCenter(centerX, centerY, centerX + radicusForAnnotation,
						centerY + radicusForAnnotation);

				arc1.setAngles(lastXSelf, lastYSelf, firstXSelf, firstYSelf);

				double startDeg = arc1.getAngleStart();
				double extentDeg = arc1.getAngleExtent();

				Double point = GuiCalculator.calculateCircularLocation(startDeg + 0.5 * extentDeg - 1,
						radicusForAnnotation + 20, centerX, centerY);
				tt.setDrawShape(arc1);
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
                if (firstLeaf == lastLeaf) {
                    double x = firstLeaf.getXSelf() + 10, y = firstLeaf.getYSelf();
                    tt.setTextLocationAndShape(x, y, new java.awt.geom.Line2D.Double(x,y-3,x,y+3));
                    continue;
                }

				GraphicsNode mrca = GraphicTreePropertyCalculator.getMostRecentCommonAnsester(firstLeaf, lastLeaf);
				double centerX = mrca.getXSelf();
				double centerY = mrca.getYSelf();

				double lastXSelf = 0;
				double lastYSelf = 0;
				double firstXSelf = 0;
				double firstYSelf = 0;
				if (firstLeaf == lastLeaf) {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = lastXSelf + 5;
					firstYSelf = lastYSelf + 5;
				} else {
					lastXSelf = lastLeaf.getXSelf();
					lastYSelf = lastLeaf.getYSelf();
					firstXSelf = firstLeaf.getXSelf();
					firstYSelf = firstLeaf.getYSelf();
				}

				double radicusForAnnotation = getRadicusForAnnotation(leaves.toArray(new GraphicsNode[1]), centerX,
						centerY);

				Arc2D.Double arc1 = new Arc2D.Double();
				arc1.setFrameFromCenter(centerX, centerY, centerX + radicusForAnnotation,
						centerY + radicusForAnnotation);

				arc1.setAngles(lastXSelf, lastYSelf, firstXSelf, firstYSelf);

				double startDeg = arc1.getAngleStart();
				double extentDeg = arc1.getAngleExtent();

				Double point = GuiCalculator.calculateCircularLocation(startDeg + 0.5 * extentDeg - 1,
						radicusForAnnotation + 20, centerX, centerY);

				tt.setTextLocationAndShape(point.getX(), point.getY(), arc1);
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
	protected void specificTreeDrawingProcess(Graphics2D g2d) {

	}

	@Override
	protected void leafLineDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		unRootedhorizontalLine(g2d, node);

	}

	@Override
	protected void rootDrawingProcess(Graphics2D g2d, GraphicsNode node) {

	}

	@Override
	protected void innerNodeDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		unRootedhorizontalLine(g2d, node);
	}

}
