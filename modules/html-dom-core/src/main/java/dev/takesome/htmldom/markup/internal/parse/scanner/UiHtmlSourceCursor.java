package dev.takesome.htmldom.markup.internal.parse.scanner;

/** Mutable character cursor for scanner hot paths. */
public final class UiHtmlSourceCursor {
    private final String source;
    private final int length;
    private int offset;
    private int line = 1;
    private int column = 1;

    public UiHtmlSourceCursor(String source) {
        this.source = source == null ? "" : source;
        this.length = this.source.length();
    }

    public boolean eof() {
        return offset >= length;
    }

    public char peek() {
        return eof() ? '\0' : source.charAt(offset);
    }

    public char peek(int ahead) {
        int index = offset + ahead;
        return index >= length ? '\0' : source.charAt(index);
    }

    public char next() {
        if (eof()) {
            return '\0';
        }
        char c = source.charAt(offset++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    public boolean startsWith(String prefix) {
        return prefix != null && source.startsWith(prefix, offset);
    }

    public int offset() {
        return offset;
    }

    public int length() {
        return length;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public String source() {
        return source;
    }

    public String slice(int start, int end) {
        int safeStart = Math.max(0, Math.min(start, length));
        int safeEnd = Math.max(safeStart, Math.min(end, length));
        return source.substring(safeStart, safeEnd);
    }

    public int indexOf(String needle, int from) {
        return source.indexOf(needle, Math.max(0, from));
    }

    public int indexOfIgnoreCase(String needle, int from) {
        if (needle == null || needle.isEmpty()) {
            return -1;
        }
        int max = length - needle.length();
        for (int i = Math.max(0, from); i <= max; i++) {
            if (source.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    public void moveTo(int targetOffset) {
        int safeTarget = Math.max(0, Math.min(targetOffset, length));
        while (offset < safeTarget) {
            next();
        }
    }

    public void moveToEnd() {
        moveTo(length);
    }
}
