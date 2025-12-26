package module.evoltreio.parser;

import evoltree.phylogeny.DefaultPhyNode;
import module.evoltreio.exception.NewickParseException;

/**
 * Strategy interface for parsing node label strings in different Newick formats.
 * Each format has different expectations for what the label before/after the colon represents.
 */
public interface NodeParseStrategy {

	/**
	 * Parse a node label string and populate the node's properties.
	 *
	 * @param labelStr the label string (e.g., "TaxonA:0.5" or "95:0.3")
	 * @param node     the node to populate
	 * @param isLeaf   whether this is a leaf node
	 * @param position position in original string for error reporting
	 * @param context  original newick string for error reporting
	 * @throws NewickParseException if parsing fails
	 */
	void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf, int position, String context)
			throws NewickParseException;

	/**
	 * Get description of this strategy for debugging/logging.
	 */
	String getDescription();
}
