package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

public class CircularCladoAlign2Tip extends CircularPhylo {

    @Override
    protected boolean usesBranchLengths() { return false; }

	public CircularCladoAlign2Tip(TreeLayoutProperties controller, GraphicsNode rootNode,TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode,phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		TreeOperationUtil.withEqualDisplayedBranchLengths(rootNode, () -> super.calculateForPainting(width, height));
		
		this.scaleBarProperty.setIfDrawScaleBar(false);
	}

	@Override
	protected double calculateRadicus(double distance, GraphicsNode node) {
		return node.getChildCount() == 0 ? biggestCircleRadicus : super.calculateRadicus(distance, node);
	}

	@Override
	protected void calculateRatioAndChooseStartLength() {
		super.calculateRatioAndChooseStartLength();
		if (rootNode.getChildCount() == 0) recursiveCalculateStartLength = biggestCircleRadicus;
	}
	
	protected boolean isLayoutSupportDrawScaleBar() {
		return false;
	}


}
