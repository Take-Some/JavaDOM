package dev.takesome.htmldom.css;

import java.util.Locale;

/**
 * Runtime cost classification for CSS property changes.
 */
public enum UiCssStyleImpact {
    NONE,
    COMPOSITE,
    PAINT,
    LAYOUT;

    public UiCssStyleImpact merge(UiCssStyleImpact other) {
        if (other == null) return this;
        return ordinal() >= other.ordinal() ? this : other;
    }

    public boolean needsLayout() {
        return this == LAYOUT;
    }

    public boolean needsPaint() {
        return this == PAINT || this == COMPOSITE || this == LAYOUT;
    }

    public static UiCssStyleImpact of(String property) {
        String p = property == null ? "" : property.trim().toLowerCase(Locale.ROOT);
        if (p.isBlank()) return NONE;
        return switch (p) {
            case "opacity", "transform", "translate", "scale", "rotate" -> COMPOSITE;
            case "color", "background", "background-color", "border-color", "border-top-color",
                    "border-right-color", "border-bottom-color", "border-left-color",
                    "outline-color", "box-shadow", "text-shadow", "fill", "stroke" -> PAINT;
            case "display", "position", "float", "clear", "width", "height", "min-width", "min-height",
                    "max-width", "max-height", "top", "right", "bottom", "left",
                    "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
                    "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
                    "border", "border-width", "border-top-width", "border-right-width",
                    "border-bottom-width", "border-left-width", "font", "font-size", "font-family",
                    "font-weight", "line-height", "letter-spacing", "word-spacing", "white-space",
                    "text-align", "vertical-align", "gap", "row-gap", "column-gap",
                    "flex", "flex-basis", "flex-direction", "flex-wrap", "justify-content",
                    "align-items", "align-content", "align-self", "grid", "grid-template-columns",
                    "grid-template-rows", "grid-column", "grid-row", "overflow", "overflow-x", "overflow-y" -> LAYOUT;
            default -> p.endsWith("-color") ? PAINT : PAINT;
        };
    }
}
