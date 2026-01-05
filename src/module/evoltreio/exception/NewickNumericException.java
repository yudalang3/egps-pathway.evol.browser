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
			// Check if this looks like a node name rather than a number
			if (numericType == NumericType.SUPPORT_VALUE && looksLikeNodeName(invalidValue)) {
				sb.append("\n\nHint: '").append(invalidValue).append("' looks like an internal node name, not a support value.");
				sb.append("\n      Your tree likely uses Format 1 (internal node names) instead of Format 0 (support values).");
				sb.append("\n      Try setting: newick.format=1");
				sb.append("\n\n      Format 0: Internal nodes have support values, e.g., (A,B)95:0.3");
				sb.append("\n      Format 1: Internal nodes have names, e.g., (A,B)Clade1:0.3");
			} else if (invalidValue.contains(",")) {
				sb.append("\nHint: Use '.' as decimal separator, not ','");
			} else if (invalidValue.contains(" ")) {
				sb.append("\nHint: Remove spaces from the number");
			} else if (invalidValue.matches(".*[a-zA-Z].*") && !invalidValue.toLowerCase().contains("e")) {
				sb.append("\nHint: Remove non-numeric characters");
			}
		}

		return sb.toString();
	}

	/**
	 * Check if the value looks like a node name rather than a number.
	 * A value looks like a name if it:
	 * - Contains letters (excluding scientific notation 'e'/'E')
	 * - Starts with a letter
	 * - Is a reasonable length for a name
	 */
	private static boolean looksLikeNodeName(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		// Remove scientific notation pattern first
		String withoutScientific = value.replaceAll("[eE][+-]?\\d+$", "");
		// Check if it starts with a letter or contains multiple letters
		boolean startsWithLetter = Character.isLetter(value.charAt(0));
		boolean hasMultipleLetters = withoutScientific.replaceAll("[^a-zA-Z]", "").length() > 1;
		// Check if it's mostly alphabetic (more than 50% letters)
		int letterCount = withoutScientific.replaceAll("[^a-zA-Z]", "").length();
		boolean mostlyAlphabetic = letterCount > withoutScientific.length() / 2;

		return startsWithLetter || (hasMultipleLetters && mostlyAlphabetic);
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
