package module.evoltreio.parser;

import evoltree.phylogeny.DefaultPhyNode;
import module.evoltreio.NewickFormatConfig;
import module.evoltreio.exception.NewickParseException;
import module.evoltreio.exception.NewickSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Format-aware Newick parser that supports all 10 ETE3/ETE4 format types.
 * <p>
 * This parser handles the complete Newick string parsing, delegating node label
 * interpretation to format-specific strategies.
 * <p>
 * Newick format basics:
 * <pre>
 * - Trees are enclosed in parentheses and end with semicolon: (...);
 * - Siblings are separated by commas: (A,B,C)
 * - Node labels follow closing parenthesis: (A,B)Label
 * - Branch lengths follow colon: Node:0.5
 * - Nested clades: ((A,B),(C,D))
 * </pre>
 */
public class FormatAwareNewickParser {

	private static final Logger log = LoggerFactory.getLogger(FormatAwareNewickParser.class);

	private final NodeParseStrategy strategy;
	private final NewickFormatConfig config;

	// Parsing state
	private String newick;
	private int pos;
	private int nodeCounter;

	public FormatAwareNewickParser(int formatNumber) throws NewickParseException {
		this.config = NewickFormatConfig.getFormat(formatNumber);
		this.strategy = NodeParseStrategyFactory.createStrategy(formatNumber);
	}

	public FormatAwareNewickParser(NewickFormatConfig config) {
		this.config = config;
		this.strategy = NodeParseStrategyFactory.createStrategy(config);
	}

	/**
	 * Parse a Newick string into a tree.
	 *
	 * @param newickString the Newick format string
	 * @return the root node of the parsed tree
	 * @throws NewickParseException if parsing fails
	 */
	public DefaultPhyNode parse(String newickString) throws NewickParseException {
		if (newickString == null || newickString.isEmpty()) {
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.EMPTY_INPUT, newickString, 0);
		}

		// Preprocess: trim and remove trailing whitespace
		this.newick = newickString.trim();
		this.pos = 0;
		this.nodeCounter = 0;

		// Validate basic syntax
		validateSyntax();

		log.debug("Parsing Newick string with {}", strategy.getDescription());

		// Parse the tree
		DefaultPhyNode root = parseNode();

		// Should end with semicolon
		skipWhitespace();
		if (pos < newick.length() && newick.charAt(pos) == ';') {
			pos++;
		}

		// Verify we consumed everything (except trailing whitespace)
		skipWhitespace();
		if (pos < newick.length()) {
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.UNEXPECTED_CHARACTER,
					newick, pos, "end of input", String.valueOf(newick.charAt(pos)));
		}

		log.debug("Successfully parsed tree with {} nodes", nodeCounter);
		return root;
	}

	/**
	 * Validate basic Newick syntax before parsing.
	 */
	private void validateSyntax() throws NewickSyntaxException {
		// Check for terminating semicolon
		if (!newick.endsWith(";")) {
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.MISSING_SEMICOLON,
					newick, newick.length() - 1, ";",
					newick.substring(Math.max(0, newick.length() - 1)));
		}

		// Check for balanced parentheses
		int depth = 0;
		for (int i = 0; i < newick.length(); i++) {
			char c = newick.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
				if (depth < 0) {
					throw new NewickSyntaxException(
							NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS,
							newick, i, "(", ")");
				}
			}
		}
		if (depth != 0) {
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS,
					newick, newick.lastIndexOf('('), ")", "end of string");
		}
	}

	/**
	 * Parse a node (which may be a leaf or an internal node with children).
	 */
	private DefaultPhyNode parseNode() throws NewickParseException {
		skipWhitespace();

		DefaultPhyNode node = new DefaultPhyNode();
		nodeCounter++;

		if (pos < newick.length() && newick.charAt(pos) == '(') {
			// Internal node: parse children
			pos++; // consume '('
			parseChildren(node);

			// Expect ')'
			skipWhitespace();
			if (pos >= newick.length() || newick.charAt(pos) != ')') {
				throw new NewickSyntaxException(
						NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS,
						newick, pos, ")", pos < newick.length() ? String.valueOf(newick.charAt(pos)) : "EOF");
			}
			pos++; // consume ')'

			// Parse label for internal node
			String label = parseLabel();
			strategy.parseLabel(label, node, false, pos, newick);

		} else {
			// Leaf node: parse label
			String label = parseLabel();
			if (label.isEmpty()) {
				// Anonymous leaf - may be valid in some formats
				log.debug("Anonymous leaf node at position {}", pos);
			}
			strategy.parseLabel(label, node, true, pos, newick);
		}

		return node;
	}

	/**
	 * Parse children of an internal node.
	 */
	private void parseChildren(DefaultPhyNode parent) throws NewickParseException {
		// Parse first child
		DefaultPhyNode child = parseNode();
		parent.addChild(child);

		// Parse additional children separated by commas
		skipWhitespace();
		while (pos < newick.length() && newick.charAt(pos) == ',') {
			pos++; // consume ','
			skipWhitespace();

			// Note: In format 9 (pure topology) and other formats, empty nodes
			// like (,) are valid. Only check for trailing comma after a node.
			// The parseNode() method handles empty labels appropriately.

			child = parseNode();
			parent.addChild(child);
			skipWhitespace();
		}
	}

	/**
	 * Parse a node label (everything up to the next delimiter).
	 * Label may contain name and/or branch length: "name:0.5" or "name" or ":0.5"
	 */
	private String parseLabel() {
		skipWhitespace();

		int start = pos;
		StringBuilder label = new StringBuilder();
		boolean inQuotes = false;
		char quoteChar = 0;

		while (pos < newick.length()) {
			char c = newick.charAt(pos);

			// Handle quoted strings
			if (!inQuotes && (c == '\'' || c == '"')) {
				inQuotes = true;
				quoteChar = c;
				pos++;
				continue;
			}
			if (inQuotes) {
				if (c == quoteChar) {
					inQuotes = false;
					pos++;
					continue;
				}
				label.append(c);
				pos++;
				continue;
			}

			// Stop at delimiters
			if (c == '(' || c == ')' || c == ',' || c == ';') {
				break;
			}

			label.append(c);
			pos++;
		}

		return label.toString().trim();
	}

	/**
	 * Skip whitespace characters.
	 */
	private void skipWhitespace() {
		while (pos < newick.length() && Character.isWhitespace(newick.charAt(pos))) {
			pos++;
		}
	}

	/**
	 * Get the format config used by this parser.
	 */
	public NewickFormatConfig getConfig() {
		return config;
	}

	/**
	 * Get the strategy used by this parser.
	 */
	public NodeParseStrategy getStrategy() {
		return strategy;
	}
}
