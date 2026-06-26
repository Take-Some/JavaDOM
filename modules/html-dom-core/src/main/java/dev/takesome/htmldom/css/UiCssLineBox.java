package dev.takesome.htmldom.css;

/** A resolved inline line box generated during CSS layout. */
public record UiCssLineBox(
        String text,
        float x,
        float y,
        float width,
        float height,
        float baseline
) {
    public UiCssLineBox {
        text = text == null ? "" : text;
        x = finite(x);
        y = finite(y);
        width = finiteNonNegative(width);
        height = finiteNonNegative(height);
        baseline = finiteNonNegative(baseline);
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0f;
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
