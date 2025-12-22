package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

public class CircularCladoEqual extends CircularPhylo {
	public CircularCladoEqual(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		// 让所有的枝长都一样！
		TreeOperationUtil.letAllNodesBeEqualLength(rootNode);

		super.calculateForPainting(width, height);
		// 恢复枝长
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			node.setDisplayedBranchLength(node.getRealBranchLength());
		});
		//和TreeLayoutProperties 那里的设置不一样
		this.scaleBarProperty.setIfDrawScaleBar(false);
	}
	
	protected boolean isLayoutSupportDrawScaleBar() {
		return false;
	}
}