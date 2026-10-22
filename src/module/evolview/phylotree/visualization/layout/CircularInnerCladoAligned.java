package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

public class CircularInnerCladoAligned extends CircularInnerPhylo{

    @Override
    protected boolean usesBranchLengths() { return false; }

	public CircularInnerCladoAligned(TreeLayoutProperties controller, GraphicsNode rootNode,TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode,phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		
		TreeOperationUtil.withEqualDisplayedBranchLengths(rootNode, () -> super.calculateForPainting(width, height));
		this.scaleBarProperty.setIfDrawScaleBar(false);
	}
	
	@Override
	protected double calculateRadicus(double distance, GraphicsNode node) {
		return node.getChildCount() == 0
				? treeLayoutProperties.getCircularLayoutPropertiy().resolveInnerRadius(biggestCircleRadicus)
				: super.calculateRadicus(distance, node);
	}

	protected boolean isLayoutSupportDrawScaleBar() {
		return false;
	}

}
