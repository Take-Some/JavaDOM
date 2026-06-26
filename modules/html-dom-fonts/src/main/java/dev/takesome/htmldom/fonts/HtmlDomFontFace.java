package dev.takesome.htmldom.fonts;

import java.awt.Font;

/** Registered classpath font face. */
public record HtmlDomFontFace(String id, String family, String source, Font baseFont, boolean iconFont) {
    public HtmlDomFontFace {
        id = clean(id, "id");
        family = clean(family, "family");
        source = clean(source, "source").replace('\\', '/');
        if (baseFont == null) throw new IllegalArgumentException("baseFont must not be null");
    }

    public Font derive(int style, float size) {
        return baseFont.deriveFont(style, Math.max(1f, size));
    }

    private static String clean(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
