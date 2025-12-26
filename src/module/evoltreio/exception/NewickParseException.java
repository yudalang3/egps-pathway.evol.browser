package module.evoltreio.exception;

/**
 * Base exception for all Newick tree parsing errors.
 * Provides detailed error information including position and context.
 */
public class NewickParseException extends Exception {

	private final String newickString;
	private final int position;
	private final String context;

	public NewickParseException(String message) {
		super(message);
		this.newickString = null;
		this.position = -1;
		this.context = null;
	}

	public NewickParseException(String message, Throwable cause) {
		super(message, cause);
		this.newickString = null;
		this.position = -1;
		this.context = null;
	}

	public NewickParseException(String message, String newickString, int position) {
		super(buildDetailedMessage(message, newickString, position));
		this.newickString = newickString;
		this.position = position;
		this.context = extractContext(newickString, position);
	}

	public NewickParseException(String message, String newickString, int position, Throwable cause) {
		super(buildDetailedMessage(message, newickString, position), cause);
		this.newickString = newickString;
		this.position = position;
		this.context = extractContext(newickString, position);
	}

	/**
	 * Build a detailed error message with position indicator.
	 */
	private static String buildDetailedMessage(String message, String newickString, int position) {
		if (newickString == null || position < 0) {
			return message;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(message);
		sb.append("\n");

		// Show context around the error position
		String context = extractContext(newickString, position);
		int contextStart = Math.max(0, position - 20);
		int relativePosition = position - contextStart;

		sb.append("  Context: ");
		if (contextStart > 0) {
			sb.append("...");
		}
		sb.append(context);
		sb.append("\n");

		// Add position indicator
		sb.append("           ");
		if (contextStart > 0) {
			sb.append("   "); // offset for "..."
		}
		for (int i = 0; i < relativePosition && i < context.length(); i++) {
			sb.append(" ");
		}
		sb.append("^ position ").append(position);

		return sb.toString();
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
}
