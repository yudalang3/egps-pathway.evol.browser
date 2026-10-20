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
		
		boolean newick = !com.google.common.base.Strings.isNullOrEmpty(object.getInput_nwk_string())
				|| !com.google.common.base.Strings.isNullOrEmpty(object.getInput_nwk_path());
		int format = object.getNwkFormat();
		if (format < 0 || format > 9) format = module.evoltreio.NewickFormatConfig.DEFAULT_FORMAT;
		GraphicsNode theGraphicsTree = NodeUtils.convertBasicNode2graphicsNode(tree.get(), newick && (format == 0 || format == 2));
		module.evolview.phylotree.visualization.layout.BranchLengthDomain.maximum(theGraphicsTree, false);
		return Optional.of(theGraphicsTree);
	}

}
