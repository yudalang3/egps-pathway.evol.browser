package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

import java.awt.*;

public class RadialCladoLayout extends RadialPhyloLayout {
	public RadialCladoLayout(TreeLayoutProperties controller, GraphicsNode rootNode,TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode,phylogeneticTreePanel);
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

	@Override
	public void paintGraphics(Graphics2D g2d) {
		super.paintGraphics(g2d);
	}

}
