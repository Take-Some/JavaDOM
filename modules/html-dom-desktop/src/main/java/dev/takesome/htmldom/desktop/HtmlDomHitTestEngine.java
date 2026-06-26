package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.devtools.HtmlDomDevToolsHitNode;
import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Owns screen-space hit targets, clip stack and scrollbar hit geometry. */
public final class HtmlDomHitTestEngine {
    private final ArrayList<Hit> hits = new ArrayList<>();
    private final ArrayList<Hit> scrollHits = new ArrayList<>();
    private final ArrayList<ScrollbarHit> scrollbarHits = new ArrayList<>();
    private final ArrayList<Shape> clips = new ArrayList<>();

    public void clear() {
        hits.clear();
        scrollHits.clear();
        scrollbarHits.clear();
        clips.clear();
    }

    public List<Hit> hits() {
        return hits;
    }

    public List<Hit> scrollHits() {
        return scrollHits;
    }

    public List<ScrollbarHit> scrollbarHits() {
        return scrollbarHits;
    }

    public void pushClip(Shape clip) {
        if (clip != null) clips.add(clip);
    }

    public void popClip(Shape clip) {
        if (clip != null && !clips.isEmpty()) clips.remove(clips.size() - 1);
    }

    public void addHit(Graphics2D g, UiDomElement element, Shape localShape) {
        addHit(hits, g, element, localShape);
    }

    public void addScrollHit(Graphics2D g, UiDomElement element, Shape localShape) {
        addHit(scrollHits, g, element, localShape);
    }

    public void addScrollbarHit(Graphics2D g, UiDomElement element, ScrollAxis axis, Rectangle track, Rectangle thumb) {
        if (pointerEventsNone(element)) return;
        Shape screenTrack = g.getTransform().createTransformedShape(track);
        Shape screenThumb = g.getTransform().createTransformedShape(thumb);
        if (!clips.isEmpty()) {
            Area trackArea = new Area(screenTrack);
            Area thumbArea = new Area(screenThumb);
            for (Shape clip : clips) {
                trackArea.intersect(new Area(clip));
                thumbArea.intersect(new Area(clip));
            }
            if (trackArea.isEmpty() || thumbArea.isEmpty()) return;
            screenTrack = trackArea;
            screenThumb = thumbArea;
        }
        scrollbarHits.add(new ScrollbarHit(element, axis, screenTrack, screenThumb));
    }

    public List<HtmlDomDevToolsHitNode> devToolsHitTargets(Function<UiDomElement, String> selector) {
        ArrayList<HtmlDomDevToolsHitNode> out = new ArrayList<>();
        appendHitTargets(out, hits, selector);
        appendHitTargets(out, scrollHits, selector);
        return List.copyOf(out);
    }

    private void addHit(ArrayList<Hit> target, Graphics2D g, UiDomElement element, Shape localShape) {
        if (element == null || localShape == null || pointerEventsNone(element)) return;
        Shape screenShape = g.getTransform().createTransformedShape(localShape);
        boolean clipped = false;
        if (!clips.isEmpty()) {
            Area area = new Area(screenShape);
            for (Shape clip : clips) area.intersect(new Area(clip));
            if (area.isEmpty()) return;
            screenShape = area;
            clipped = true;
        }
        Rectangle bounds = screenShape.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) return;
        target.add(new Hit(element, screenShape, transformed(g), clipped));
    }

    private void appendHitTargets(ArrayList<HtmlDomDevToolsHitNode> out, ArrayList<Hit> source, Function<UiDomElement, String> selector) {
        for (Hit hit : source) {
            Rectangle b = hit.bounds();
            out.add(new HtmlDomDevToolsHitNode(hit.element.nodeId(), selector.apply(hit.element), b.x, b.y, b.width, b.height, hit.scrollAdjusted, hit.clipped));
        }
    }

    private boolean pointerEventsNone(UiDomElement element) {
        return element != null && "none".equals(element.style("pointer-events", "").trim().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean transformed(Graphics2D g) {
        return Math.abs(g.getTransform().getTranslateX()) > 0.01 || Math.abs(g.getTransform().getTranslateY()) > 0.01
                || Math.abs(g.getTransform().getScaleX() - 1.0) > 0.01 || Math.abs(g.getTransform().getScaleY() - 1.0) > 0.01;
    }

    public record Hit(UiDomElement element, Shape shape, boolean scrollAdjusted, boolean clipped) {
        public boolean contains(int x, int y) {
            return shape != null && shape.contains(x, y);
        }

        public Rectangle bounds() {
            return shape == null ? new Rectangle() : shape.getBounds();
        }
    }

    public enum ScrollAxis { X, Y }

    public record ScrollbarHit(UiDomElement element, ScrollAxis axis, Shape track, Shape thumb) { }

    public record ScrollDrag(ScrollbarHit hit, int grabOffset) { }
}
