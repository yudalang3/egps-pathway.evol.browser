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
 * Newick format basics (per Wikipedia/PHYLIP standard):
 * <pre>
 * - Trees are enclosed in parentheses and end with semicolon: (...)
 * - Tree → Subtree ";"
 * - Siblings are separated by commas: (A,B,C)
 * - Node labels follow closing parenthesis: (A,B)Label
 * - Branch lengths follow colon: Node:0.5
 * - Nested clades: ((A,B),(C,D))
 *
 * Forbidden characters in unquoted node names:
 *   - blanks (spaces, tabs, newlines)
 *   - parentheses ( )
 *   - square brackets [ ]
 *   - single quotes '
 *   - colons :
 *   - semicolons ;
 *   - commas ,
 *
 * Special rules:
 *   - Underscore (_) in unquoted strings is converted to blank
 *   - Strings can be quoted with single quotes: 'node name'
 *   - Single quotes in quoted strings: use two consecutive quotes ''
 * </pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Newick_format">Wikipedia: Newick format</a>
 * @see <a href="https://phylipweb.github.io/phylip/newicktree.html">PHYLIP Newick Standard</a>
 */
public class FormatAwareNewickParser {

	/**
	 * Characters forbidden in unquoted Newick node names (per Wikipedia/PHYLIP standard).
	 * These are: blanks, parentheses, square brackets, single quotes, colons, semicolons, commas.
	 */
	private static final String FORBIDDEN_CHARS_IN_UNQUOTED_NAME = "()[]':;, \t\n\r";

	/**
	 * Reference URL for the Newick format standard.
	 */
	private static final String NEWICK_STANDARD_REFERENCE = "https://en.wikipedia.org/wiki/Newick_format";

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
			String errorMsg = buildErrorMessage(
					"Empty or null Newick string",
					"A valid Newick tree string is required for parsing. The input was " +
							(newickString == null ? "null" : "empty"),
					"Provide a valid Newick tree string. Example: (A,B,(C,D)E)F;"
			);
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.EMPTY_INPUT, newickString, 0,
					"valid Newick string", errorMsg);
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
			String remainingChar = String.valueOf(newick.charAt(pos));
			String errorMsg = buildErrorMessage(
					"Unexpected character '" + remainingChar + "' at position " + pos + " after tree parsing completed",
					"The parser successfully parsed a complete tree but found additional characters. " +
							"This may indicate:\n" +
							"  - Multiple trees in a single string (not supported in standard Newick)\n" +
							"  - Extra characters after the terminating semicolon\n" +
							"  - Malformed tree structure",
					"Ensure your Newick string contains only one tree and ends with a single semicolon. " +
							"Remove any extra characters after the ';'"
			);
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.UNEXPECTED_CHARACTER,
					newick, pos, "end of input", errorMsg);
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
			String errorMsg = buildErrorMessage(
					"Missing terminating semicolon",
					"Newick trees must end with a semicolon (;). Grammar rule: Tree → Subtree \";\"",
					"Add a semicolon at the end of your tree string. Example: (A,B,C);"
			);
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.MISSING_SEMICOLON,
					newick, newick.length() - 1, ";",
					errorMsg);
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
					String errorMsg = buildErrorMessage(
							"Unmatched closing parenthesis at position " + i,
							"Found ')' without a matching '(' before it",
							"Check your tree structure and ensure all parentheses are properly matched"
					);
					throw new NewickSyntaxException(
							NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS,
							newick, i, "(", errorMsg);
				}
			}
		}
		if (depth != 0) {
			String errorMsg = buildErrorMessage(
					"Unmatched opening parenthesis",
					"Found " + depth + " unclosed '(' parenthesis(es)",
					"Add " + depth + " closing ')' parenthesis(es) to match the opening ones"
			);
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS,
					newick, newick.lastIndexOf('('), ")", errorMsg);
		}

		// Check for semicolons in the middle of the string (likely in node names)
		// This helps users identify invalid node names with special characters
		// Note: We need to skip semicolons inside quoted strings
		int problematicSemicolonPos = findUnquotedSemicolonBeforeEnd();
		if (problematicSemicolonPos >= 0) {
			String errorMsg = buildErrorMessage(
					"Semicolon (;) found in node name at position " + problematicSemicolonPos,
					"Semicolon is a reserved character that marks the end of a Newick tree. " +
							"It cannot appear in unquoted node names. " +
							"Forbidden characters in unquoted names: ( ) [ ] ' : ; , and whitespace",
					"Option 1: Remove or replace the semicolon in your node name\n" +
							"  Option 2: Quote the node name with single quotes: 'name;with;semicolon':1.0\n" +
							"  Example: 'Prokaryota|bacteria;archaea' instead of Prokaryota|bacteria;archaea"
			);
			throw new NewickSyntaxException(
					NewickSyntaxException.SyntaxErrorType.INVALID_NODE_NAME,
					newick, problematicSemicolonPos, "valid node name without forbidden characters", errorMsg);
		}

		// Check for other common issues with forbidden characters (quick scan)
		validateForbiddenCharactersInNames();
	}

	/**
	 * Find an unquoted semicolon that is not the terminating semicolon.
	 * This method properly skips semicolons inside single-quoted strings.
	 *
	 * @return position of the problematic semicolon, or -1 if none found
	 */
	private int findUnquotedSemicolonBeforeEnd() {
		boolean inQuotes = false;
		int lastUnquotedSemicolon = -1;

		for (int i = 0; i < newick.length(); i++) {
			char c = newick.charAt(i);

			// Track quoted strings (single quotes per Newick standard)
			if (c == '\'') {
				if (!inQuotes) {
					inQuotes = true;
				} else {
					// Check for escaped quote ('')
					if (i + 1 < newick.length() && newick.charAt(i + 1) == '\'') {
						i++; // Skip the escaped quote
						continue;
					}
					inQuotes = false;
				}
				continue;
			}

			// Track semicolons outside quotes
			if (c == ';' && !inQuotes) {
				if (i < newick.length() - 1) {
					// Found a semicolon that is not at the end
					return i;
				}
				lastUnquotedSemicolon = i;
			}
		}

		// All semicolons found are at the end (or inside quotes)
		return -1;
	}

	/**
	 * Scan for forbidden characters in node names that are not properly quoted.
	 * This provides early detection and helpful error messages.
	 */
	private void validateForbiddenCharactersInNames() throws NewickSyntaxException {
		boolean inQuotes = false;
		boolean inBranchLength = false;
		int bracketDepth = 0;

		for (int i = 0; i < newick.length(); i++) {
			char c = newick.charAt(i);

			// Track quoted strings
			if (c == '\'' && !inQuotes) {
				inQuotes = true;
				continue;
			}
			if (c == '\'' && inQuotes) {
				// Check for escaped quote ('')
				if (i + 1 < newick.length() && newick.charAt(i + 1) == '\'') {
					i++; // Skip the escaped quote
					continue;
				}
				inQuotes = false;
				continue;
			}

			// Skip content inside quotes
			if (inQuotes) {
				continue;
			}

			// Track square brackets (comments in some Newick dialects)
			if (c == '[') {
				bracketDepth++;
				continue;
			}
			if (c == ']') {
				bracketDepth--;
				continue;
			}
			if (bracketDepth > 0) {
				continue; // Inside a comment
			}

			// Track branch length context (after colon, numbers are expected)
			if (c == ':') {
				inBranchLength = true;
				continue;
			}
			if (inBranchLength) {
				if (c == ',' || c == ')' || c == '(' || c == ';') {
					inBranchLength = false;
				} else {
					continue; // Skip branch length characters
				}
			}

			// Skip structural characters
			if (c == '(' || c == ')' || c == ',' || c == ';') {
				continue;
			}

			// Check for unquoted square brackets in node names
			// Note: Square brackets are used for comments in NHX format, but within node names they must be quoted
			// We already handle bracket tracking above, so reaching here with [ or ] means it's unmatched
		}
	}

	/**
	 * Build a structured error message with cause and suggestion.
	 *
	 * @param problem     Brief description of the problem
	 * @param cause       Explanation of why this is an error
	 * @param suggestion  How to fix the error
	 * @return Formatted error message
	 */
	private static String buildErrorMessage(String problem, String cause, String suggestion) {
		StringBuilder sb = new StringBuilder();
		sb.append(problem);
		sb.append("\n\n[Cause]\n  ").append(cause);
		sb.append("\n\n[Suggestion]\n  ").append(suggestion);
		sb.append("\n\n[Reference]\n  ").append(NEWICK_STANDARD_REFERENCE);
		return sb.toString();
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
				String found = pos < newick.length() ? String.valueOf(newick.charAt(pos)) : "end of string";
				String errorMsg = buildErrorMessage(
						"Expected ')' at position " + pos + ", but found: " + found,
						"Internal nodes must be closed with ')'. The parser found an opening '(' but " +
								"could not find the matching closing ')'. This may indicate:\n" +
								"  - Missing ')' in the tree structure\n" +
								"  - Extra '(' that was not properly closed\n" +
								"  - Invalid characters in a node name that disrupted parsing",
						"Check the tree structure around position " + pos + " and ensure:\n" +
								"  - All '(' have matching ')'\n" +
								"  - Node names don't contain forbidden characters: ( ) [ ] ' : ; , and whitespace\n" +
								"  - If node names need special characters, quote them: 'node name'"
				);
				throw new NewickSyntaxException(
						NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS,
						newick, pos, ")", errorMsg);
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
