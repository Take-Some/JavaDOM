package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssInlineBox;
import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.css.UiCssLineBox;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;
import dev.takesome.htmldom.dom.UiDomText;
import dev.takesome.htmldom.dom.UiDomTraversal;
import dev.takesome.htmldom.fonts.HtmlDomFonts;
import dev.takesome.htmldom.icons.fontawesome.FontAwesomeFonts;
import dev.takesome.htmldom.icons.fontawesome.FontAwesomeIcons;
import dev.takesome.htmldom.icons.fontawesome.FontAwesomeStyle;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Text, inline-run, icon and simple form-control painter for the desktop Java2D renderer. */
public final class HtmlDomTextPaintEngine {
    public void paintSpecialOrText(Graphics2D g, UiDomElement element, Rectangle r, Context context) {
        String tag = element.tagName();
        if ("img".equals(tag) && context.paint().paintImage(g, element, r)) {
            return;
        }
        String iconGlyph = FontAwesomeIcons.glyph(element.classList().values());
        if (!iconGlyph.isBlank()) {
            drawIcon(g, element, iconGlyph, r, context);
            return;
        }
        UiCssLayoutResult layout = context.layout();
        if (layout != null && !layout.inlineBoxes(element).isEmpty()) {
            paintInlineBoxes(g, element, layout.inlineBoxes(element), context);
            return;
        }
        if (layout != null && !layout.lineBoxes(element).isEmpty()) {
            paintLineBoxes(g, element, layout.lineBoxes(element), context);
            return;
        }
        String text = directText(element);
        if (!text.isBlank()) drawText(g, element, text, r, "button".equals(tag) || element.classList().contains("brand-mark"), context);
    }

    public void paintInlineBoxes(Graphics2D g, UiDomElement owner, List<UiCssInlineBox> runs, Context context) {
        if (runs == null || runs.isEmpty()) return;
        for (UiCssInlineBox run : runs) {
            UiDomElement styleElement = elementByNodeId(context.document(), run.styleNodeId());
            if (styleElement == null) styleElement = owner;
            String text = run.text();
            Font font;
            int size = Math.max(8, Math.round(context.paint().length(styleElement.style("font-size", "15px"), Math.max(8f, run.height() * 0.8f))));
            if (run.replaced()) {
                Rectangle runRect = new Rectangle(Math.round(run.x()), Math.round(context.viewportHeight() - run.y() - run.height()), Math.round(run.width()), Math.round(run.height()));
                if ("img".equals(styleElement.tagName()) && context.paint().paintImage(g, styleElement, runRect)) {
                    continue;
                }
                String glyph = FontAwesomeIcons.glyph(styleElement.classList().values());
                if (glyph.isBlank() && inlineBoxLike(styleElement)) {
                    paintInlineBlockRun(g, styleElement, run, context);
                    continue;
                }
                text = glyph.isBlank() ? "□" : glyph;
                FontAwesomeStyle iconStyle = FontAwesomeIcons.style(styleElement.classList().values());
                font = HtmlDomFonts.registry().font(FontAwesomeFonts.fontId(iconStyle) + ", " + iconStyle.displayName(), Font.PLAIN, size);
            } else {
                font = fontFor(styleElement, size, fontStyle(styleElement));
            }
            if (text == null || text.isBlank()) continue;
            g.setFont(font);
            g.setColor(context.paint().color(styleElement.style("color", ""), inheritedColor(styleElement, context)));
            int x = Math.round(run.x());
            int baselineY = Math.round(context.viewportHeight() - run.y() - run.height() + run.baseline());
            g.drawString(text, x, baselineY);
        }
    }

    public void paintLineBoxes(Graphics2D g, UiDomElement element, List<UiCssLineBox> lines, Context context) {
        if (lines == null || lines.isEmpty()) return;
        int size = Math.max(8, Math.round(context.paint().length(element.style("font-size", "15px"), 15)));
        int style = fontStyle(element);
        g.setFont(fontFor(element, size, style));
        g.setColor(context.paint().color(element.style("color", ""), inheritedColor(element, context)));
        for (UiCssLineBox line : lines) {
            if (line.text().isBlank()) continue;
            int x = Math.round(line.x());
            int baselineY = Math.round(context.viewportHeight() - line.y() - line.height() + line.baseline());
            g.drawString(line.text(), x, baselineY);
        }
    }

    public void drawText(Graphics2D g, UiDomElement element, String text, Rectangle r, boolean center, Context context) {
        int size = Math.max(8, Math.round(context.paint().length(element.style("font-size", "15px"), 15)));
        int style = fontStyle(element);
        Color color = context.paint().color(element.style("color", ""), inheritedColor(element, context));
        if (center || "center".equals(element.style("text-align", ""))) centerText(g, text, r, size, style, color, fontFor(element, size, style));
        else drawPlain(g, text, r.x + 8, r.y + Math.max(size + 6, (r.height + size) / 2), size, style, color, r.width - 16, fontFor(element, size, style));
    }

    public void drawPlain(Graphics2D g, String text, int x, int y, int size, int style, Color color, int maxWidth) {
        drawPlain(g, text, x, y, size, style, color, maxWidth, fontFor(size, style));
    }

    public void drawPlain(Graphics2D g, String text, int x, int y, int size, int style, Color color, int maxWidth, Font font) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        int lineY = y;
        for (String line : wrap(text == null ? "" : text, fm, Math.max(10, maxWidth))) {
            g.drawString(line, x, lineY);
            lineY += fm.getHeight() + 2;
        }
    }

    public void centerText(Graphics2D g, String text, Rectangle r, int size, int style, Color color) {
        centerText(g, text, r, size, style, color, fontFor(size, style));
    }

    public void centerText(Graphics2D g, String text, Rectangle r, int size, int style, Color color, Font font) {
        String value = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        int x = r.x + Math.max(0, (r.width - fm.stringWidth(value)) / 2);
        int y = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(value, x, y);
    }

    public void drawIcon(Graphics2D g, UiDomElement element, String glyph, Rectangle r, Context context) {
        int size = Math.max(8, Math.round(context.paint().length(element.style("font-size", "18px"), 18)));
        FontAwesomeStyle style = FontAwesomeIcons.style(element.classList().values());
        Font font = HtmlDomFonts.registry().font(FontAwesomeFonts.fontId(style) + ", " + style.displayName(), Font.PLAIN, size);
        centerText(g, glyph, r, size, Font.PLAIN, context.paint().color(element.style("color", ""), inheritedColor(element, context)), font);
    }

    public Font fontFor(int size, int style) {
        return HtmlDomFonts.registry().font("", style, Math.max(8, size));
    }

    public Font fontFor(UiDomElement element, int size, int style) {
        return HtmlDomFonts.registry().font(element == null ? "" : element.style("font-family", ""), style, Math.max(8, size));
    }

    public int fontStyle(UiDomElement element) {
        int style = bold(element) ? Font.BOLD : Font.PLAIN;
        String raw = element == null ? "" : element.style("font-style", "").trim().toLowerCase(Locale.ROOT);
        if (raw.equals("italic") || raw.equals("oblique")) style |= Font.ITALIC;
        return style;
    }

    public String directText(UiDomElement element) {
        StringBuilder out = new StringBuilder();
        for (UiDomNode child : element.children()) if (child instanceof UiDomText text) out.append(text.text()).append(' ');
        return out.toString().replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }

    public String firstOptionText(UiDomElement select) {
        for (UiDomNode child : select.children()) if (child instanceof UiDomElement element && "option".equals(element.tagName())) return element.textContent().trim();
        return "";
    }

    public Color inheritedColor(UiDomElement element, Context context) {
        UiDomElement current = element;
        while (current != null) {
            Color color = context.paint().color(current.style("color", ""), null);
            if (color != null) return color;
            current = current.parent();
        }
        return Color.WHITE;
    }

    private void paintInlineBlockRun(Graphics2D g, UiDomElement element, UiCssInlineBox run, Context context) {
        int x = Math.round(run.x());
        int y = Math.round(context.viewportHeight() - run.y() - run.height());
        Rectangle outer = new Rectangle(x, y, Math.round(run.width()), Math.round(run.height()));
        CssInsets margin = cssInsets(element, "margin", context.paint());
        Rectangle r = new Rectangle(
                outer.x + margin.left,
                outer.y + margin.top,
                Math.max(0, outer.width - margin.left - margin.right),
                Math.max(0, outer.height - margin.top - margin.bottom)
        );
        context.paint().paintBackground(g, element, r);
        context.paint().paintBorder(g, element, r);
        String text = directText(element);
        if (!text.isBlank()) {
            int size = Math.max(8, Math.round(context.paint().length(element.style("font-size", "13px"), Math.max(8f, r.height * 0.6f))));
            centerText(g, text, r, size, fontStyle(element), context.paint().color(element.style("color", ""), inheritedColor(element, context)), fontFor(element, size, fontStyle(element)));
        }
        context.paint().paintOutline(g, element, r, element == context.focusedElement());
    }

    private List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.replaceAll("\\s+", " ").trim().split(" ")) {
            if (word.isBlank()) continue;
            String next = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(next) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(next);
            } else {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out.isEmpty() ? List.of("") : out;
    }

    private boolean bold(UiDomElement element) {
        String weight = element == null ? "" : element.style("font-weight", "").toLowerCase(Locale.ROOT);
        return weight.equals("bold") || weight.equals("700") || weight.equals("800") || weight.equals("900");
    }

    private boolean inlineBoxLike(UiDomElement element) {
        return inlineBlock(element) || inlineBoxMetrics(element);
    }

    private boolean inlineBlock(UiDomElement element) {
        return element != null && "inline-block".equals(element.style("display", "").trim().toLowerCase(Locale.ROOT));
    }

    private boolean inlineBoxMetrics(UiDomElement element) {
        if (element == null) return false;
        return meaningfulBoxValue(firstNonBlank(element.style("width", ""), element.style("height", "")))
                || meaningfulBoxValue(firstNonBlank(element.style("padding", ""), element.style("padding-left", ""), element.style("padding-right", ""), element.style("padding-top", ""), element.style("padding-bottom", "")))
                || meaningfulBoxValue(firstNonBlank(element.style("margin", ""), element.style("margin-left", ""), element.style("margin-right", ""), element.style("margin-top", ""), element.style("margin-bottom", "")))
                || meaningfulBoxValue(firstNonBlank(element.style("border", ""), element.style("border-width", ""), element.style("border-color", "")));
    }

    private boolean meaningfulBoxValue(String raw) {
        if (raw == null) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()
                || value.equals("auto")
                || value.equals("none")
                || value.equals("normal")
                || value.equals("transparent")
                || value.equals("initial")
                || value.equals("inherit")
                || value.equals("unset")) return false;
        for (String token : value.split("\s+")) {
            String part = token.trim();
            if (part.isBlank() || part.equals("none") || part.equals("solid") || part.equals("transparent")) continue;
            try {
                String numeric = part.replaceAll("[a-z%]+$", "");
                if (numeric.isBlank() || numeric.equals("+") || numeric.equals("-")) continue;
                if (Math.abs(Float.parseFloat(numeric)) > 0.0001f) return true;
            } catch (RuntimeException ignored) {
                return true;
            }
        }
        return false;
    }

    private CssInsets cssInsets(UiDomElement element, String property, HtmlDomPaintEngine paint) {
        int all = Math.round(paint.length(element.style(property, ""), 0f));
        int left = Math.round(paint.length(element.style(property + "-left", ""), all));
        int right = Math.round(paint.length(element.style(property + "-right", ""), all));
        int top = Math.round(paint.length(element.style(property + "-top", ""), all));
        int bottom = Math.round(paint.length(element.style(property + "-bottom", ""), all));
        return new CssInsets(Math.max(0, left), Math.max(0, top), Math.max(0, right), Math.max(0, bottom));
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private UiDomElement elementByNodeId(UiDomDocument document, int nodeId) {
        if (document == null || nodeId <= 0) return null;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            if (element.nodeId() == nodeId) return element;
        }
        return null;
    }

    private record CssInsets(int left, int top, int right, int bottom) { }

    public record Context(
            UiDomDocument document,
            UiCssLayoutResult layout,
            HtmlDomPaintEngine paint,
            int viewportHeight,
            UiDomElement focusedElement
    ) { }
}
