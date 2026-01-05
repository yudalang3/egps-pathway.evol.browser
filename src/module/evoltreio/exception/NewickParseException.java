package module.evoltreio.exception;

/**
 * Base exception for all Newick tree parsing errors.
 * Provides detailed error information including position and context.
 */
public class NewickParseException extends Exception {

	private final String newickString;
	private final int position;
	private final String context;
	private final String suggestedFix;

	public NewickParseException(String message) {
		super(message);
		this.newickString = null;
		this.position = -1;
		this.context = null;
		this.suggestedFix = null;
	}

	public NewickParseException(String message, Throwable cause) {
		super(message, cause);
		this.newickString = null;
		this.position = -1;
		this.context = null;
		this.suggestedFix = null;
	}

	public NewickParseException(String message, String newickString, int position) {
		this(message, newickString, position, (String) null);
	}

	public NewickParseException(String message, String newickString, int position, String suggestedFix) {
		super(buildDetailedMessage(message, newickString, position, suggestedFix));
		this.newickString = newickString;
		this.position = position;
		this.context = extractContext(newickString, position);
		this.suggestedFix = suggestedFix;
	}

	public NewickParseException(String message, String newickString, int position, Throwable cause) {
		this(message, newickString, position, null, cause);
	}

	public NewickParseException(String message, String newickString, int position, String suggestedFix, Throwable cause) {
		super(buildDetailedMessage(message, newickString, position, suggestedFix), cause);
		this.newickString = newickString;
		this.position = position;
		this.context = extractContext(newickString, position);
		this.suggestedFix = suggestedFix;
	}

	/**
	 * Build a detailed error message with position indicator.
	 */
	private static String buildDetailedMessage(String message, String newickString, int position, String suggestedFix) {
		if (newickString == null || position < 0) {
			return message;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(message);
		sb.append("\n");

		// Show error position
		sb.append("  Position: ").append(position);
		sb.append("\n");

		// Show context around the error position
		String context = extractContext(newickString, position);
		int contextStart = Math.max(0, position - 20);

		sb.append("  Context: ");
		if (contextStart > 0) {
			sb.append("...");
		}
		sb.append(context);
		if (position + 20 < newickString.length()) {
			sb.append("...");
		}

		// Show expected (corrected) version if suggested fix is provided
		if (suggestedFix != null && !suggestedFix.isEmpty()) {
			sb.append("\n");
			sb.append("  Expected: ");
			if (contextStart > 0) {
				sb.append("...");
			}
			// Insert or append the fix to show what it should look like
			String correctedContext = applySuggestedFix(newickString, position, suggestedFix);
			sb.append(correctedContext);
			if (position + 20 < newickString.length()) {
				sb.append("...");
			}
		}

		return sb.toString();
	}

	/**
	 * Apply the suggested fix to generate a corrected context string.
	 */
	private static String applySuggestedFix(String newickString, int position, String suggestedFix) {
		// Build the corrected string by inserting the fix at the position
		StringBuilder corrected = new StringBuilder();
		int start = Math.max(0, position - 20);
		int end = Math.min(newickString.length(), position + 20);

		// Get context before position
		String before = newickString.substring(start, Math.min(position + 1, newickString.length()));
		// Get context after position (if any)
		String after = position + 1 < end ? newickString.substring(position + 1, end) : "";

		corrected.append(before);
		corrected.append(suggestedFix);
		corrected.append(after);

		return corrected.toString();
	}

	/**
	 * Extract context around the error position.
	 */
	private static String extractContext(String newickString, int position) {
		if (newickString == null) {
			return "";
		}
		int start = Math.max(0, position - 20);
		int end = Math.min(newickString.length(), position + 20);
		return newickString.substring(start, end);
	}

	public String getNewickString() {
		return newickString;
	}

	public int getPosition() {
		return position;
	}

	public String getContext() {
		return context;
	}

	public String getSuggestedFix() {
		return suggestedFix;
	}
}
