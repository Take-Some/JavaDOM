package dev.takesome.htmldom.model;

public final class UiRect {
    public final float x;
    public final float y;
    public final float w;
    public final float h;

    public UiRect(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(0f, w);
        this.h = Math.max(0f, h);
    }

    public float right() {
        return x + w;
    }

    public float top() {
        return y + h;
    }
}
