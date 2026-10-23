package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.DrawUtil;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class RectPhyloAlign2Tip extends RectPhyloLayout {

	final BasicStroke basicStroke;

	public RectPhyloAlign2Tip(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);

		basicStroke = DrawUtil.getLineStroke(1);
	}

	@Override
	protected void specificTreeDrawingProcess(Graphics2D g2d) {
		drawButtomAxis(g2d);
	}

	@Override
	protected void leafLineDrawingProcess(Graphics2D g2d, GraphicsNode node) {
		drawEachLines(g2d, node);
		if (node.getChildCount() == 0 && !node.hideNode()) {
			Point2D.Double endpoint = getDisplayedNodePosition(node);
			if (endpoint.x > node.getXSelf()) {
				g2d.setStroke(basicStroke);
				g2d.setColor(node.getDrawUnit().getLineColor());
				lineDrawUtil.setLine(node.getXSelf(), node.getYSelf(), endpoint.x, endpoint.y);
				g2d.draw(lineDrawUtil);
			}
		}
	}

	@Override
	public Point2D.Double getDisplayedNodePosition(GraphicsNode node) {
		return node.getChildCount() == 0
				? new Point2D.Double(currentWidth - blankArea.getRight(), node.getYSelf())
				: super.getDisplayedNodePosition(node);
	}



	@Override
	protected double getRightMostXLocation4node2leafAnno(List<GraphicsNode> t) {
		return currentWidth - blankArea.getRight();
	}
}
