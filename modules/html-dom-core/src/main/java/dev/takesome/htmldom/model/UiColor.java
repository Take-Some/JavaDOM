package dev.takesome.htmldom.model;

/**
 * Small engine-neutral color value used by the UI layer.
 * Adapt this to any rendering backend at the integration boundary.
 */
public final class UiColor {
    public static final UiColor WHITE = new UiColor(1f, 1f, 1f, 1f);
    public static final UiColor TRANSPARENT = new UiColor(0f, 0f, 0f, 0f);

    public final float r;
    public final float g;
    public final float b;
    public final float a;

    public UiColor(float r, float g, float b, float a) {
        this.r = clamp01(r);
        this.g = clamp01(g);
        this.b = clamp01(b);
        this.a = clamp01(a);
    }

    public static UiColor rgba255(int r, int g, int b, int a) {
        return new UiColor(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public UiColor withAlpha(float alpha) {
        return new UiColor(r, g, b, alpha);
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
