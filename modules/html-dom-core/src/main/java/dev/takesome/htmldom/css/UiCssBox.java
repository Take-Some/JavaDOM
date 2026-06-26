package dev.takesome.htmldom.css;

import dev.takesome.htmldom.model.UiRect;

/** Resolved absolute UI CSS layout box in pixels. */
public record UiCssBox(float x, float y, float width, float height) {
    public UiCssBox {
        width = Math.max(0f, width);
        height = Math.max(0f, height);
    }

    public UiRect rect() {
        return new UiRect(x, y, width, height);
    }
}
