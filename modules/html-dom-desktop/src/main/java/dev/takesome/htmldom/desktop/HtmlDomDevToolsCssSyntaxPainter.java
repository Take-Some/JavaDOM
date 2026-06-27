package dev.takesome.htmldom.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

final class HtmlDomDevToolsCssSyntaxPainter {
    private HtmlDomDevToolsCssSyntaxPainter() { }

    static void drawLine(
            Graphics2D g,
            String line,
            int x,
            int y,
            Font font,
            Color text,
            Color selector,
            Color property,
            Color value,
            Color muted
    ) {
        g.setFont(font);
        String s = line == null ? "" : line;
        String trimmed = s.trim();
        if (trimmed.startsWith("/*")) {
            drawToken(g, s, x, y, muted);
            return;
        }
        int brace = s.indexOf('{');
        if (brace >= 0) {
            x = drawToken(g, s.substring(0, brace), x, y, selector);
            drawToken(g, s.substring(brace), x, y, muted);
            return;
        }
        if (trimmed.equals("}")) {
            drawToken(g, s, x, y, muted);
            return;
        }
        int colon = s.indexOf(':');
        if (colon > 0) {
            int leading = 0;
            while (leading < s.length() && Character.isWhitespace(s.charAt(leading))) leading++;
            if (leading > 0) x = drawToken(g, s.substring(0, leading), x, y, text);
            x = drawToken(g, s.substring(leading, colon), x, y, property);
            x = drawToken(g, ":", x, y, muted);
            int semi = s.lastIndexOf(';');
            int valueEnd = semi > colon ? semi : s.length();
            x = drawToken(g, s.substring(colon + 1, valueEnd), x, y, value);
            if (semi > colon) drawToken(g, s.substring(semi), x, y, muted);
            return;
        }
        drawToken(g, s, x, y, text);
    }

    private static int drawToken(Graphics2D g, String token, int x, int y, Color color) {
        g.setColor(color);
        g.drawString(token, x, y);
        return x + g.getFontMetrics().stringWidth(token);
    }
}
