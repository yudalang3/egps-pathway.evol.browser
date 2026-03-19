package api.rpython;

import com.google.common.base.Objects;
import evoltree.phylogeny.DefaultPhyNode;
import evoltree.phylogeny.NWKInternalCoderDecoder;
import evoltree.phylogeny.NWKLeafCoderDecoder;
import evoltree.struct.io.PrimaryNodeTreeDecoder;
import evoltree.struct.util.EvolNodeUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;

/**
 * External API for extracting node information from phylogenetic trees in Newick format.
 */
public class EvolTreeManipulator {

	/**
	 * Extract node names from a phylogenetic tree file.
	 *
	 * @param treePath Path to the tree file in Newick format
	 * @param targetInternalNodeName Target internal node name to start search from, or {@code null} for the root
	 * @param includeLeafNodes Whether to include leaf nodes
	 * @param includeInternalNodes Whether to include internal nodes
	 * @return Array of node names matching the criteria
	 * @throws Exception If there is an error reading or parsing the tree file
	 */
	public String[] extractNodeNames(
			String treePath,
			String targetInternalNodeName,
			boolean includeLeafNodes,
			boolean includeInternalNodes
	) throws Exception {

		PrimaryNodeTreeDecoder<DefaultPhyNode> treeCoder = new PrimaryNodeTreeDecoder<>(
				new NWKLeafCoderDecoder<DefaultPhyNode>(), new NWKInternalCoderDecoder<DefaultPhyNode>());

		String nwkString = FileUtils.readFileToString(new File(treePath), StandardCharsets.UTF_8);
		DefaultPhyNode decode = treeCoder.decode(nwkString);

		DefaultPhyNode searchRoot;
		if (targetInternalNodeName == null) {
			searchRoot = decode;
		} else {
			Optional<DefaultPhyNode> searchRootNode = EvolNodeUtil.searchNodeWithReturn(decode, node -> {
				if (Objects.equal(targetInternalNodeName, node.getName())) {
					return true;
				}
				return false;
			});
			if (searchRootNode.isEmpty()) {
				throw new InputMismatchException("Sorry, the target internal node was not found.");
			}
			searchRoot = searchRootNode.get();
		}

		List<String> output = new ArrayList<>();
		EvolNodeUtil.recursiveIterateTreeIF(searchRoot, node -> {
			boolean isLeaf = node.getChildCount() == 0;
			if (isLeaf) {
				if (includeLeafNodes) {
					output.add(node.getName());
				}
			} else {
				if (includeInternalNodes) {
					output.add(node.getName());
				}
			}
		});

		return output.toArray(new String[0]);
	}

	/**
	 * Human-readable description of this API.
	 */
	public String describe() {
		return "External API for extracting node names from phylogenetic trees in Newick format.";
	}
}
