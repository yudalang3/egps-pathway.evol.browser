package module.evolview.moderntreeviewer.io;

import evoltree.phylogeny.DefaultPhyNode;
import graphic.engine.guicalculator.BlankArea;
import module.evoltreio.TreeParser4Evoltree;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.model.tree.NodeUtils;

import java.util.Objects;
import java.util.Optional;

public class TreeParser4MTV extends TreeParser4Evoltree {

	public Optional<GraphicsNode> parseTree(MTVImportInforBean object) throws Exception {

		BlankArea blank_space = object.getBlank_space();
		Objects.requireNonNull(blank_space, "Please provide the blinkArea instance.");
		Optional<DefaultPhyNode> tree = super.getTree(object);
		
		GraphicsNode theGraphicsTree = NodeUtils.convertBasicNode2graphicsNode(tree.get());
		return Optional.of(theGraphicsTree);
	}

}
