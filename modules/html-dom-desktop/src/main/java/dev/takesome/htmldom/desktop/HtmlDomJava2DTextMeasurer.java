package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiIntrinsicTextMeasurer;
import dev.takesome.htmldom.css.UiIntrinsicTextMetrics;
import dev.takesome.htmldom.fonts.HtmlDomFonts;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

/** Text measurer backed by the same AWT/Java2D font registry used by the Swing renderer. */
final class HtmlDomJava2DTextMeasurer implements UiIntrinsicTextMeasurer {
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    @Override
    public UiIntrinsicTextMetrics measure(String text, String fontId, float scale, float fallbackFontSize) {
        String value = text == null ? "" : text;
        if (value.isEmpty()) return UiIntrinsicTextMetrics.ZERO;
        float size = Math.max(1f, fallbackFontSize) * Math.max(0.01f, scale);
        Font font = HtmlDomFonts.registry().font(fontId == null ? "" : fontId, style(fontId), size);
        Rectangle2D bounds = font.getStringBounds(value, FRC);
        float width = (float) Math.ceil(bounds.getWidth());
        float height = (float) Math.ceil(font.getLineMetrics(value, FRC).getHeight());
        return new UiIntrinsicTextMetrics(width, height);
    }

    private int style(String fontId) {
        String key = fontId == null ? "" : fontId.toLowerCase(Locale.ROOT);
        boolean bold = key.contains("bold") || key.contains("heavy") || key.contains("black");
        boolean italic = key.contains("italic") || key.contains("oblique");
        if (bold && italic) return Font.BOLD | Font.ITALIC;
        if (bold) return Font.BOLD;
        if (italic) return Font.ITALIC;
        return Font.PLAIN;
    }
}
