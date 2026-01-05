package module.evoltreio;

import module.evoltreio.exception.NewickFormatException;

/**
 * Configuration for parsing Newick format strings.
 * Defines what fields are expected for leaf and internal nodes based on format number.
 * <p>
 * Reference: ETE3/ETE4 format specification
 * <pre>
 * Format | Leaf Node      | Internal Node     | Description
 * -------|----------------|-------------------|------------------------------------------
 *   0    | name:dist      | support:dist      | Flexible with support values (default)
 *   1    | name:dist      | name:dist         | Internal node names instead of support
 *   2    | name:dist      | support:dist      | Strict format 0 (all fields required)
 *   3    | name:dist      | name:dist         | Strict format 1 (all fields required)
 *   4    | name:dist      | - (none)          | Leaf only, no internal node data
 *   5    | name:dist      | :dist only        | Internal nodes have only distance
 *   6    | name:dist      | name only         | Internal nodes have only name
 *   7    | name only      | name only         | Names only, no distances
 *   8    | name only      | - (none)          | Leaf names only
 *   9    | - (none)       | - (none)          | Pure topology
 * </pre>
 */
public class NewickFormatConfig {

	public static final int MIN_FORMAT = 0;
	public static final int MAX_FORMAT = 9;
	public static final int DEFAULT_FORMAT = 0;

	/**
	 * Node field types that can appear in Newick format.
	 */
	public enum NodeField {
		/**
		 * Node name (taxon name for leaf, clade name for internal)
		 */
		NAME("name"),
		/**
		 * Branch length/distance
		 */
		DISTANCE("distance"),
		/**
		 * Bootstrap or other support value (internal nodes only)
		 */
		SUPPORT("support"),
		/**
		 * Field not present in this format
		 */
		NONE("none");

		private final String description;

		NodeField(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	private final int formatNumber;
	private final NodeField leafField1;      // Before colon for leaf
	private final NodeField leafField2;      // After colon for leaf
	private final NodeField internalField1;  // Before colon for internal
	private final NodeField internalField2;  // After colon for internal
	private final boolean strict;            // Whether missing fields cause errors

	/**
	 * Create a format configuration.
	 */
	private NewickFormatConfig(int formatNumber, NodeField leafField1, NodeField leafField2,
	                           NodeField internalField1, NodeField internalField2, boolean strict) {
		this.formatNumber = formatNumber;
		this.leafField1 = leafField1;
		this.leafField2 = leafField2;
		this.internalField1 = internalField1;
		this.internalField2 = internalField2;
		this.strict = strict;
	}

	/**
	 * Get the format configuration for a given format number.
	 *
	 * @param formatNumber the format number (0-9)
	 * @return the format configuration
	 * @throws NewickFormatException if the format number is invalid
	 */
	public static NewickFormatConfig getFormat(int formatNumber) throws NewickFormatException {
		return switch (formatNumber) {
			case 0 -> new NewickFormatConfig(0,
					NodeField.NAME, NodeField.DISTANCE,
					NodeField.SUPPORT, NodeField.DISTANCE,
					false);
			case 1 -> new NewickFormatConfig(1,
					NodeField.NAME, NodeField.DISTANCE,
					NodeField.NAME, NodeField.DISTANCE,
					false);
			case 2 -> new NewickFormatConfig(2,
					NodeField.NAME, NodeField.DISTANCE,
					NodeField.SUPPORT, NodeField.DISTANCE,
					true);  // Strict
			case 3 -> new NewickFormatConfig(3,
					NodeField.NAME, NodeField.DISTANCE,
					NodeField.NAME, NodeField.DISTANCE,
					true);  // Strict
			case 4 -> new NewickFormatConfig(4,
					NodeField.NAME, NodeField.DISTANCE,
					NodeField.NONE, NodeField.NONE,
					false);
			case 5 -> new NewickFormatConfig(5,
					NodeField.NAME, NodeField.DISTANCE,
					NodeField.NONE, NodeField.DISTANCE,
					false);
			case 6 -> new NewickFormatConfig(6,
					NodeField.NAME, NodeField.DISTANCE,
					NodeField.NAME, NodeField.NONE,
					false);
			case 7 -> new NewickFormatConfig(7,
					NodeField.NAME, NodeField.NONE,
					NodeField.NAME, NodeField.NONE,
					false);
			case 8 -> new NewickFormatConfig(8,
					NodeField.NAME, NodeField.NONE,
					NodeField.NONE, NodeField.NONE,
					false);
			case 9 -> new NewickFormatConfig(9,
					NodeField.NONE, NodeField.NONE,
					NodeField.NONE, NodeField.NONE,
					false);
			default -> throw new NewickFormatException(formatNumber, MIN_FORMAT, MAX_FORMAT);
		};
	}

	/**
	 * Get the default format configuration (format 0).
	 */
	public static NewickFormatConfig getDefaultFormat() {
		try {
			return getFormat(DEFAULT_FORMAT);
		} catch (NewickFormatException e) {
			// Should never happen
			throw new RuntimeException("Failed to get default format", e);
		}
	}

	/**
	 * Check if a format number is valid.
	 */
	public static boolean isValidFormat(int formatNumber) {
		return formatNumber >= MIN_FORMAT && formatNumber <= MAX_FORMAT;
	}

	// Getters

	public int getFormatNumber() {
		return formatNumber;
	}

	public NodeField getLeafField1() {
		return leafField1;
	}

	public NodeField getLeafField2() {
		return leafField2;
	}

	public NodeField getInternalField1() {
		return internalField1;
	}

	public NodeField getInternalField2() {
		return internalField2;
	}

	public boolean isStrict() {
		return strict;
	}

	// Convenience methods

	/**
	 * Check if leaf nodes should have a name.
	 */
	public boolean leafHasName() {
		return leafField1 == NodeField.NAME;
	}

	/**
	 * Check if leaf nodes should have a distance/branch length.
	 */
	public boolean leafHasDistance() {
		return leafField2 == NodeField.DISTANCE;
	}

	/**
	 * Check if internal nodes should have a name.
	 */
	public boolean internalHasName() {
		return internalField1 == NodeField.NAME;
	}

	/**
	 * Check if internal nodes should have a support value.
	 */
	public boolean internalHasSupport() {
		return internalField1 == NodeField.SUPPORT;
	}

	/**
	 * Check if internal nodes should have a distance/branch length.
	 */
	public boolean internalHasDistance() {
		return internalField2 == NodeField.DISTANCE;
	}

	/**
	 * Check if internal nodes should have any data before the colon.
	 */
	public boolean internalHasField1() {
		return internalField1 != NodeField.NONE;
	}

	/**
	 * Check if internal nodes should have any data after the colon.
	 */
	public boolean internalHasField2() {
		return internalField2 != NodeField.NONE;
	}

	/**
	 * Get a human-readable description of this format.
	 */
	public String getDescription() {
		return switch (formatNumber) {
			case 0 -> "Format 0: Flexible with support values (leaf: name:dist, internal: support:dist)";
			case 1 -> "Format 1: Internal node names (leaf: name:dist, internal: name:dist)";
			case 2 -> "Format 2: Strict format 0 - all fields required (leaf: name:dist, internal: support:dist)";
			case 3 -> "Format 3: Strict format 1 - all fields required (leaf: name:dist, internal: name:dist)";
			case 4 -> "Format 4: Leaf only (leaf: name:dist, internal: none)";
			case 5 -> "Format 5: Internal distance only (leaf: name:dist, internal: :dist)";
			case 6 -> "Format 6: Internal name only (leaf: name:dist, internal: name)";
			case 7 -> "Format 7: Names only (leaf: name, internal: name)";
			case 8 -> "Format 8: Leaf names only (leaf: name, internal: none)";
			case 9 -> "Format 9: Pure topology (no names or distances)";
			default -> "Unknown format: " + formatNumber;
		};
	}

	@Override
	public String toString() {
		return getDescription();
	}
}
