package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.CircularLayoutProperty;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;

public class CircularInnerPhylo extends CircularPhylo{

	public CircularInnerPhylo(TreeLayoutProperties controller, GraphicsNode rootNode,TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode,phylogeneticTreePanel);
	}

	@Override
	protected void calculateRatioAndChooseStartLength() {
		CircularLayoutProperty circularLayoutPropertiy = treeLayoutProperties.getCircularLayoutPropertiy();
		
		double avaliableLength = biggestCircleRadicus - circularLayoutPropertiy.resolveInnerRadius(biggestCircleRadicus);
		double distance = rootNode.getChildCount() == 0 || rootNode.getCollapse() ? 0
				: GraphicTreePropertyCalculator.getMaxLengthOfRoot2Leaf(rootNode).getLength();
        canvas2logicRatio = distance > 0 ? avaliableLength / distance : 0;
		
		recursiveCalculateStartLength = rootNode.getChildCount() == 0
				? circularLayoutPropertiy.resolveInnerRadius(biggestCircleRadicus) : biggestCircleRadicus;
		
		
		this.scaleBarProperty.setIfDrawScaleBar(false);
		
	}
	
	@Override
	protected double calculateRadicus(double cumulateRealBranchLengthExceptCurrent, GraphicsNode node) {
		double radicus = cumulateRealBranchLengthExceptCurrent - 
				node.getDisplayedBranchLength() * canvas2logicRatio;
		return radicus;
	}


	@Override
	protected double getTipLabelReserve() {
		return Math.max(getCircularDecorationReserve(), treeLayoutProperties.isShowRoot()
				? Math.max(0, treeLayoutProperties.getRootTipLength()) + 2 : 0);
	}

	@Override
	protected double minimumTreeRadius() {
		if (!treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel()) return 50;
		var metrics = phylogeneticTreePanel.getHostComponent().getFontMetrics(treeLayoutProperties.getGlobalFont());
		double inner = 0;
		for (GraphicsNode leaf : GraphicTreePropertyCalculator.getLeaves(rootNode)) {
			if (leaf.getChildCount() == 0 && leaf.getDrawUnit().isDrawName() && leaf.getName() != null)
				inner = Math.max(inner, metrics.stringWidth(leaf.getName()) + leaf.getDrawUnit().getCircleRadius() + 4 + metrics.getHeight());
		}
		return Math.max(50, inner / treeLayoutProperties.getCircularLayoutPropertiy().getInnerRadiusRatio());
	}

	@Override
	protected TipLabelGeometry getTipLabelGeometry(GraphicsNode node, java.awt.FontMetrics metrics) {
		return TipLabelGeometry.inwardAt(node.getAngleIfNeeded(), node.getDrawUnit().getCircleRadius(), node.getName(),
				metrics, treeLayoutProperties.getCircularLayoutPropertiy().resolveInnerRadius(biggestCircleRadicus));
	}

	@Override
	protected void configureRootBranch() {
		super.configureRootBranch();
		double radius = rootNode.getRadicusIfNeeded() + Math.max(0, treeLayoutProperties.getRootTipLength());
		java.awt.geom.Point2D.Double endpoint = graphic.engine.guicalculator.GuiCalculator.calculateCircularLocation(
				rootNode.getAngleIfNeeded(), radius, centerX, centerY);
		rootNode.setXParent(endpoint.x);
		rootNode.setYParent(endpoint.y);
	}

	@Override
	protected void rootDrawingProcess(java.awt.Graphics2D graphics, GraphicsNode node) {
		if (treeLayoutProperties.getRootTipLength() > 0) super.rootDrawingProcess(graphics, node);
	}
}
