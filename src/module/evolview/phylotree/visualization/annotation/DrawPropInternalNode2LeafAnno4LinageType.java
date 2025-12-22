package module.evolview.phylotree.visualization.annotation;

import module.evolview.model.tree.GraphicsNode;

import java.awt.*;
import java.util.List;

public class DrawPropInternalNode2LeafAnno4LinageType extends DrawPropInternalNode2LeafAnno {

	private List<GraphicsNode> nodes;

	public DrawPropInternalNode2LeafAnno4LinageType(Color color, List<GraphicsNode> nodes) {
		super(color, nodes.get(0));
		this.nodes = nodes;
	}


	public List<GraphicsNode> getNodes() {
		return nodes;
	}

}
