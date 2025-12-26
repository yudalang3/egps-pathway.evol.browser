package module.evoltreio.exception;

/**
 * Exception thrown when the Newick format parameter is invalid.
 */
public class NewickFormatException extends NewickParseException {

	private final int providedFormat;
	private final int minFormat;
	private final int maxFormat;

	public NewickFormatException(int providedFormat, int minFormat, int maxFormat) {
		super(buildMessage(providedFormat, minFormat, maxFormat));
		this.providedFormat = providedFormat;
		this.minFormat = minFormat;
		this.maxFormat = maxFormat;
	}

	private static String buildMessage(int providedFormat, int minFormat, int maxFormat) {
		return String.format(
				"Invalid Newick format: %d. Valid range is %d to %d.\n" +
				"  Format 0: Flexible with support values (leaf: name:dist, internal: support:dist)\n" +
				"  Format 1: Internal node names (leaf: name:dist, internal: name:dist)\n" +
				"  Format 2: Strict format 0 (all fields required)\n" +
				"  Format 3: Strict format 1 (all fields required)\n" +
				"  Format 4: Leaf only (leaf: name:dist, internal: none)\n" +
				"  Format 5: Internal distance only (leaf: name:dist, internal: :dist)\n" +
				"  Format 6: Internal name only (leaf: name:dist, internal: name)\n" +
				"  Format 7: Names only (leaf: name, internal: name)\n" +
				"  Format 8: Leaf names only (leaf: name, internal: none)\n" +
				"  Format 9: Pure topology (no names or distances)",
				providedFormat, minFormat, maxFormat
		);
	}

	public int getProvidedFormat() {
		return providedFormat;
	}

	public int getMinFormat() {
		return minFormat;
	}

	public int getMaxFormat() {
		return maxFormat;
	}
}
