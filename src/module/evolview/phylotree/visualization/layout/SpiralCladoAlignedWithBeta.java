package module.evolview.phylotree.visualization.layout;

import module.evolview.phylotree.visualization.layout.TreeLayoutHost;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

public class SpiralCladoAlignedWithBeta extends SpiralPhyloWithBeta {

	public SpiralCladoAlignedWithBeta(TreeLayoutProperties controller, GraphicsNode rootNode,TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode,phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		// 让所有的枝长都一样！
		TreeOperationUtil.letAllNodesBeEqualLength(rootNode);

		super.calculateForPainting(width, height);
		// 恢复枝长
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			node.setDisplayedBranchLength(node.getRealBranchLength());

			if (node.getChildCount() == 0) {
				node.setRadicusIfNeeded(maxBeta);
				assignLocation(node);
			}
		});
		
		scaleBarProperty.setIfDrawScaleBar(false);
	}

}
