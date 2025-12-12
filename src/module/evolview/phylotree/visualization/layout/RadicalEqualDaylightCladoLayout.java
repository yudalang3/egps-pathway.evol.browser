package module.evolview.phylotree.visualization.layout;

import module.evolview.phylotree.visualization.layout.TreeLayoutHost;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

public class RadicalEqualDaylightCladoLayout extends RadicalEqualDaylightPhyloLayout {

	public RadicalEqualDaylightCladoLayout(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		// 让所有的枝长都一样！
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			if (node.getParentCount() == 0) {
				node.setDisplayedBranchLength(0);
			} else {
				node.setDisplayedBranchLength(0.1);
			}
		});

		super.calculateForPainting(width, height);
		// 恢复枝长
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			node.setDisplayedBranchLength(node.getRealBranchLength());
		});
	}
}
