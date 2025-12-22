package module.evolview.gfamily.work.gui.render;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

import java.awt.*;

public class NoColorRender implements IColorRenderer {

	@Override
	public void renderNodes(GraphicsNode rootNode) {
		TreeOperationUtil.recursiveIterateTreeIF(rootNode, node ->{
			node.getDrawUnit().setLineColor(Color.black);
		});
	}

}
