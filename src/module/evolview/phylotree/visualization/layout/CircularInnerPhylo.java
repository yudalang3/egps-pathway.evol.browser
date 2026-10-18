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
		
		double avaliableLength = biggestCircleRadicus -circularLayoutPropertiy.getInnerCircleRadicus();
		canvas2logicRatio = avaliableLength / GraphicTreePropertyCalculator.getMaxLengthOfRoot2Leaf(rootNode).getLength();
		
		recursiveCalculateStartLength = (int) biggestCircleRadicus;
		
		
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
		return Math.max(super.getTipLabelReserve(), treeLayoutProperties.isShowRoot()
				? Math.max(0, treeLayoutProperties.getRootTipLength()) + 2 : 0);
	}

	@Override
	protected double minimumTreeRadius() {
		return treeLayoutProperties.getCircularLayoutPropertiy().getInnerCircleRadicus() + 50;
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
