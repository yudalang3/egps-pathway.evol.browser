package module.evoltreio.parser;

import java.util.ArrayList;
import java.util.List;

/** Quote-aware text operations shared by the importer and graphics codec. */
public final class NewickText {
    private NewickText() { }

    public static String quote(String name) {
        if (name == null) return "";
        boolean quoted = name.chars().anyMatch(c -> Character.isWhitespace(c) || "()[]':;,\"".indexOf(c) >= 0);
        return quoted ? "'" + name.replace("'", "''") + "'" : name;
    }

    public static String unquote(String text) {
        String value = text.trim();
        if (value.length() >= 2 && (value.charAt(0) == '\'' || value.charAt(0) == '"')) {
            char quote = value.charAt(0);
            if (value.charAt(value.length() - 1) != quote) throw new IllegalArgumentException("Unclosed quoted node name");
            return value.substring(1, value.length() - 1).replace("" + quote + quote, "" + quote);
        }
        return value;
    }

    /** Return the end of one quoted segment, including doubled quote escapes. */
    public static int quotedEnd(String text, int start) {
        char quote = text.charAt(start);
        for (int i = start + 1; i < text.length(); i++) {
            if (text.charAt(i) != quote) continue;
            if (i + 1 < text.length() && text.charAt(i + 1) == quote) { i++; continue; }
            return i + 1;
        }
        throw new IllegalArgumentException("Unclosed quoted node name at position " + start);
    }

    public static int labelEnd(String text, int start) {
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\'' || c == '"') i = quotedEnd(text, i);
            else if ("(),;".indexOf(c) >= 0) break;
            else i++;
        }
        return i;
    }

    public static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' || c == '"') i = quotedEnd(text, i) - 1;
            else if (c == ':') { parts.add(text.substring(start, i)); start = i + 1; }
        }
        parts.add(text.substring(start));
        return parts;
    }

    public static String removeWhitespace(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' || c == '"') {
                int end = quotedEnd(text, i);
                result.append(text, i, end); i = end - 1;
            } else if (!Character.isWhitespace(c)) result.append(c);
        }
        return result.toString();
    }
}
