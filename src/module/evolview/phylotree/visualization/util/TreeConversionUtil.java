package module.evolview.phylotree.visualization.util;

import module.evolview.model.tree.GraphicsNode;
import module.remnant.treeoperator.NodeEGPSv1;

/**
 * Utilities for converting algorithmic tree nodes to visualization nodes.
 */
public final class TreeConversionUtil {

	private TreeConversionUtil() {}

	public static GraphicsNode node4eGPSToGraphicsNode(NodeEGPSv1 node) {
		GraphicsNode ret = new GraphicsNode();

		int childCount = node.getChildCount();
		if (childCount == 0) {
			ret.setName(node.getLeafName());
		} else {
			ret.setName(node.getName());
		}

		ret.setRealBranchLength(node.getBranch().getLength());

		if (node.getBs() < 1) {
			ret.setBootstrap((int) (node.getBs() * 100));
		} else {
			ret.setBootstrap((int) (node.getBs()));
		}

		for (int i = 0; i < childCount; i++) {
			NodeEGPSv1 child = node.getChildAt(i);
			GraphicsNode child4graphics = node4eGPSToGraphicsNode(child);
			ret.addChild(child4graphics);
		}

		return ret;
	}
}

