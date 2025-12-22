package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

public class CircularInnerCladoAligned extends CircularInnerPhylo{

	public CircularInnerCladoAligned(TreeLayoutProperties controller, GraphicsNode rootNode,TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode,phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		
		TreeOperationUtil.letAllNodesBeEqualLength(rootNode);
		
		super.calculateForPainting(width, height);
		// 恢复枝长
		int innerCircleRadicus = treeLayoutProperties.getCircularLayoutPropertiy().getInnerCircleRadicus();
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			node.setDisplayedBranchLength(node.getRealBranchLength());

			if (node.getChildCount() == 0) {
				node.setRadicusIfNeeded(innerCircleRadicus);
				assignLocation(node);
			}
		});
		this.scaleBarProperty.setIfDrawScaleBar(false);
	}
	
	protected boolean isLayoutSupportDrawScaleBar() {
		return true;
	}

}
