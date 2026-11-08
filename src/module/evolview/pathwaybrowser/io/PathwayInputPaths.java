package module.evolview.pathwaybrowser.io;

/** Shared optional-path rules at parser, preparation and mounting boundaries. */
public final class PathwayInputPaths {
    private PathwayInputPaths() { }
    public static boolean disabled(String path) {
        if (path == null || path.isBlank()) return true;
        return switch (path.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "false", "f", "null" -> true;
            default -> false;
        };
    }
    public static String normalize(String path) { return disabled(path) ? "" : path.strip(); }
}
