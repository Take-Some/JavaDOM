package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;

/** Context passed before an inline run is committed into layout result boxes. */
public record UiCssInlineRunTextEvent(
        UiDomElement owner,
        UiDomElement styleElement,
        int sourceNodeId,
        String text,
        boolean atomic,
        boolean lineBreak,
        boolean spaceBefore,
        float leadingAdvance,
        float x,
        float y,
        float width,
        float height,
        float baseline,
        int lineIndex,
        int runIndex
) {
    public UiCssInlineRunTextEvent {
        text = text == null ? "" : text;
        leadingAdvance = finiteNonNegative(leadingAdvance);
        x = finite(x);
        y = finite(y);
        width = finiteNonNegative(width);
        height = finiteNonNegative(height);
        baseline = finiteNonNegative(baseline);
        lineIndex = Math.max(0, lineIndex);
        runIndex = Math.max(0, runIndex);
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0f;
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
