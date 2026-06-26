package dev.takesome.htmldom.css.internal.parse;

/** Brace-aware block scanner for stylesheet and keyframe parsing. */
public final class UiCssBlockScanner {
    public int skipWhitespace(String source, int index) {
        int i = Math.max(0, index);
        while (source != null && i < source.length() && Character.isWhitespace(source.charAt(i))) {
            i++;
        }
        return i;
    }

    public UiCssBlock nextBlock(String source, int index) {
        if (source == null || index >= source.length()) {
            return null;
        }
        int open = source.indexOf('{', Math.max(0, index));
        if (open < 0) {
            return null;
        }
        int close = matchingBrace(source, open);
        if (close < 0) {
            return null;
        }
        return new UiCssBlock(
                source.substring(index, open),
                source.substring(open + 1, close),
                open,
                close,
                close + 1
        );
    }

    public int matchingBrace(String source, int open) {
        int depth = 0;
        char quote = 0;
        for (int i = Math.max(0, open); source != null && i < source.length(); i++) {
            char ch = source.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                }
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
