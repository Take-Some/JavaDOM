package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.css.UiCssScrollBox;
import dev.takesome.htmldom.dom.UiDomElement;

import java.util.LinkedHashMap;
import java.util.Map;

/** Desktop runtime scroll controller. Owns scroll offsets outside DOM attributes. */
public final class HtmlDomScrollController {
    private final UiScrollState state = new UiScrollState();

    public UiScrollState state() {
        return state;
    }

    public void sync(UiCssLayoutResult layout) {
        state.sync(layout);
    }

    public UiCssScrollBox resolve(UiDomElement element, UiCssScrollBox box) {
        if (element == null || box == null) return box;
        UiScrollState.Offset offset = state.offset(element.nodeId());
        return withOffset(box, offset.x(), offset.y());
    }

    public boolean scroll(UiDomElement element, UiCssScrollBox box, float dx, float dy) {
        if (element == null || box == null) return false;
        return state.scroll(element.nodeId(), box, dx, dy);
    }

    public boolean set(UiDomElement element, UiCssScrollBox box, float x, float y) {
        if (element == null || box == null) return false;
        UiScrollState.Offset before = state.offset(element.nodeId());
        state.set(element.nodeId(), x, y, box);
        return !before.equals(state.offset(element.nodeId()));
    }

    public Map<Integer, UiCssScrollBox> resolvedScrollBoxes(UiCssLayoutResult layout) {
        LinkedHashMap<Integer, UiCssScrollBox> out = new LinkedHashMap<>();
        if (layout == null) return out;
        for (UiCssScrollBox box : layout.scrollBoxes().values()) {
            UiScrollState.Offset offset = state.offset(box.nodeId());
            out.put(box.nodeId(), withOffset(box, offset.x(), offset.y()));
        }
        return Map.copyOf(out);
    }

    private UiCssScrollBox withOffset(UiCssScrollBox box, float x, float y) {
        return new UiCssScrollBox(
                box.nodeId(), box.viewportWidth(), box.viewportHeight(), box.contentWidth(), box.contentHeight(),
                x, y, box.scrollXEnabled(), box.scrollYEnabled(), box.clipX(), box.clipY()
        );
    }
}
