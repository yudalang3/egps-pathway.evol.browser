package module.evoltreio.exception;

/**
 * Exception thrown when a numeric value (branch length or support) cannot be parsed.
 */
public class NewickNumericException extends NewickParseException {

	public enum NumericType {
		BRANCH_LENGTH("branch length"),
		SUPPORT_VALUE("support value");

		private final String description;

		NumericType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	private final NumericType numericType;
	private final String invalidValue;
	private final String nodeName;

	public NewickNumericException(NumericType numericType, String invalidValue, String nodeName,
	                              String newickString, int position) {
		super(buildMessage(numericType, invalidValue, nodeName), newickString, position);
		this.numericType = numericType;
		this.invalidValue = invalidValue;
		this.nodeName = nodeName;
	}

	public NewickNumericException(NumericType numericType, String invalidValue, String nodeName,
	                              String newickString, int position, Throwable cause) {
		super(buildMessage(numericType, invalidValue, nodeName), newickString, position, cause);
		this.numericType = numericType;
		this.invalidValue = invalidValue;
		this.nodeName = nodeName;
	}

	private static String buildMessage(NumericType numericType, String invalidValue, String nodeName) {
		StringBuilder sb = new StringBuilder();
		sb.append("Invalid ").append(numericType.getDescription());
		sb.append(": '").append(invalidValue).append("'");
		if (nodeName != null && !nodeName.isEmpty()) {
			sb.append(" for node '").append(nodeName).append("'");
		}
		sb.append(".\nExpected a valid number (e.g., 0.5, 1.23e-4, 100).");

		// Add specific hints based on common mistakes
		if (invalidValue != null) {
			if (invalidValue.contains(",")) {
				sb.append("\nHint: Use '.' as decimal separator, not ','");
			} else if (invalidValue.contains(" ")) {
				sb.append("\nHint: Remove spaces from the number");
			} else if (invalidValue.matches(".*[a-zA-Z].*") && !invalidValue.toLowerCase().contains("e")) {
				sb.append("\nHint: Remove non-numeric characters");
			}
		}

		return sb.toString();
	}

	public NumericType getNumericType() {
		return numericType;
	}

	public String getInvalidValue() {
		return invalidValue;
	}

	public String getNodeName() {
		return nodeName;
	}
}
