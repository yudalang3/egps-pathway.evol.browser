package module.evoltreio.exception;

/**
 * Exception thrown when node data doesn't match the expected format.
 * For example, when using strict format but a required field is missing.
 */
public class NewickNodeFormatException extends NewickParseException {

	public enum NodeType {
		LEAF("Leaf node"),
		INTERNAL("Internal node"),
		ROOT("Root node");

		private final String description;

		NodeType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public enum MissingField {
		NAME("name"),
		DISTANCE("branch length/distance"),
		SUPPORT("support value");

		private final String description;

		MissingField(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	private final NodeType nodeType;
	private final String nodeName;
	private final MissingField missingField;
	private final int formatUsed;

	public NewickNodeFormatException(NodeType nodeType, String nodeName, MissingField missingField,
	                                 int formatUsed, String newickString, int position) {
		super(buildMessage(nodeType, nodeName, missingField, formatUsed), newickString, position);
		this.nodeType = nodeType;
		this.nodeName = nodeName;
		this.missingField = missingField;
		this.formatUsed = formatUsed;
	}

	private static String buildMessage(NodeType nodeType, String nodeName, MissingField missingField, int formatUsed) {
		StringBuilder sb = new StringBuilder();
		sb.append(nodeType.getDescription());
		if (nodeName != null && !nodeName.isEmpty()) {
			sb.append(" '").append(nodeName).append("'");
		}
		sb.append(" is missing required field: ").append(missingField.getDescription());
		sb.append(".\nFormat ").append(formatUsed);

		if (formatUsed == 2 || formatUsed == 3) {
			sb.append(" is a strict format - all fields are required.");
		}

		sb.append("\nHint: ");
		switch (missingField) {
			case NAME:
				sb.append("Add a node name before the colon, e.g., 'NodeA:0.5'");
				break;
			case DISTANCE:
				sb.append("Add a branch length after the colon, e.g., 'NodeA:0.5'");
				break;
			case SUPPORT:
				sb.append("Add a support value for internal nodes, e.g., '(A,B)95:0.3'");
				break;
		}

		return sb.toString();
	}

	public NodeType getNodeType() {
		return nodeType;
	}

	public String getNodeName() {
		return nodeName;
	}

	public MissingField getMissingField() {
		return missingField;
	}

	public int getFormatUsed() {
		return formatUsed;
	}
}
