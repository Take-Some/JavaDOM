package dev.takesome.htmldom.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

final class HtmlDomDevToolsHtmlSyntaxPainter {
    private HtmlDomDevToolsHtmlSyntaxPainter() { }

    static void drawLine(
            Graphics2D g,
            String line,
            int x,
            int y,
            Font font,
            Color text,
            Color tag,
            Color attr,
            Color value,
            Color muted
    ) {
        g.setFont(font);
        int i = 0;
        while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch != '<') {
                int next = line.indexOf('<', i);
                if (next < 0) next = line.length();
                x = drawToken(g, line.substring(i, next), x, y, text);
                i = next;
                continue;
            }
            x = drawToken(g, "<", x, y, tag);
            i++;
            if (i < line.length() && line.charAt(i) == '/') {
                x = drawToken(g, "/", x, y, tag);
                i++;
            }
            int nameStart = i;
            while (i < line.length() && isHtmlName(line.charAt(i))) i++;
            if (i > nameStart) x = drawToken(g, line.substring(nameStart, i), x, y, tag);
            while (i < line.length()) {
                char c = line.charAt(i);
                if (c == '>') {
                    x = drawToken(g, ">", x, y, tag);
                    i++;
                    break;
                }
                if (Character.isWhitespace(c)) {
                    int ws = i;
                    while (i < line.length() && Character.isWhitespace(line.charAt(i))) i++;
                    x = drawToken(g, line.substring(ws, i), x, y, text);
                    continue;
                }
                int attrStart = i;
                while (i < line.length() && isHtmlName(line.charAt(i))) i++;
                if (i > attrStart) x = drawToken(g, line.substring(attrStart, i), x, y, attr);
                if (i < line.length() && line.charAt(i) == '=') {
                    x = drawToken(g, "=", x, y, muted);
                    i++;
                }
                if (i < line.length() && (line.charAt(i) == '"' || line.charAt(i) == '\'')) {
                    char quote = line.charAt(i++);
                    int valueStart = i;
                    while (i < line.length() && line.charAt(i) != quote) i++;
                    x = drawToken(g, String.valueOf(quote), x, y, value);
                    if (i > valueStart) x = drawToken(g, line.substring(valueStart, i), x, y, value);
                    if (i < line.length()) x = drawToken(g, String.valueOf(line.charAt(i++)), x, y, value);
                }
                if (i == attrStart) {
                    x = drawToken(g, String.valueOf(line.charAt(i)), x, y, text);
                    i++;
                }
            }
        }
    }

    private static int drawToken(Graphics2D g, String token, int x, int y, Color color) {
        g.setColor(color);
        g.drawString(token, x, y);
        return x + g.getFontMetrics().stringWidth(token);
    }

    private static boolean isHtmlName(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':';
    }
}
