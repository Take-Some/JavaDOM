package dev.takesome.htmldom.fonts;

/** Global default font registry for desktop HtmlDom integrations. */
public final class HtmlDomFonts {
    private static final HtmlDomFontRegistry REGISTRY = new HtmlDomFontRegistry().loadBuiltIns();

    private HtmlDomFonts() {
    }

    public static HtmlDomFontRegistry registry() {
        return REGISTRY;
    }
}
