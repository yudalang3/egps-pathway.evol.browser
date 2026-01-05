package module.evoltreio.parser;

import evoltree.phylogeny.DefaultPhyNode;
import module.evoltreio.NewickFormatConfig;
import module.evoltreio.exception.NewickNodeFormatException;
import module.evoltreio.exception.NewickParseException;

/**
 * Factory for creating format-specific node parsing strategies.
 * <p>
 * Format reference (ETE3/ETE4 compatible):
 * <pre>
 * Format | Leaf         | Internal        | Description
 * -------|--------------|-----------------|------------------------------------------
 *   0    | name:dist    | support:dist    | Flexible with support values (default)
 *   1    | name:dist    | name:dist       | Internal node names instead of support
 *   2    | name:dist    | support:dist    | Strict format 0 (all fields required)
 *   3    | name:dist    | name:dist       | Strict format 1 (all fields required)
 *   4    | name:dist    | - (none)        | Leaf only, no internal node data
 *   5    | name:dist    | :dist only      | Internal nodes have only distance
 *   6    | name:dist    | name only       | Internal nodes have only name
 *   7    | name only    | name only       | Names only, no distances
 *   8    | name only    | - (none)        | Leaf names only
 *   9    | - (none)     | - (none)        | Pure topology
 * </pre>
 * <p>
 * <b>IMPORTANT: Bootstrap/Support Value Convention</b>
 * <p>
 * Support values (for formats 0, 2) should be in the <b>0-100 range</b>
 * (e.g., 90, 95, 100), not decimal format (0.0-1.0).
 * <p>
 * Example: Use {@code (A:0.1,B:0.2)90:0.3;} for 90% bootstrap support,
 * not {@code (A:0.1,B:0.2)0.9:0.3;}
 */
public class NodeParseStrategyFactory {

	/**
	 * Create the appropriate strategy for a format.
	 */
	public static NodeParseStrategy createStrategy(int formatNumber) {
		return switch (formatNumber) {
			case 0 -> new Format0Strategy();
			case 1 -> new Format1Strategy();
			case 2 -> new Format2Strategy();
			case 3 -> new Format3Strategy();
			case 4 -> new Format4Strategy();
			case 5 -> new Format5Strategy();
			case 6 -> new Format6Strategy();
			case 7 -> new Format7Strategy();
			case 8 -> new Format8Strategy();
			case 9 -> new Format9Strategy();
			default -> new Format0Strategy(); // Default to format 0
		};
	}

	/**
	 * Create strategy from config.
	 */
	public static NodeParseStrategy createStrategy(NewickFormatConfig config) {
		return createStrategy(config.getFormatNumber());
	}

	// ==================== Format Implementations ====================

	/**
	 * Format 0: Flexible with support values
	 * - Leaf: name:dist
	 * - Internal: support:dist
	 */
	static class Format0Strategy extends AbstractNodeParseStrategy {
		Format0Strategy() {
			super(0, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			String[] parts = splitByColon(labelStr);

			if (isLeaf) {
				// Leaf: name:dist
				setName(node, parts[0]);
				setDistance(node, parts[1], parts[0], position, context);
			} else {
				// Internal: support:dist
				setSupport(node, parts[0], null, position, context);
				setDistance(node, parts[1], null, position, context);
			}
		}

		@Override
		public String getDescription() {
			return "Format 0: Flexible (leaf=name:dist, internal=support:dist)";
		}
	}

	/**
	 * Format 1: Internal node names
	 * - Leaf: name:dist
	 * - Internal: name:dist
	 */
	static class Format1Strategy extends AbstractNodeParseStrategy {
		Format1Strategy() {
			super(1, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			String[] parts = splitByColon(labelStr);

			// Both leaf and internal: name:dist
			setName(node, parts[0]);
			setDistance(node, parts[1], parts[0], position, context);
		}

		@Override
		public String getDescription() {
			return "Format 1: Internal names (leaf=name:dist, internal=name:dist)";
		}
	}

	/**
	 * Format 2: Strict format 0 - all fields required
	 * - Leaf: name:dist (required)
	 * - Internal: support:dist (required)
	 */
	static class Format2Strategy extends AbstractNodeParseStrategy {
		Format2Strategy() {
			super(2, true);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			String[] parts = splitByColon(labelStr);

			if (isLeaf) {
				// Leaf: name:dist (both required)
				checkRequired(parts[0], null, NewickNodeFormatException.NodeType.LEAF,
						NewickNodeFormatException.MissingField.NAME, position, context);
				checkRequired(parts[1], parts[0], NewickNodeFormatException.NodeType.LEAF,
						NewickNodeFormatException.MissingField.DISTANCE, position, context);
				setName(node, parts[0]);
				setDistance(node, parts[1], parts[0], position, context);
			} else {
				// Internal: support:dist (both required)
				checkRequired(parts[0], null, NewickNodeFormatException.NodeType.INTERNAL,
						NewickNodeFormatException.MissingField.SUPPORT, position, context);
				checkRequired(parts[1], null, NewickNodeFormatException.NodeType.INTERNAL,
						NewickNodeFormatException.MissingField.DISTANCE, position, context);
				setSupport(node, parts[0], null, position, context);
				setDistance(node, parts[1], null, position, context);
			}
		}

		@Override
		public String getDescription() {
			return "Format 2: Strict format 0 (all fields required)";
		}
	}

	/**
	 * Format 3: Strict format 1 - all fields required
	 * - Leaf: name:dist (required)
	 * - Internal: name:dist (required)
	 */
	static class Format3Strategy extends AbstractNodeParseStrategy {
		Format3Strategy() {
			super(3, true);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			String[] parts = splitByColon(labelStr);

			NewickNodeFormatException.NodeType nodeType = isLeaf ?
					NewickNodeFormatException.NodeType.LEAF : NewickNodeFormatException.NodeType.INTERNAL;

			// Both: name:dist (both required)
			checkRequired(parts[0], null, nodeType,
					NewickNodeFormatException.MissingField.NAME, position, context);
			checkRequired(parts[1], parts[0], nodeType,
					NewickNodeFormatException.MissingField.DISTANCE, position, context);
			setName(node, parts[0]);
			setDistance(node, parts[1], parts[0], position, context);
		}

		@Override
		public String getDescription() {
			return "Format 3: Strict format 1 (all fields required)";
		}
	}

	/**
	 * Format 4: Leaf only
	 * - Leaf: name:dist
	 * - Internal: ignored
	 */
	static class Format4Strategy extends AbstractNodeParseStrategy {
		Format4Strategy() {
			super(4, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			if (isLeaf) {
				String[] parts = splitByColon(labelStr);
				setName(node, parts[0]);
				setDistance(node, parts[1], parts[0], position, context);
			}
			// Internal nodes: ignore any label
		}

		@Override
		public String getDescription() {
			return "Format 4: Leaf only (internal ignored)";
		}
	}

	/**
	 * Format 5: Internal distance only
	 * - Leaf: name:dist
	 * - Internal: :dist only (label before colon is ignored)
	 */
	static class Format5Strategy extends AbstractNodeParseStrategy {
		Format5Strategy() {
			super(5, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			String[] parts = splitByColon(labelStr);

			if (isLeaf) {
				// Leaf: name:dist
				setName(node, parts[0]);
				setDistance(node, parts[1], parts[0], position, context);
			} else {
				// Internal: :dist only (ignore name/support before colon)
				setDistance(node, parts[1], null, position, context);
			}
		}

		@Override
		public String getDescription() {
			return "Format 5: Internal distance only (leaf=name:dist, internal=:dist)";
		}
	}

	/**
	 * Format 6: Internal name only
	 * - Leaf: name:dist
	 * - Internal: name only (no distance)
	 */
	static class Format6Strategy extends AbstractNodeParseStrategy {
		Format6Strategy() {
			super(6, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			String[] parts = splitByColon(labelStr);

			if (isLeaf) {
				// Leaf: name:dist
				setName(node, parts[0]);
				setDistance(node, parts[1], parts[0], position, context);
			} else {
				// Internal: name only (use full label as name, ignore any colon)
				setName(node, labelStr.contains(":") ? parts[0] : labelStr);
			}
		}

		@Override
		public String getDescription() {
			return "Format 6: Internal name only (leaf=name:dist, internal=name)";
		}
	}

	/**
	 * Format 7: Names only
	 * - Leaf: name only
	 * - Internal: name only
	 */
	static class Format7Strategy extends AbstractNodeParseStrategy {
		Format7Strategy() {
			super(7, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			// Both: use full label as name (ignore any colon)
			if (labelStr != null && !labelStr.isEmpty()) {
				// If there's a colon, only take the part before it as name
				String[] parts = splitByColon(labelStr);
				setName(node, parts[0].isEmpty() ? labelStr : parts[0]);
			}
		}

		@Override
		public String getDescription() {
			return "Format 7: Names only (no distances)";
		}
	}

	/**
	 * Format 8: Leaf names only
	 * - Leaf: name only
	 * - Internal: ignored
	 */
	static class Format8Strategy extends AbstractNodeParseStrategy {
		Format8Strategy() {
			super(8, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			if (isLeaf) {
				// Leaf: name only
				if (labelStr != null && !labelStr.isEmpty()) {
					String[] parts = splitByColon(labelStr);
					setName(node, parts[0].isEmpty() ? labelStr : parts[0]);
				}
			}
			// Internal: ignore
		}

		@Override
		public String getDescription() {
			return "Format 8: Leaf names only (internal ignored)";
		}
	}

	/**
	 * Format 9: Pure topology
	 * - Leaf: ignored
	 * - Internal: ignored
	 */
	static class Format9Strategy extends AbstractNodeParseStrategy {
		Format9Strategy() {
			super(9, false);
		}

		@Override
		public void parseLabel(String labelStr, DefaultPhyNode node, boolean isLeaf,
		                       int position, String context) throws NewickParseException {
			// Pure topology: ignore all labels
			// Node is created with default values
		}

		@Override
		public String getDescription() {
			return "Format 9: Pure topology (all labels ignored)";
		}
	}
}
