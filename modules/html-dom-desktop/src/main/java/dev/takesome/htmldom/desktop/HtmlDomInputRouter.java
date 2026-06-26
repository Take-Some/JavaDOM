package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.css.UiCssScrollBox;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.Hit;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollAxis;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollDrag;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollbarHit;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/** Routes Swing input events into HtmlDom focus, hit-test, scroll and action systems. */
public final class HtmlDomInputRouter {
    private final Host host;
    private UiDomElement lastPointerElement;

    public HtmlDomInputRouter(Host host) {
        this.host = host;
    }

    public void install(JComponent component) {
        MouseAdapter input = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { handleClick(event.getX(), event.getY()); }
            @Override public void mousePressed(MouseEvent event) { handleMousePressed(event); }
            @Override public void mouseDragged(MouseEvent event) { handleMouseDragged(event); }
            @Override public void mouseMoved(MouseEvent event) { handleMouseMoved(event); }
            @Override public void mouseEntered(MouseEvent event) { handleMouseMoved(event); }
            @Override public void mouseExited(MouseEvent event) { handleMouseExited(); }
            @Override public void mouseReleased(MouseEvent event) { host.setActiveScrollDrag(null); if (host.clearActiveElement()) host.repaintHost(); }
            @Override public void mouseWheelMoved(MouseWheelEvent event) { handleWheel(event); }
        };
        component.addMouseListener(input);
        component.addMouseMotionListener(input);
        component.addMouseWheelListener(input);
        component.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) { handleKey(event); }
        });
    }

    public void scrollIntoView(String elementId, String block, String inline) {
        UiDomElement element = host.document().getElementById(elementId == null ? "" : elementId).orElse(null);
        if (element == null) return;
        host.ensureLayout();
        scrollIntoView(element, block, inline);
        host.repaintHost();
    }

    private UiDomElement topHitElement(int x, int y) {
        for (int i = host.hitTest().hits().size() - 1; i >= 0; i--) {
            Hit hit = host.hitTest().hits().get(i);
            if (hit.contains(x, y)) return hit.element();
        }
        return null;
    }

    private void handleClick(int x, int y) {
        for (int i = host.hitTest().hits().size() - 1; i >= 0; i--) {
            Hit hit = host.hitTest().hits().get(i);
            if (!hit.contains(x, y)) continue;
            UiDomElement element = hit.element();
            if (host.focusController().focusable(element)) setFocusedElement(element, false);
            host.requestPanelFocus();
            host.activate(element);
            host.repaintHost();
            return;
        }
        if (host.dialogOpen()) {
            host.setDialogOpen(false);
            host.repaintHost();
        }
    }

    private void handleWheel(MouseWheelEvent event) {
        UiCssLayoutResult layout = host.layoutResult();
        if (layout == null || host.hitTest().scrollHits().isEmpty()) return;
        for (int i = host.hitTest().scrollHits().size() - 1; i >= 0; i--) {
            Hit hit = host.hitTest().scrollHits().get(i);
            if (!hit.contains(event.getX(), event.getY())) continue;
            UiCssScrollBox box = host.scrollBox(hit.element());
            if (box == null) continue;
            float delta = (float) event.getPreciseWheelRotation() * 48f;
            boolean changed;
            if (event.isShiftDown() && box.scrollableX()) changed = host.scrollController().scroll(hit.element(), box, delta, 0f);
            else if (box.scrollableY()) changed = host.scrollController().scroll(hit.element(), box, 0f, delta);
            else changed = box.scrollableX() && host.scrollController().scroll(hit.element(), box, delta, 0f);
            if (changed) host.repaintHost();
            return;
        }
    }

    private void handleMousePressed(MouseEvent event) {
        UiDomElement active = topHitElement(event.getX(), event.getY());
        boolean repaint = host.setActiveElement(active);
        ScrollDrag drag = host.scrollbarPaintEngine().beginDrag(host.hitTest().scrollbarHits(), event.getX(), event.getY());
        host.setActiveScrollDrag(drag);
        if (drag != null) updateScrollbarDrag(drag.hit(), event.getX(), event.getY(), drag.grabOffset());
        else if (repaint) host.repaintHost();
    }

    private void handleMouseDragged(MouseEvent event) {
        ScrollDrag drag = host.activeScrollDrag();
        if (drag == null) return;
        updateScrollbarDrag(drag.hit(), event.getX(), event.getY(), drag.grabOffset());
    }

    private void updateScrollbarDrag(ScrollbarHit hit, int mouseX, int mouseY, int grabOffset) {
        UiCssScrollBox box = host.scrollBox(hit.element());
        if (box == null) return;
        HtmlDomScrollbarPaintEngine.ScrollTarget target = host.scrollbarPaintEngine().dragTarget(hit, box, mouseX, mouseY, grabOffset);
        if (target.valid() && host.scrollController().set(hit.element(), box, target.x(), target.y())) host.repaintHost();
    }

    private void handleMouseMoved(MouseEvent event) {
        UiDomElement hover = topHitElement(event.getX(), event.getY());
        boolean repaint = updatePointerTarget(hover);
        if (hover != null) {
            host.dispatchEvent("pointermove", hover);
            host.dispatchEvent("mousemove", hover);
        }
        repaint = host.setHoveredElement(hover) || repaint;
        HtmlDomScrollbarPaintEngine.HoverState state = host.scrollbarPaintEngine().hoverState(host.hitTest().scrollbarHits(), event.getX(), event.getY());
        if (state.element() != host.hoveredScrollElement() || state.axis() != host.hoveredScrollAxis()) {
            host.setHoveredScrollbar(state.element(), state.axis());
            repaint = true;
        }
        if (repaint) host.repaintHost();
    }

    private void handleMouseExited() {
        boolean repaint = updatePointerTarget(null);
        repaint = host.setHoveredElement(null) || repaint;
        if (host.hoveredScrollElement() != null || host.hoveredScrollAxis() != null) {
            host.setHoveredScrollbar(null, null);
            repaint = true;
        }
        if (repaint) host.repaintHost();
    }

    private boolean updatePointerTarget(UiDomElement next) {
        UiDomElement previous = lastPointerElement;
        if (previous == next) return false;
        List<UiDomElement> previousPath = elementPath(previous);
        List<UiDomElement> nextPath = elementPath(next);

        if (previous != null) {
            host.dispatchEvent("pointerout", previous);
            host.dispatchEvent("mouseout", previous);
        }
        for (UiDomElement element : previousPath) {
            if (containsElement(nextPath, element)) continue;
            host.dispatchEvent("pointerleave", element);
            host.dispatchEvent("mouseleave", element);
        }

        lastPointerElement = next;

        if (next != null) {
            host.dispatchEvent("pointerover", next);
            host.dispatchEvent("mouseover", next);
        }
        for (int i = nextPath.size() - 1; i >= 0; i--) {
            UiDomElement element = nextPath.get(i);
            if (containsElement(previousPath, element)) continue;
            host.dispatchEvent("pointerenter", element);
            host.dispatchEvent("mouseenter", element);
        }
        return true;
    }

    private List<UiDomElement> elementPath(UiDomElement element) {
        ArrayList<UiDomElement> out = new ArrayList<>();
        UiDomElement current = element;
        while (current != null) {
            out.add(current);
            current = current.parent();
        }
        return out;
    }

    private boolean containsElement(List<UiDomElement> elements, UiDomElement target) {
        if (target == null || elements == null) return false;
        for (UiDomElement element : elements) if (element == target) return true;
        return false;
    }

    private void handleKey(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.VK_F12) {
            host.openDevTools();
            event.consume();
            return;
        }
        if (code == KeyEvent.VK_TAB) {
            focusNext(!event.isShiftDown());
            event.consume();
            return;
        }
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
            UiDomElement focused = host.focusController().focusedElement();
            if (focused != null && host.clickable(focused)) {
                host.activate(focused);
                host.repaintHost();
                event.consume();
                return;
            }
        }
        if (keyboardScroll(code)) event.consume();
    }

    private boolean keyboardScroll(int code) {
        UiDomElement focused = host.focusController().focusedElement();
        UiDomElement root = host.document().body().orElse(host.document().documentElement());
        UiDomElement target = host.nearestScrollContainer(focused == null ? root : focused);
        if (target == null) target = host.firstScrollContainer();
        if (target == null) return false;
        UiCssScrollBox box = host.scrollBox(target);
        if (box == null) return false;
        float line = 42f;
        float page = Math.max(64f, box.viewportHeight() * 0.85f);
        boolean changed = switch (code) {
            case KeyEvent.VK_UP -> host.scrollController().scroll(target, box, 0f, -line);
            case KeyEvent.VK_DOWN -> host.scrollController().scroll(target, box, 0f, line);
            case KeyEvent.VK_LEFT -> host.scrollController().scroll(target, box, -line, 0f);
            case KeyEvent.VK_RIGHT -> host.scrollController().scroll(target, box, line, 0f);
            case KeyEvent.VK_PAGE_UP -> host.scrollController().scroll(target, box, 0f, -page);
            case KeyEvent.VK_PAGE_DOWN -> host.scrollController().scroll(target, box, 0f, page);
            case KeyEvent.VK_HOME -> host.scrollController().set(target, box, box.scrollX(), 0f);
            case KeyEvent.VK_END -> host.scrollController().set(target, box, box.scrollX(), box.contentHeight() - box.viewportHeight());
            default -> false;
        };
        if (changed) host.repaintHost();
        return changed;
    }

    private void focusNext(boolean forward) {
        UiDomElement next = host.focusController().focusNext(host.document(), forward);
        if (next != null) setFocusedElement(next, true);
    }

    private void setFocusedElement(UiDomElement element, boolean shouldScrollIntoView) {
        if (element == null || !host.focusController().focusable(element)) return;
        host.focusController().setFocusedElement(element);
        if (shouldScrollIntoView) scrollIntoView(element, "nearest", "nearest");
        host.repaintHost();
    }

    private void scrollIntoView(UiDomElement element, String block, String inline) {
        UiDomElement scroller = host.nearestScrollContainer(element);
        if (scroller == null) return;
        UiCssScrollBox box = host.scrollBox(scroller);
        if (box == null) return;
        Rectangle viewport = host.rect(scroller);
        Rectangle child = host.rect(element);
        float nextX = scrollTarget(box.scrollX(), visibleStart(child.x, box.scrollX()), child.width, viewport.x, viewport.width, box.contentWidth(), box.viewportWidth(), normalizeScrollAlign(inline));
        float nextY = scrollTarget(box.scrollY(), visibleStart(child.y, box.scrollY()), child.height, viewport.y, viewport.height, box.contentHeight(), box.viewportHeight(), normalizeScrollAlign(block));
        host.scrollController().set(scroller, box, nextX, nextY);
    }

    private int visibleStart(int rawStart, float scroll) {
        return rawStart - Math.round(scroll);
    }

    private float scrollTarget(float current, int visibleStart, int itemSize, int viewportStart, int viewportSize, float contentSize, float viewportContentSize, String align) {
        float max = Math.max(0f, contentSize - viewportContentSize);
        int viewportEnd = viewportStart + viewportSize;
        int itemEnd = visibleStart + itemSize;
        float target = current;
        switch (align) {
            case "start" -> target += visibleStart - viewportStart;
            case "center" -> target += visibleStart + itemSize * 0.5f - (viewportStart + viewportSize * 0.5f);
            case "end" -> target += itemEnd - viewportEnd;
            default -> {
                if (visibleStart < viewportStart) target += visibleStart - viewportStart;
                else if (itemEnd > viewportEnd) target += itemEnd - viewportEnd;
            }
        }
        return Math.max(0f, Math.min(max, target));
    }

    private String normalizeScrollAlign(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (value) {
            case "start", "center", "end" -> value;
            default -> "nearest";
        };
    }

    public interface Host {
        UiDomDocument document();
        UiCssLayoutResult layoutResult();
        void ensureLayout();
        HtmlDomHitTestEngine hitTest();
        HtmlDomScrollController scrollController();
        HtmlDomScrollbarPaintEngine scrollbarPaintEngine();
        HtmlDomFocusController focusController();
        UiCssScrollBox scrollBox(UiDomElement element);
        Rectangle rect(UiDomElement element);
        UiDomElement nearestScrollContainer(UiDomElement element);
        UiDomElement firstScrollContainer();
        boolean clickable(UiDomElement element);
        boolean dialogOpen();
        void setDialogOpen(boolean open);
        void activate(UiDomElement element);
        void repaintHost();
        void requestPanelFocus();
        void dispatchEvent(String type, UiDomElement target);
        void openDevTools();
        ScrollDrag activeScrollDrag();
        void setActiveScrollDrag(ScrollDrag drag);
        UiDomElement hoveredScrollElement();
        ScrollAxis hoveredScrollAxis();
        void setHoveredScrollbar(UiDomElement element, ScrollAxis axis);
        boolean setHoveredElement(UiDomElement element);
        boolean setActiveElement(UiDomElement element);
        boolean clearActiveElement();
    }
}
