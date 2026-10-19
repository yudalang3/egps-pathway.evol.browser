package module.evolview.moderntreeviewer.para;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict JSON values with comments, trailing commas and duplicate-key diagnostics. */
final class SettingsJson {
    private final String text;
    private int position;
    private final Map<String, int[]> spans = new LinkedHashMap<>();

    private SettingsJson(String text) { this.text = text; }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String text) {
        SettingsJson parser = new SettingsJson(text);
        Object value = parser.value("$");
        parser.space();
        if (parser.position != text.length()) throw parser.error("Unexpected text after the settings object");
        if (!(value instanceof Map)) throw parser.error("Settings must be a JSON object");
        return (Map<String, Object>) value;
    }

    private Object value(String path) {
        space();
        int start = position;
        Object result = readValue(path);
        spans.put(path, new int[]{start, position});
        return result;
    }

    private Object readValue(String path) {
        if (position == text.length()) throw error("Expected a value at " + path);
        char c = text.charAt(position);
        if (c == '{') return object(path);
        if (c == '[') return array(path);
        if (c == '"') return string();
        for (String literal : new String[] {"true", "false", "null"}) {
            if (text.startsWith(literal, position)) {
                position += literal.length();
                return literal.equals("null") ? null : Boolean.valueOf(literal);
            }
        }
        int start = position;
        if (c == '-') position++;
        if (position < text.length() && text.charAt(position) == '0') position++;
        else digits();
        if (position < text.length() && text.charAt(position) == '.') { position++; digits(); }
        if (position < text.length() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
            position++;
            if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) position++;
            digits();
        }
        try { return new BigDecimal(text.substring(start, position)); }
        catch (NumberFormatException error) { throw error("Expected a JSON value at " + path); }
    }

    /** Replace only values; untouched whitespace and comments retain their original bytes. */
    static String update(String text, Map<String, Object> updates) {
        parse(text);
        for (var entry : updates.entrySet()) {
            SettingsJson parser = new SettingsJson(text);
            parser.value("$");
            String[] parts = entry.getKey().split("/");
            String path = "$";
            int depth = 0;
            while (depth < parts.length && parser.spans.containsKey(path + " / " + parts[depth])) path += " / " + parts[depth++];
            String replacement = com.alibaba.fastjson.JSON.toJSONString(entry.getValue());
            if (depth == parts.length) {
                int[] span = parser.spans.get(path);
                text = text.substring(0, span[0]) + replacement + text.substring(span[1]);
            } else {
                String newline = text.contains("\r\n") ? "\r\n" : "\n";
                for (int i = parts.length - 1; i > depth; i--) replacement = "{" + newline + "    ".repeat(i + 1) + com.alibaba.fastjson.JSON.toJSONString(parts[i]) + ": " + replacement + newline + "    ".repeat(i) + "}";
                int[] span = parser.spans.get(path);
                // Find the final real token before the closing brace, ignoring comments.
                SettingsJson tail = new SettingsJson(text);
                tail.position = span[0] + 1;
                int last = span[0];
                while (tail.position < span[1] - 1) {
                    tail.space();
                    if (tail.position >= span[1] - 1) break;
                    if (text.charAt(tail.position) == '"') tail.string(); else tail.position++;
                    last = tail.position - 1;
                }
                boolean comma = last != span[0] && text.charAt(last) != ',';
                if (comma) {
                    text = text.substring(0, last + 1) + "," + text.substring(last + 1);
                    span[1]++;
                }
                int close = span[1] - 1;
                String addition = newline + "    ".repeat(depth + 1) + com.alibaba.fastjson.JSON.toJSONString(parts[depth]) + ": " + replacement + newline + "    ".repeat(depth);
                text = text.substring(0, close) + addition + text.substring(close);
            }
        }
        parse(text);
        return text;
    }

    private Map<String, Object> object(String path) {
        position++;
        Map<String, Object> result = new LinkedHashMap<>();
        space();
        if (take('}')) return result;
        while (true) {
            space();
            if (position == text.length() || text.charAt(position) != '"') throw error("Use a double-quoted name at " + path);
            String key = string();
            String child = path + " / " + key;
            if (result.containsKey(key)) throw error("Duplicate setting: " + child);
            space();
            expect(':');
            result.put(key, value(child));
            space();
            if (take('}')) return result;
            expect(',');
            space();
            if (take('}')) return result;
        }
    }

    private List<Object> array(String path) {
        position++;
        List<Object> result = new ArrayList<>();
        space();
        if (take(']')) return result;
        while (true) {
            result.add(value(path + "[" + result.size() + "]"));
            space();
            if (take(']')) return result;
            expect(',');
            space();
            if (take(']')) return result;
        }
    }

    private String string() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (position < text.length()) {
            char c = text.charAt(position++);
            if (c == '"') return result.toString();
            if (c < 32) throw error("Unescaped control character in string");
            if (c != '\\') { result.append(c); continue; }
            if (position == text.length()) throw error("Unterminated string escape");
            char escaped = text.charAt(position++);
            switch (escaped) {
                case '"', '\\', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (position + 4 > text.length()) throw error("Incomplete Unicode escape");
                    String hex = text.substring(position, position + 4);
                    if (!hex.matches("[0-9a-fA-F]{4}")) throw error("Invalid Unicode escape");
                    result.append((char) Integer.parseInt(hex, 16));
                    position += 4;
                }
                default -> throw error("Invalid string escape");
            }
        }
        throw error("Unterminated string");
    }

    private void digits() {
        int start = position;
        while (position < text.length() && text.charAt(position) >= '0' && text.charAt(position) <= '9') position++;
        if (start == position) throw error("Expected a number");
    }

    private void space() {
        while (position < text.length()) {
            char c = text.charAt(position);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || (position == 0 && c == '\uFEFF')) { position++; continue; }
            if (text.startsWith("//", position)) {
                while (position < text.length() && text.charAt(position) != '\n' && text.charAt(position) != '\r') position++;
            } else if (text.startsWith("/*", position)) {
                int end = text.indexOf("*/", position + 2);
                if (end < 0) throw error("Unterminated block comment");
                position = end + 2;
            } else return;
        }
    }

    private boolean take(char c) {
        if (position < text.length() && text.charAt(position) == c) { position++; return true; }
        return false;
    }

    private void expect(char c) { if (!take(c)) throw error("Expected '" + c + "'"); }

    private IllegalArgumentException error(String message) {
        int line = 1, column = 1;
        for (int i = 0; i < position; i++) {
            char c = text.charAt(i);
            if (c == '\r' || c == '\n' && (i == 0 || text.charAt(i - 1) != '\r')) { line++; column = 1; }
            else if (c != '\n') column++;
        }
        return new IllegalArgumentException(message + " (line " + line + ", column " + column + ")");
    }
}
