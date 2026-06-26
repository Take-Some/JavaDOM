package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.css.UiCssScrollBox;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Runtime scroll offsets keyed by DOM node id. This is not stored in DOM attributes. */
public final class UiScrollState {
    private final HashMap<Integer, Offset> offsets = new HashMap<>();

    public Offset offset(int nodeId) {
        return offsets.getOrDefault(nodeId, Offset.ZERO);
    }

    public void set(int nodeId, float x, float y, UiCssScrollBox box) {
        if (nodeId <= 0 || box == null) return;
        float maxX = Math.max(0f, box.contentWidth() - box.viewportWidth());
        float maxY = Math.max(0f, box.contentHeight() - box.viewportHeight());
        offsets.put(nodeId, new Offset(clamp(x, 0f, maxX), clamp(y, 0f, maxY)));
    }

    public boolean scroll(int nodeId, UiCssScrollBox box, float dx, float dy) {
        if (nodeId <= 0 || box == null) return false;
        Offset old = offset(nodeId);
        float nextX = old.x + dx;
        float nextY = old.y + dy;
        set(nodeId, nextX, nextY, box);
        return !offset(nodeId).equals(old);
    }

    public void sync(UiCssLayoutResult layout) {
        if (layout == null) {
            offsets.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Offset>> iterator = offsets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Offset> entry = iterator.next();
            UiCssScrollBox box = layout.scrollBoxes().get(entry.getKey());
            if (box == null) {
                iterator.remove();
                continue;
            }
            set(entry.getKey(), entry.getValue().x, entry.getValue().y, box);
        }
        for (UiCssScrollBox box : layout.scrollBoxes().values()) {
            offsets.putIfAbsent(box.nodeId(), new Offset(box.scrollX(), box.scrollY()));
            set(box.nodeId(), offset(box.nodeId()).x, offset(box.nodeId()).y, box);
        }
    }

    public Map<Integer, Offset> snapshot() {
        return Map.copyOf(offsets);
    }

    private static float clamp(float value, float min, float max) {
        float safe = Float.isFinite(value) ? value : 0f;
        return Math.max(min, Math.min(max, safe));
    }

    public record Offset(float x, float y) {
        public static final Offset ZERO = new Offset(0f, 0f);
    }
}
