package dev.takesome.htmldom.model;

public final class UiBoxShadow {
    private final float offsetX;
    private final float offsetY;
    private final float blurRadius;
    private final float spreadRadius;
    private final UiColor color;
    private final boolean inset;

    public UiBoxShadow(float offsetX, float offsetY, float blurRadius, float spreadRadius, UiColor color, boolean inset) {
        this.offsetX = Float.isFinite(offsetX) ? offsetX : 0f;
        this.offsetY = Float.isFinite(offsetY) ? offsetY : 0f;
        this.blurRadius = Float.isFinite(blurRadius) ? Math.max(0f, blurRadius) : 0f;
        this.spreadRadius = Float.isFinite(spreadRadius) ? spreadRadius : 0f;
        this.color = color == null ? UiColor.TRANSPARENT : color;
        this.inset = inset;
    }

    public float offsetX() { return offsetX; }
    public float offsetY() { return offsetY; }
    public float blurRadius() { return blurRadius; }
    public float spreadRadius() { return spreadRadius; }
    public UiColor color() { return color; }
    public boolean inset() { return inset; }
    public boolean visible() { return color.a > 0f && (offsetX != 0f || offsetY != 0f || blurRadius > 0f || spreadRadius != 0f); }
}
