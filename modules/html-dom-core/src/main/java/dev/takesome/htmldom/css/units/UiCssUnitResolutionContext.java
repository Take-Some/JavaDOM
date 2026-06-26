package dev.takesome.htmldom.css.units;

/** Runtime data required by relative CSS units. */
public record UiCssUnitResolutionContext(float viewportWidth, float viewportHeight, float rootFontSize) {
    public static final float DEFAULT_ROOT_FONT_SIZE = 16f;

    public UiCssUnitResolutionContext {
        viewportWidth = finite(viewportWidth, 0f);
        viewportHeight = finite(viewportHeight, 0f);
        rootFontSize = Math.max(1f, finite(rootFontSize, DEFAULT_ROOT_FONT_SIZE));
    }

    public static UiCssUnitResolutionContext defaults() {
        return new UiCssUnitResolutionContext(0f, 0f, DEFAULT_ROOT_FONT_SIZE);
    }

    public static UiCssUnitResolutionContext viewport(float viewportWidth, float viewportHeight, float rootFontSize) {
        return new UiCssUnitResolutionContext(viewportWidth, viewportHeight, rootFontSize);
    }

    private static float finite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
