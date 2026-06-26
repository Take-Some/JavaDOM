package dev.takesome.htmldom.desktop;

import java.awt.Font;
import java.io.InputStream;

final class HtmlDomDevToolsFonts {
    private static Font debugMono;
    private static Font fontAwesomeSolid;

    private HtmlDomDevToolsFonts() { }

    static Font mono(float size) {
        try {
            if (debugMono == null) {
                debugMono = loadFont("html-dom/fonts/debug.ttf");
                if (debugMono == null) debugMono = loadFont("html-dom/fonts/RobotomonoRegular.ttf");
            }
            if (debugMono != null) return debugMono.deriveFont(size);
        } catch (Exception ignored) {
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
    }

    static Font icon(float size) {
        try {
            if (fontAwesomeSolid == null) fontAwesomeSolid = loadFont("html-dom/icons/fontawesome/fa-solid-900.ttf");
            if (fontAwesomeSolid != null) return fontAwesomeSolid.deriveFont(size);
        } catch (Exception ignored) {
        }
        return mono(size);
    }

    private static Font loadFont(String resourcePath) {
        try (InputStream stream = HtmlDomDevToolsFonts.class.getClassLoader().getResourceAsStream(resourcePath)) {
            return stream == null ? null : Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception ignored) {
            return null;
        }
    }
}
