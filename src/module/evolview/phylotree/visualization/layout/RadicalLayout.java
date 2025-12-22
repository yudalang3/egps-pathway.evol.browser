package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;

import java.awt.*;
import java.awt.geom.Point2D;

public abstract class RadicalLayout extends CicularLayout {


	public RadicalLayout(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}

	protected void unRootedhorizontalLine(Graphics2D g2d, final GraphicsNode node) {
		commonDrawEachUnit(g2d, node, true);
	}

	protected double getRadicusForAnnotation(GraphicsNode[] leaves, double centerX, double centerY) {

		Point2D.Double point = new Point2D.Double(centerX, centerY);
		double biggestRadicus = getRadicus(leaves[0], point);

		int length = leaves.length;

		for (int i = 1; i < length; i++) {
			double radicus = getRadicus(leaves[i], point);
			if (radicus > biggestRadicus) {
				biggestRadicus = radicus;
			}
		}

		if (treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel()) {
			int maxLengthLeafNameWidthAccording2CurrentGlobalFont = getMaxLengthLeafNameWidthAccording2CurrentFont();
			biggestRadicus += maxLengthLeafNameWidthAccording2CurrentGlobalFont;
		} else {
			biggestRadicus += 15;

		}

		return biggestRadicus;
	}

	protected double getRadicus(GraphicsNode graphicsNode, Point2D point) {
		return point.distance(graphicsNode.getXSelf(), graphicsNode.getYSelf());
	}

}
