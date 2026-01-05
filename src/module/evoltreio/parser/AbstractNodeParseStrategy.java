package module.evoltreio.parser;

import evoltree.phylogeny.DefaultPhyNode;
import module.evoltreio.exception.NewickNodeFormatException;
import module.evoltreio.exception.NewickNumericException;
import module.evoltreio.exception.NewickParseException;

/**
 * Abstract base class for node parsing strategies.
 * Provides common utility methods for parsing names and numbers.
 * <p>
 * Note: DefaultPhyNode doesn't have a setSupport() method, so support values
 * are stored in the name field for internal nodes. Downstream code (like
 * GraphicsNode conversion) should check if the name is a number and treat
 * it as a support value accordingly.
 */
public abstract class AbstractNodeParseStrategy implements NodeParseStrategy {

	protected final int formatNumber;
	protected final boolean strict;

	protected AbstractNodeParseStrategy(int formatNumber, boolean strict) {
		this.formatNumber = formatNumber;
		this.strict = strict;
	}

	/**
	 * Parse a double value from string with proper error handling.
	 */
	protected double parseDouble(String value, String nodeName, NewickNumericException.NumericType type,
	                             int position, String context) throws NewickNumericException {
		if (value == null || value.isEmpty()) {
			if (strict) {
				throw new NewickNumericException(type, value, nodeName, context, position);
			}
			return 0.0; // Default value for non-strict mode
		}

		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			throw new NewickNumericException(type, value, nodeName, context, position, e);
		}
	}

	/**
	 * Split a label string by colon, handling edge cases.
	 * Returns array of [beforeColon, afterColon] or [label] if no colon.
	 */
	protected String[] splitByColon(String label) {
		if (label == null || label.isEmpty()) {
			return new String[]{"", ""};
		}

		int colonIndex = label.lastIndexOf(':');
		if (colonIndex == -1) {
			return new String[]{label, ""};
		}

		String before = label.substring(0, colonIndex);
		String after = colonIndex < label.length() - 1 ? label.substring(colonIndex + 1) : "";
		return new String[]{before, after};
	}

	/**
	 * Check if a required field is missing in strict mode.
	 */
	protected void checkRequired(String value, String nodeName,
	                             NewickNodeFormatException.NodeType nodeType,
	                             NewickNodeFormatException.MissingField field,
	                             int position, String context) throws NewickNodeFormatException {
		if (strict && (value == null || value.isEmpty())) {
			throw new NewickNodeFormatException(nodeType, nodeName, field, formatNumber, context, position);
		}
	}

	/**
	 * Set the node name if provided.
	 */
	protected void setName(DefaultPhyNode node, String name) {
		if (name != null && !name.isEmpty()) {
			node.setName(name);
		}
	}

	/**
	 * Set the branch length/distance if provided.
	 */
	protected void setDistance(DefaultPhyNode node, String distStr, String nodeName,
	                           int position, String context) throws NewickNumericException {
		if (distStr != null && !distStr.isEmpty()) {
			double dist = parseDouble(distStr, nodeName, NewickNumericException.NumericType.BRANCH_LENGTH,
					position, context);
			node.setLength(dist);
		}
	}

	/**
	 * Set the support value if provided (internal nodes only).
	 * <p>
	 * Note: Since DefaultPhyNode doesn't have setSupport(), we store the support
	 * value in the name field. This allows downstream code to recognize it as a
	 * support value.
	 * <p>
	 * <b>IMPORTANT: Support values should be in 0-100 range</b> (e.g., 90, 95, 100),
	 * not decimal format (0.0-1.0). For example, use "90" to represent 90% bootstrap
	 * support, not "0.9".
	 */
	protected void setSupport(DefaultPhyNode node, String supportStr, String nodeName,
	                          int position, String context) throws NewickNumericException {
		if (supportStr != null && !supportStr.isEmpty()) {
			// Validate that it's a valid number
			parseDouble(supportStr, nodeName, NewickNumericException.NumericType.SUPPORT_VALUE,
					position, context);
			// Store support value in name field for internal nodes
			// Downstream code should check if name is numeric and treat as support
			node.setName(supportStr);
		}
	}

	public int getFormatNumber() {
		return formatNumber;
	}

	public boolean isStrict() {
		return strict;
	}
}
