package dev.takesome.htmldom.css;

import java.util.ArrayList;
import java.util.List;

/** Simple CSS specificity tuple. */
public record UiCssSpecificity(int ids, int classes, int tags) implements Comparable<UiCssSpecificity> {
    public static UiCssSpecificity of(String selector) {
        if (selector == null || selector.isBlank() || "*".equals(selector.trim())) return new UiCssSpecificity(0, 0, 0);
        String value = selector.trim();
        int ids = count(value, '#');
        int classes = count(value, '.') + pseudoClassCount(value);
        int tags = pseudoElementCount(value) + tagNameCount(value);
        return new UiCssSpecificity(ids, classes, tags);
    }

    @Override
    public int compareTo(UiCssSpecificity other) {
        if (other == null) return 1;
        int byId = Integer.compare(ids, other.ids);
        if (byId != 0) return byId;
        int byClass = Integer.compare(classes, other.classes);
        if (byClass != 0) return byClass;
        return Integer.compare(tags, other.tags);
    }

    private static int count(String value, char needle) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == needle) count++;
        return count;
    }

    private static int pseudoClassCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != ':') continue;
            if (i + 1 < value.length() && value.charAt(i + 1) == ':') {
                i = skipPseudoToken(value, i + 2);
                continue;
            }
            String pseudo = pseudoToken(value, i + 1);
            if ("before".equalsIgnoreCase(pseudo) || "after".equalsIgnoreCase(pseudo)) {
                i = skipPseudoToken(value, i + 1);
                continue;
            }
            count++;
        }
        return count;
    }

    private static int pseudoElementCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != ':') continue;
            if (i + 1 < value.length() && value.charAt(i + 1) == ':') {
                count++;
                i = skipPseudoToken(value, i + 2);
                continue;
            }
            String pseudo = pseudoToken(value, i + 1);
            if ("before".equalsIgnoreCase(pseudo) || "after".equalsIgnoreCase(pseudo)) {
                count++;
                i = skipPseudoToken(value, i + 1);
            }
        }
        return count;
    }

    private static String pseudoToken(String value, int from) {
        int end = skipPseudoToken(value, from);
        return end <= from ? "" : value.substring(from, end);
    }

    private static int skipPseudoToken(String value, int from) {
        int index = from;
        while (index < value.length()) {
            char ch = value.charAt(index);
            if (ch == '#' || ch == '.' || ch == ':' || ch == '>' || Character.isWhitespace(ch)) break;
            index++;
        }
        return index;
    }

    private static int tagNameCount(String selector) {
        int count = 0;
        for (String part : selectorParts(selector)) {
            String value = stripPseudoElement(part).trim();
            if (value.isBlank()) continue;
            char first = value.charAt(0);
            if (first == '*' || first == '#' || first == '.' || first == ':' || first == '[') continue;
            count++;
        }
        return count;
    }

    private static String stripPseudoElement(String value) {
        int index = value.indexOf("::");
        if (index >= 0) return value.substring(0, index);
        int legacyBefore = value.indexOf(":before");
        int legacyAfter = value.indexOf(":after");
        int legacy = legacyBefore >= 0 && legacyAfter >= 0 ? Math.min(legacyBefore, legacyAfter) : Math.max(legacyBefore, legacyAfter);
        return legacy >= 0 ? value.substring(0, legacy) : value;
    }

    private static List<String> selectorParts(String selector) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < selector.length(); i++) {
            char ch = selector.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == '(' || ch == '[') depth++;
            else if ((ch == ')' || ch == ']') && depth > 0) depth--;
            if ((Character.isWhitespace(ch) || ch == '>') && depth == 0) {
                addPart(out, current);
                continue;
            }
            current.append(ch);
        }
        addPart(out, current);
        return out;
    }

    private static void addPart(List<String> out, StringBuilder current) {
        String value = current.toString().trim();
        if (!value.isBlank()) out.add(value);
        current.setLength(0);
    }
}
