package dev.takesome.htmldom.css;

import java.util.List;

/** A paint-tree node derived from DOM + computed style + layout. */
public record UiCssPaintNode(
        int nodeId,
        int parentNodeId,
        int depth,
        int order,
        String tagName,
        String id,
        String classes,
        float x,
        float y,
        float width,
        float height,
        int zIndex,
        boolean zIndexAuto,
        boolean positioned,
        boolean stackingContext,
        boolean scrollContainer,
        float opacity,
        List<UiCssPaintPhase> phases
) {
    public UiCssPaintNode {
        tagName = tagName == null ? "" : tagName;
        id = id == null ? "" : id;
        classes = classes == null ? "" : classes;
        x = finite(x);
        y = finite(y);
        width = finiteNonNegative(width);
        height = finiteNonNegative(height);
        opacity = Float.isFinite(opacity) ? Math.max(0f, Math.min(1f, opacity)) : 1f;
        phases = phases == null || phases.isEmpty() ? List.of(UiCssPaintPhase.BACKGROUND, UiCssPaintPhase.BORDER, UiCssPaintPhase.CONTENT) : List.copyOf(phases);
    }

    public String selector() {
        return tagName + (id.isBlank() ? "" : "#" + id) + (classes.isBlank() ? "" : "." + classes.replace(' ', '.'));
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0f;
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
