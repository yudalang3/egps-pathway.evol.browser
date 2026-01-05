package module.evoltreio;

import com.google.common.base.Strings;
import evoltree.phylogeny.DefaultPhyNode;
import module.evoltreio.exception.NewickFormatException;
import module.evoltreio.exception.NewickParseException;
import module.evoltreio.parser.FormatAwareNewickParser;
import module.evolview.model.tree.NodeUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class TreeParser4Evoltree {

	private static final Logger log = LoggerFactory.getLogger(TreeParser4Evoltree.class);

	/**
	 * Parse a Newick string into a tree structure using format-aware parser.
	 *
	 * @param nwkStr          the Newick format string
	 * @param removeWhitespace whether to remove whitespace before parsing
	 * @param formatConfig    the format configuration
	 * @return the parsed tree root node
	 * @throws NewickParseException if parsing fails
	 */
	private DefaultPhyNode getTree(String nwkStr, boolean removeWhitespace, NewickFormatConfig formatConfig)
			throws NewickParseException {

		// Remove whitespace if requested
		String processedNwk = nwkStr;
		if (removeWhitespace) {
			processedNwk = nwkStr.replaceAll("\\s+", "");
		}

		// Use format-aware parser
		FormatAwareNewickParser parser = new FormatAwareNewickParser(formatConfig);
		log.debug("Parsing Newick with {}", parser.getStrategy().getDescription());

		return parser.parse(processedNwk);
	}

	/**
	 * Parse a tree from a table-like format file.
	 */
	private Optional<DefaultPhyNode> getTreeFromTreeTableFormat(String treePath) throws Exception {
		if (Strings.isNullOrEmpty(treePath)) {
			throw new IllegalArgumentException("Tree table file path cannot be null or empty");
		}
		File treeFile = new File(treePath);
		if (!treeFile.exists()) {
			throw new FileNotFoundException("Tree table file not found: " + treePath);
		}
		if (!treeFile.isFile()) {
			throw new IllegalArgumentException("Path is not a file: " + treePath);
		}
		List<String> input3 = FileUtils.readLines(treeFile, StandardCharsets.UTF_8);
		DefaultPhyNode rootNode = (DefaultPhyNode) NodeUtils.parseTableTree(input3);
		return Optional.of(rootNode);
	}

	/**
	 * Get a tree from the import info bean.
	 * Tries input methods in priority order: nwk.string > nwk.path > tableLike.path
	 *
	 * @param object the import configuration
	 * @return the parsed tree
	 * @throws Exception if parsing fails or no valid input is provided
	 */
	public Optional<DefaultPhyNode> getTree(EvolTreeImportInfoBean object) throws Exception {
		boolean removeWhitespace = object.isRemoveWhitespace();
		int formatNumber = object.getNwkFormat();

		// Get format configuration with validation
		NewickFormatConfig formatConfig;
		try {
			formatConfig = NewickFormatConfig.getFormat(formatNumber);
		} catch (NewickFormatException e) {
			log.warn("Invalid format {}, using default format {}: {}",
					formatNumber, NewickFormatConfig.DEFAULT_FORMAT, e.getMessage());
			formatConfig = NewickFormatConfig.getDefaultFormat();
		}

		Optional<DefaultPhyNode> optRoot;

		// Priority 1: Direct Newick string input
		if (!Strings.isNullOrEmpty(object.getInput_nwk_string())) {
			log.debug("Parsing tree from direct Newick string input");
			optRoot = Optional.of(getTree(object.getInput_nwk_string(), removeWhitespace, formatConfig));

		// Priority 2: Newick file path
		} else if (!Strings.isNullOrEmpty(object.getInput_nwk_path())) {
			String nwkPath = object.getInput_nwk_path();
			File file = new File(nwkPath);

			// Validate file exists
			if (!file.exists()) {
				throw new FileNotFoundException("Newick file not found: " + nwkPath);
			}
			if (!file.isFile()) {
				throw new IllegalArgumentException("Path is not a file: " + nwkPath);
			}

			log.debug("Parsing tree from Newick file: {}", nwkPath);
			List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);

			StringBuilder sBuilder = new StringBuilder();
			for (String string : lines) {
				sBuilder.append(string);
			}
			String nwkStr = sBuilder.toString();
			optRoot = Optional.of(getTree(nwkStr, removeWhitespace, formatConfig));

		// Priority 3: Table-like format file
		} else if (!Strings.isNullOrEmpty(object.getInput_tableLike_path())) {
			log.debug("Parsing tree from table-like format file: {}", object.getInput_tableLike_path());
			optRoot = getTreeFromTreeTableFormat(object.getInput_tableLike_path());

		} else {
			throw new IllegalArgumentException(
					"No tree input provided. Please specify one of: " +
					"input.nwk.string, input.nwk.path, or input.tableLike.path");
		}

		return optRoot;
	}

}
