package module.evolview.phylotree.visualization.util;

import module.evolview.model.tree.GraphicsNode;
import module.remnant.treeoperator.NodeEGPSv1;

/**
 * Utilities for converting algorithmic tree nodes to visualization nodes.
 */
public final class TreeConversionUtil {

	private TreeConversionUtil() {}

	public static GraphicsNode node4eGPSToGraphicsNode(NodeEGPSv1 node) {
		return convert(node, null);
	}

	public static GraphicsNode node4eGPSToGraphicsNode(NodeEGPSv1 node, boolean bootstrapCalculated) {
		return convert(node, bootstrapCalculated);
	}

	private static GraphicsNode convert(NodeEGPSv1 node, Boolean bootstrapCalculated) {
		GraphicsNode ret = new GraphicsNode();

		int childCount = node.getChildCount();
		if (childCount == 0) {
			ret.setName(node.getLeafName());
		} else {
			ret.setName(node.getName());
		}

		ret.setRealBranchLength(node.getBranch().getLength());

		if (childCount > 0 && node.getParentCount() > 0) {
			if (Boolean.TRUE.equals(bootstrapCalculated)) ret.setBootstrapValue(node.getBs() * 100);
			else if (bootstrapCalculated == null && node.getBs() != 0)
				ret.setBootstrapValue(node.getBs() < 1 ? node.getBs() * 100 : node.getBs());
		}

		for (int i = 0; i < childCount; i++) {
			NodeEGPSv1 child = node.getChildAt(i);
			GraphicsNode child4graphics = convert(child, bootstrapCalculated);
			ret.addChild(child4graphics);
		}

		return ret;
	}
}

