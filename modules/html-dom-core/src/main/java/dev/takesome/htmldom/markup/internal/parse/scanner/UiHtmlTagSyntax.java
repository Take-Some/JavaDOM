package dev.takesome.htmldom.markup.internal.parse.scanner;

/** Low-level tag syntax helpers used by the scanner. */
final class UiHtmlTagSyntax {
    private UiHtmlTagSyntax() {
    }

    static int findTagEnd(String source, int index) {
        char quote = 0;
        for (int i = index; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                }
                continue;
            }
            if (ch == '"' || ch == '\'') {
                quote = ch;
                continue;
            }
            if (ch == '>') {
                return i;
            }
        }
        return -1;
    }

    static String closingName(String rawName, UiHtmlNameCanonicalizer names) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }
        String trimmed = rawName.trim();
        int end = 0;
        while (end < trimmed.length() && isNameChar(trimmed.charAt(end))) {
            end++;
        }
        return end <= 0 ? "" : names.canonical(trimmed.substring(0, end));
    }

    static int skipWhitespace(String source, int index) {
        int i = index;
        while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
            i++;
        }
        return i;
    }

    static int lastNonWhitespace(String value) {
        for (int i = value.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    static boolean isNameChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':';
    }
}
