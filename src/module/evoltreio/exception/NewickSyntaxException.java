package module.evoltreio.exception;

/**
 * Exception thrown when the Newick string has syntax errors.
 */
public class NewickSyntaxException extends NewickParseException {

	public enum SyntaxErrorType {
		UNMATCHED_PARENTHESIS("Unmatched parenthesis"),
		MISSING_SEMICOLON("Missing terminating semicolon"),
		UNEXPECTED_CHARACTER("Unexpected character"),
		EMPTY_INPUT("Empty or null Newick string"),
		INVALID_BRANCH_LENGTH("Invalid branch length format"),
		MISSING_COMMA("Missing comma between siblings"),
		EXTRA_COMMA("Extra comma found"),
		EMPTY_NODE_NAME("Empty node name where name is required"),
		INVALID_SUPPORT_VALUE("Invalid support value format");

		private final String description;

		SyntaxErrorType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	private final SyntaxErrorType errorType;
	private final String expectedToken;
	private final String foundToken;

	public NewickSyntaxException(SyntaxErrorType errorType, String newickString, int position) {
		super(errorType.getDescription(), newickString, position, getSuggestedFixForType(errorType, null));
		this.errorType = errorType;
		this.expectedToken = null;
		this.foundToken = null;
	}

	public NewickSyntaxException(SyntaxErrorType errorType, String newickString, int position,
	                             String expectedToken, String foundToken) {
		super(buildDetailedMessage(errorType, expectedToken, foundToken), newickString, position, expectedToken);
		this.errorType = errorType;
		this.expectedToken = expectedToken;
		this.foundToken = foundToken;
	}

	/**
	 * Get a suggested fix string based on the error type when no expected token is provided.
	 */
	private static String getSuggestedFixForType(SyntaxErrorType errorType, String expectedToken) {
		if (expectedToken != null) {
			return expectedToken;
		}
		switch (errorType) {
			case MISSING_SEMICOLON:
				return ";";
			case MISSING_COMMA:
				return ",";
			default:
				return null;
		}
	}

	private static String buildDetailedMessage(SyntaxErrorType errorType, String expected, String found) {
		StringBuilder sb = new StringBuilder();
		sb.append(errorType.getDescription());
		if (expected != null) {
			sb.append(". Expected: '").append(expected).append("'");
		}
		if (found != null) {
			sb.append(", Found: '").append(found).append("'");
		}
		return sb.toString();
	}

	public SyntaxErrorType getErrorType() {
		return errorType;
	}

	public String getExpectedToken() {
		return expectedToken;
	}

	public String getFoundToken() {
		return foundToken;
	}
}
