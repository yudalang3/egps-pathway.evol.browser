package module.evolview.phylotree.visualization.layout;

import module.evolview.phylotree.visualization.layout.TreeLayoutHost;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

public class CircularCladoAlign2Tip extends CircularPhylo {

	public CircularCladoAlign2Tip(TreeLayoutProperties controller, GraphicsNode rootNode,TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode,phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		// 让所有的枝长都一样！
		super.calculateForPainting(width, height);
		// 恢复枝长
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			node.setDisplayedBranchLength(node.getRealBranchLength());

			if (node.getChildCount() == 0) {
				node.setRadicusIfNeeded(biggestCircleRadicus);
				assignLocation(node);
			}
		});
		
		this.scaleBarProperty.setIfDrawScaleBar(false);
	}
	
	protected boolean isLayoutSupportDrawScaleBar() {
		return false;
	}


}