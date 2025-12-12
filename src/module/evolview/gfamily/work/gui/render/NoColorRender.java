package module.evolview.gfamily.work.gui.render;

import java.awt.Color;

import module.evolview.phylotree.visualization.util.TreeOperationUtil;
import module.evolview.model.tree.GraphicsNode;

public class NoColorRender implements IColorRenderer {

	@Override
	public void renderNodes(GraphicsNode rootNode) {
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node ->{
			node.getDrawUnit().setLineColor(Color.black);
		});
	}

}
