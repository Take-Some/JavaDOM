package dev.takesome.htmldom.markup.internal.parse.diagnostics;

import java.util.ArrayList;
import java.util.Collections;

/** Lazy offset -> line/column source map for parser diagnostics. */
public final class UiHtmlSourceMap {
    private final String source;
    private int[] lineStarts;

    public UiHtmlSourceMap(String source) {
        this.source = source == null ? "" : source;
    }

    public UiHtmlSourcePosition positionAt(int offset) {
        ensureLineStarts();
        int safeOffset = Math.max(0, Math.min(offset, source.length()));
        int index = findLineIndex(safeOffset);
        int column = safeOffset - lineStarts[index] + 1;
        return new UiHtmlSourcePosition(index + 1, column);
    }

    private int findLineIndex(int offset) {
        int index = java.util.Arrays.binarySearch(lineStarts, offset);
        if (index >= 0) {
            return index;
        }
        return Math.max(0, -index - 2);
    }

    private void ensureLineStarts() {
        if (lineStarts != null) {
            return;
        }
        ArrayList<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }
        lineStarts = new int[starts.size()];
        for (int i = 0; i < starts.size(); i++) {
            lineStarts[i] = starts.get(i);
        }
    }

    public record UiHtmlSourcePosition(int line, int column) {
    }
}
