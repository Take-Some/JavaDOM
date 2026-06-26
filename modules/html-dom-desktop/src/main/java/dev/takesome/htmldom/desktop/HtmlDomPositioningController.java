package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssBox;
import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.css.UiCssScrollBox;
import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Rectangle;
import java.util.Locale;
import java.util.function.Function;

/** Converts layout boxes into screen-space rectangles with fixed/sticky scroll behavior. */
public final class HtmlDomPositioningController {
    public Rectangle rect(UiDomElement element, UiCssLayoutResult layout, int viewportWidth, int viewportHeight, Function<UiDomElement, UiCssScrollBox> scrollResolver) {
        if (layout == null || element == null) return new Rectangle();
        UiCssBox box = layout.box(element).orElse(null);
        if (box == null) return new Rectangle();
        Rectangle raw = new Rectangle(Math.round(box.x()), Math.round(viewportHeight - box.y() - box.height()), Math.round(box.width()), Math.round(box.height()));
        String pos = positionValue(element);
        if ("fixed".equals(pos)) return fixedRect(element, raw, viewportWidth, viewportHeight, scrollResolver);
        if ("sticky".equals(pos)) return stickyRect(element, raw, layout, scrollResolver, viewportWidth, viewportHeight);
        return raw;
    }

    public boolean positioned(UiDomElement element) {
        String pos = positionValue(element);
        return pos.equals("relative") || pos.equals("absolute") || pos.equals("fixed") || pos.equals("sticky");
    }

    public boolean fixed(UiDomElement element) {
        return "fixed".equals(positionValue(element));
    }

    public UiDomElement nearestScrollContainer(UiDomElement element, Function<UiDomElement, UiCssScrollBox> scrollResolver) {
        UiDomElement current = element == null ? null : element.parent();
        while (current != null) {
            if (scrollResolver.apply(current) != null) return current;
            current = current.parent();
        }
        return null;
    }

    private Rectangle fixedRect(UiDomElement element, Rectangle raw, int viewportWidth, int viewportHeight, Function<UiDomElement, UiCssScrollBox> scrollResolver) {
        float left = length(first(element, "left", "x"), Float.NaN);
        float right = length(element.style("right", ""), Float.NaN);
        float top = length(first(element, "top", "y"), Float.NaN);
        float bottom = length(element.style("bottom", ""), Float.NaN);
        int x = Float.isFinite(left) ? Math.round(left) : Float.isFinite(right) ? Math.round(viewportWidth - raw.width - right) : raw.x;
        int y = Float.isFinite(top) ? Math.round(top) : Float.isFinite(bottom) ? Math.round(viewportHeight - raw.height - bottom) : raw.y;
        ScrollOffset compensation = ancestorScrollOffset(element, scrollResolver);
        return new Rectangle(x + Math.round(compensation.x), y + Math.round(compensation.y), raw.width, raw.height);
    }

    private Rectangle stickyRect(UiDomElement element, Rectangle raw, UiCssLayoutResult layout, Function<UiDomElement, UiCssScrollBox> scrollResolver, int viewportWidth, int viewportHeight) {
        UiDomElement scroller = nearestScrollContainer(element, scrollResolver);
        if (scroller == null) return raw;
        UiCssScrollBox box = scrollResolver.apply(scroller);
        if (box == null) return raw;
        Rectangle viewport = rect(scroller, layout, viewportWidth, viewportHeight, scrollResolver);
        int x = raw.x;
        int y = raw.y;
        float left = length(first(element, "left", "x"), Float.NaN);
        float top = length(first(element, "top", "y"), Float.NaN);
        if (Float.isFinite(left) && box.scrollableX()) {
            int visibleX = raw.x - Math.round(box.scrollX());
            int minX = viewport.x + Math.round(left);
            if (visibleX < minX) x = minX + Math.round(box.scrollX());
        }
        if (Float.isFinite(top) && box.scrollableY()) {
            int visibleY = raw.y - Math.round(box.scrollY());
            int minY = viewport.y + Math.round(top);
            if (visibleY < minY) y = minY + Math.round(box.scrollY());
        }
        return new Rectangle(x, y, raw.width, raw.height);
    }

    private ScrollOffset ancestorScrollOffset(UiDomElement element, Function<UiDomElement, UiCssScrollBox> scrollResolver) {
        float x = 0f;
        float y = 0f;
        UiDomElement current = element == null ? null : element.parent();
        while (current != null) {
            UiCssScrollBox box = scrollResolver.apply(current);
            if (box != null) {
                x += box.scrollX();
                y += box.scrollY();
            }
            current = current.parent();
        }
        return new ScrollOffset(x, y);
    }

    private String positionValue(UiDomElement element) {
        return element == null ? "static" : element.style("position", "static").trim().toLowerCase(Locale.ROOT);
    }

    private String first(UiDomElement element, String... names) {
        for (String name : names) {
            String value = element.style(name, "").trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private float length(String raw, float fallback) {
        if (raw == null || raw.isBlank() || raw.startsWith("var(")) return fallback;
        String value = raw.trim().toLowerCase(Locale.ROOT).replace("px", "");
        int space = value.indexOf(' ');
        if (space > 0) value = value.substring(0, space);
        try { return Float.parseFloat(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private record ScrollOffset(float x, float y) { }
}
