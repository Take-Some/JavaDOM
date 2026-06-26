package dev.takesome.htmldom.css;

import java.util.ArrayList;
import java.util.List;

/** Shared CSS token splitting helpers that respect nested functions and quoted strings. */
public final class UiCssTokenSplitter {
    private UiCssTokenSplitter() {
    }

    public static List<String> splitTopLevelComma(String raw) {
        return splitTopLevel(raw, ',', false);
    }

    public static List<String> splitTopLevelWhitespace(String raw) {
        return splitTopLevel(raw, '\0', true);
    }

    public static List<String> splitFunctionArgs(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String value = raw.trim();
        int open = value.indexOf('(');
        if (open >= 0 && closesAtEnd(value, open)) value = value.substring(open + 1, value.length() - 1);
        return splitTopLevelComma(value);
    }

    private static boolean closesAtEnd(String value, int open) {
        int depth = 0;
        char quote = 0;
        for (int i = open; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (quote != 0) {
                if (ch == '\\' && i + 1 < value.length()) {
                    i++;
                    continue;
                }
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                continue;
            }
            if (ch == '(') depth++;
            else if (ch == ')' && depth > 0) {
                depth--;
                if (depth == 0) return i == value.length() - 1;
            }
        }
        return false;
    }

    private static List<String> splitTopLevel(String raw, char delimiter, boolean whitespace) {
        ArrayList<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == '\\' && i + 1 < raw.length()) {
                    current.append(raw.charAt(++i));
                    continue;
                }
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == '(') {
                depth++;
                current.append(ch);
                continue;
            }
            if (ch == ')' && depth > 0) {
                depth--;
                current.append(ch);
                continue;
            }
            if (depth == 0 && (whitespace ? Character.isWhitespace(ch) : ch == delimiter)) {
                flush(current, out);
                continue;
            }
            current.append(ch);
        }
        flush(current, out);
        return out;
    }

    private static void flush(StringBuilder current, List<String> out) {
        String value = current.toString().trim();
        if (!value.isBlank()) out.add(value);
        current.setLength(0);
    }
}
