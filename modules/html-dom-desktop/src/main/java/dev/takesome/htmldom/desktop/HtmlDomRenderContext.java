package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollAxis;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollDrag;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;

/** Shared render-time object that produces specialized subsystem contexts. */
public record HtmlDomRenderContext(
        UiDomDocument document,
        UiCssLayoutResult layout,
        HtmlDomPaintEngine paintEngine,
        HtmlDomTextPaintEngine textPaintEngine,
        HtmlDomControlPaintEngine controlPaintEngine,
        HtmlDomOverlayPaintEngine overlayPaintEngine,
        HtmlDomScrollbarPaintEngine scrollbarPaintEngine,
        HtmlDomHitTestEngine hitTestEngine,
        UiDomElement focusedElement,
        UiDomElement hoveredScrollElement,
        ScrollAxis hoveredScrollAxis,
        ScrollDrag activeScrollDrag,
        int viewportWidth,
        int viewportHeight
) {
    public HtmlDomTextPaintEngine.Context text() {
        return new HtmlDomTextPaintEngine.Context(document, layout, paintEngine, viewportHeight, focusedElement);
    }

    public HtmlDomControlPaintEngine.Context controls() {
        return new HtmlDomControlPaintEngine.Context(paintEngine, textPaintEngine, text(), hitTestEngine, focusedElement);
    }

    public HtmlDomOverlayPaintEngine.Context overlays() {
        return new HtmlDomOverlayPaintEngine.Context(document, paintEngine, textPaintEngine, hitTestEngine, viewportWidth, viewportHeight);
    }

    public HtmlDomScrollbarPaintEngine.Context scrollbars() {
        return new HtmlDomScrollbarPaintEngine.Context(hitTestEngine, hoveredScrollElement, hoveredScrollAxis, activeScrollDrag);
    }
}
