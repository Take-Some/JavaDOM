package dev.takesome.htmldom.css;

/** Parsed bounds shorthand: x y width height. */
public record UiCssBounds(UiCssLength x, UiCssLength y, UiCssLength width, UiCssLength height) {
    public UiCssBounds {
        x = x == null ? UiCssLength.AUTO : x;
        y = y == null ? UiCssLength.AUTO : y;
        width = width == null ? UiCssLength.AUTO : width;
        height = height == null ? UiCssLength.AUTO : height;
    }

    public static UiCssBounds parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.trim().split("\\s+");
        if (parts.length != 4) throw new IllegalArgumentException("bounds expects 4 values: x y width height");
        return new UiCssBounds(
                UiCssLength.parse(parts[0]),
                UiCssLength.parse(parts[1]),
                UiCssLength.parse(parts[2]),
                UiCssLength.parse(parts[3])
        );
    }
}
