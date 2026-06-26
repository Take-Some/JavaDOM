package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/** Paints form/control elements and registers control hit geometry. */
public final class HtmlDomControlPaintEngine {
    public boolean paintControl(Graphics2D g, UiDomElement element, Rectangle r, Context context) {
        if (g == null || element == null || r == null || context == null) return false;
        return switch (element.tagName()) {
            case "input" -> {
                paintInput(g, element, r, context);
                yield true;
            }
            case "select" -> {
                paintSelect(g, element, r, context);
                yield true;
            }
            case "progress" -> {
                paintProgress(g, element, r, context);
                yield true;
            }
            default -> false;
        };
    }

    public void paintInput(Graphics2D g, UiDomElement element, Rectangle r, Context context) {
        context.hitTest().addHit(g, element, r);
        String value = element.attribute("value", "");
        if (value.isBlank()) value = element.attribute("placeholder", "");
        context.text().drawText(g, element, value, r, false, context.textContext());
        paintControlState(g, element, r, context);
    }

    public void paintSelect(Graphics2D g, UiDomElement element, Rectangle r, Context context) {
        context.hitTest().addHit(g, element, r);
        context.text().drawText(g, element, firstOptionText(element), r, false, context.textContext());
        paintSelectArrow(g, element, r, context);
        paintControlState(g, element, r, context);
    }

    public void paintProgress(Graphics2D g, UiDomElement element, Rectangle r, Context context) {
        float max = context.paint().number(element.attribute("max", "100"), 100f);
        float value = context.paint().number(element.attribute("value", "0"), 0f);
        float ratio = max <= 0f ? 0f : Math.max(0f, Math.min(1f, value / max));
        int radius = Math.max(6, r.height / 2);
        Color track = context.paint().color(element.style("background-color", ""), context.paint().color("#0c1524", Color.DARK_GRAY));
        Color fill = context.paint().color(element.style("accent-color", ""), context.paint().color("#50b7ff", Color.BLUE));
        g.setColor(track);
        g.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, radius, radius));
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Float(r.x, r.y, Math.round(r.width * ratio), r.height, radius, radius));
    }

    private void paintSelectArrow(Graphics2D g, UiDomElement element, Rectangle r, Context context) {
        int size = Math.max(8, Math.round(context.paint().length(element.style("font-size", "14px"), 14)));
        Color color = context.paint().color(element.style("color", ""), context.text().inheritedColor(element, context.textContext()));
        g.setFont(context.text().fontFor(size, Font.BOLD));
        g.setColor(color);
        g.drawString("▾", r.x + Math.max(6, r.width - 22), r.y + (r.height - size) / 2 + size);
    }

    private void paintControlState(Graphics2D g, UiDomElement element, Rectangle r, Context context) {
        boolean focused = element == context.focusedElement();
        if (!focused) return;
        int radius = Math.round(context.paint().length(context.paint().first(element, "border-radius", "border-top-left-radius"), Math.min(r.width, r.height) / 4f));
        g.setColor(context.paint().color("#50b7ff", Color.CYAN));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(r.x + 2, r.y + 2, Math.max(0, r.width - 5), Math.max(0, r.height - 5), radius, radius));
    }

    private String firstOptionText(UiDomElement select) {
        for (UiDomNode child : select.children()) {
            if (child instanceof UiDomElement element && "option".equals(element.tagName())) return element.textContent().trim();
        }
        return "";
    }

    public record Context(
            HtmlDomPaintEngine paint,
            HtmlDomTextPaintEngine text,
            HtmlDomTextPaintEngine.Context textContext,
            HtmlDomHitTestEngine hitTest,
            UiDomElement focusedElement
    ) { }
}
