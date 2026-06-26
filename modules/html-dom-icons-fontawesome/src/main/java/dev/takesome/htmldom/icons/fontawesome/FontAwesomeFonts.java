package dev.takesome.htmldom.icons.fontawesome;

import dev.takesome.htmldom.fonts.HtmlDomFontRegistry;

/** Registers bundled Font Awesome TTF files into the HtmlDom font registry. */
public final class FontAwesomeFonts {
    private FontAwesomeFonts() {
    }

    public static void register(HtmlDomFontRegistry registry) {
        if (registry == null) return;
        for (FontAwesomeStyle style : FontAwesomeStyle.values()) {
            String family = style.displayName();
            registry.registerClasspath(fontId(style), style.resource().classpathPath(), family, true);
        }
    }

    public static String fontId(FontAwesomeStyle style) {
        FontAwesomeStyle resolved = style == null ? FontAwesomeStyle.SOLID : style;
        return "fontawesome:" + resolved.styleId();
    }
}
