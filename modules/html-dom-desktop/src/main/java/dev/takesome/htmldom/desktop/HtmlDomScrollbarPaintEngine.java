package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssScrollBox;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollAxis;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollDrag;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollbarHit;
import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/** Paints native-like HtmlDom scrollbars and owns scrollbar geometry calculations. */
public final class HtmlDomScrollbarPaintEngine {
    private static final int THICKNESS = 8;

    public void paintScrollbars(Graphics2D g, UiDomElement element, Rectangle r, UiCssScrollBox scroll, Context context) {
        if (g == null || element == null || r == null || scroll == null || context == null) return;
        if (!scroll.scrollableX() && !scroll.scrollableY()) return;
        if (scroll.scrollableY()) paintVertical(g, element, r, scroll, context);
        if (scroll.scrollableX()) paintHorizontal(g, element, r, scroll, context);
    }

    public HoverState hoverState(List<ScrollbarHit> hits, int x, int y) {
        if (hits == null) return HoverState.EMPTY;
        for (int i = hits.size() - 1; i >= 0; i--) {
            ScrollbarHit hit = hits.get(i);
            if (hit.thumb().contains(x, y) || hit.track().contains(x, y)) return new HoverState(hit.element(), hit.axis());
        }
        return HoverState.EMPTY;
    }

    public ScrollDrag beginDrag(List<ScrollbarHit> hits, int mouseX, int mouseY) {
        if (hits == null) return null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            ScrollbarHit hit = hits.get(i);
            if (hit.thumb().contains(mouseX, mouseY)) {
                Rectangle thumb = hit.thumb().getBounds();
                int grab = hit.axis() == ScrollAxis.Y ? mouseY - thumb.y : mouseX - thumb.x;
                return new ScrollDrag(hit, Math.max(0, grab));
            }
            if (hit.track().contains(mouseX, mouseY)) {
                Rectangle thumb = hit.thumb().getBounds();
                int grab = hit.axis() == ScrollAxis.Y ? thumb.height / 2 : thumb.width / 2;
                return new ScrollDrag(hit, grab);
            }
        }
        return null;
    }

    public ScrollTarget dragTarget(ScrollbarHit hit, UiCssScrollBox box, int mouseX, int mouseY, int grabOffset) {
        if (hit == null || box == null) return ScrollTarget.UNCHANGED;
        Rectangle track = hit.track().getBounds();
        Rectangle thumb = hit.thumb().getBounds();
        if (hit.axis() == ScrollAxis.Y) {
            int travel = Math.max(1, track.height - thumb.height);
            float ratio = clamp01((mouseY - grabOffset - track.y) / (float) travel);
            float nextY = ratio * Math.max(0f, box.contentHeight() - box.viewportHeight());
            return new ScrollTarget(box.scrollX(), nextY);
        }
        int travel = Math.max(1, track.width - thumb.width);
        float ratio = clamp01((mouseX - grabOffset - track.x) / (float) travel);
        float nextX = ratio * Math.max(0f, box.contentWidth() - box.viewportWidth());
        return new ScrollTarget(nextX, box.scrollY());
    }

    private void paintVertical(Graphics2D g, UiDomElement element, Rectangle r, UiCssScrollBox scroll, Context context) {
        Rectangle trackRect = verticalTrack(r, scroll);
        Rectangle thumbRect = verticalThumb(trackRect, scroll);
        context.hitTest().addScrollbarHit(g, element, ScrollAxis.Y, trackRect, thumbRect);
        g.setColor(trackColor(element, ScrollAxis.Y, context));
        g.fillRoundRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height, THICKNESS, THICKNESS);
        g.setColor(thumbColor(element, ScrollAxis.Y, context));
        g.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, THICKNESS, THICKNESS);
    }

    private void paintHorizontal(Graphics2D g, UiDomElement element, Rectangle r, UiCssScrollBox scroll, Context context) {
        Rectangle trackRect = horizontalTrack(r, scroll);
        Rectangle thumbRect = horizontalThumb(trackRect, scroll);
        context.hitTest().addScrollbarHit(g, element, ScrollAxis.X, trackRect, thumbRect);
        g.setColor(trackColor(element, ScrollAxis.X, context));
        g.fillRoundRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height, THICKNESS, THICKNESS);
        g.setColor(thumbColor(element, ScrollAxis.X, context));
        g.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, THICKNESS, THICKNESS);
    }

    private Rectangle verticalTrack(Rectangle r, UiCssScrollBox scroll) {
        int trackX = r.x + r.width - THICKNESS - 2;
        int trackY = r.y + 2;
        int trackH = Math.max(1, r.height - 4 - (scroll.scrollableX() ? THICKNESS : 0));
        return new Rectangle(trackX, trackY, THICKNESS, trackH);
    }

    private Rectangle verticalThumb(Rectangle track, UiCssScrollBox scroll) {
        int thumbH = Math.max(18, Math.round(track.height * (scroll.viewportHeight() / Math.max(1f, scroll.contentHeight()))));
        int maxTravel = Math.max(1, track.height - thumbH);
        int thumbY = track.y + Math.round(maxTravel * (scroll.scrollY() / Math.max(1f, scroll.contentHeight() - scroll.viewportHeight())));
        return new Rectangle(track.x, thumbY, track.width, thumbH);
    }

    private Rectangle horizontalTrack(Rectangle r, UiCssScrollBox scroll) {
        int trackX = r.x + 2;
        int trackY = r.y + r.height - THICKNESS - 2;
        int trackW = Math.max(1, r.width - 4 - (scroll.scrollableY() ? THICKNESS : 0));
        return new Rectangle(trackX, trackY, trackW, THICKNESS);
    }

    private Rectangle horizontalThumb(Rectangle track, UiCssScrollBox scroll) {
        int thumbW = Math.max(18, Math.round(track.width * (scroll.viewportWidth() / Math.max(1f, scroll.contentWidth()))));
        int maxTravel = Math.max(1, track.width - thumbW);
        int thumbX = track.x + Math.round(maxTravel * (scroll.scrollX() / Math.max(1f, scroll.contentWidth() - scroll.viewportWidth())));
        return new Rectangle(thumbX, track.y, thumbW, track.height);
    }

    private Color trackColor(UiDomElement element, ScrollAxis axis, Context context) {
        boolean hover = element == context.hoveredElement() && axis == context.hoveredAxis();
        boolean active = context.activeDrag() != null && context.activeDrag().hit().element() == element && context.activeDrag().hit().axis() == axis;
        if (active) return new Color(15, 23, 42, 230);
        if (hover) return new Color(15, 23, 42, 190);
        return new Color(15, 23, 42, 150);
    }

    private Color thumbColor(UiDomElement element, ScrollAxis axis, Context context) {
        boolean hover = element == context.hoveredElement() && axis == context.hoveredAxis();
        boolean active = context.activeDrag() != null && context.activeDrag().hit().element() == element && context.activeDrag().hit().axis() == axis;
        if (active) return new Color(125, 211, 252, 255);
        if (hover) return new Color(80, 183, 255, 230);
        return new Color(80, 183, 255, 190);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public record Context(
            HtmlDomHitTestEngine hitTest,
            UiDomElement hoveredElement,
            ScrollAxis hoveredAxis,
            ScrollDrag activeDrag
    ) { }

    public record HoverState(UiDomElement element, ScrollAxis axis) {
        public static final HoverState EMPTY = new HoverState(null, null);
    }

    public record ScrollTarget(float x, float y) {
        public static final ScrollTarget UNCHANGED = new ScrollTarget(Float.NaN, Float.NaN);
        public boolean valid() {
            return Float.isFinite(x) && Float.isFinite(y);
        }
    }
}
