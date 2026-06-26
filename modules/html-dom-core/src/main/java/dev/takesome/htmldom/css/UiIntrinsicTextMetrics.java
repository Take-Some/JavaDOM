package dev.takesome.htmldom.css;

public record UiIntrinsicTextMetrics(float width, float height) {
    public static final UiIntrinsicTextMetrics ZERO = new UiIntrinsicTextMetrics(0f, 0f);

    public UiIntrinsicTextMetrics {
        width = finiteNonNegative(width);
        height = finiteNonNegative(height);
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
