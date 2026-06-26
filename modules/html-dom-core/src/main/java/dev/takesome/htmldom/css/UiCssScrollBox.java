package dev.takesome.htmldom.css;

/** Resolved scroll container geometry and offsets. */
public record UiCssScrollBox(
        int nodeId,
        float viewportWidth,
        float viewportHeight,
        float contentWidth,
        float contentHeight,
        float scrollX,
        float scrollY,
        boolean scrollXEnabled,
        boolean scrollYEnabled,
        boolean clipX,
        boolean clipY
) {
    public UiCssScrollBox(int nodeId, float viewportWidth, float viewportHeight, float contentWidth, float contentHeight, float scrollX, float scrollY) {
        this(nodeId, viewportWidth, viewportHeight, contentWidth, contentHeight, scrollX, scrollY, true, true, true, true);
    }

    public UiCssScrollBox {
        viewportWidth = nonNegative(viewportWidth);
        viewportHeight = nonNegative(viewportHeight);
        contentWidth = Math.max(viewportWidth, nonNegative(contentWidth));
        contentHeight = Math.max(viewportHeight, nonNegative(contentHeight));
        scrollX = scrollXEnabled ? clamp(scrollX, 0f, Math.max(0f, contentWidth - viewportWidth)) : 0f;
        scrollY = scrollYEnabled ? clamp(scrollY, 0f, Math.max(0f, contentHeight - viewportHeight)) : 0f;
    }

    public boolean scrollableX() {
        return scrollXEnabled && contentWidth > viewportWidth + 0.5f;
    }

    public boolean scrollableY() {
        return scrollYEnabled && contentHeight > viewportHeight + 0.5f;
    }

    public boolean clipsX() {
        return clipX || scrollXEnabled;
    }

    public boolean clipsY() {
        return clipY || scrollYEnabled;
    }

    private static float nonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }

    private static float clamp(float value, float min, float max) {
        float safe = Float.isFinite(value) ? value : 0f;
        return Math.max(min, Math.min(max, safe));
    }
}
