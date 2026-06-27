package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;

/** Resolves scroll extents and scroll container policies after layout boxes are committed. */
final class UiCssScrollExtentEngine {
    private final UiCssUnitResolutionContext lengthContext;

    UiCssScrollExtentEngine(UiCssUnitResolutionContext lengthContext) {
        this.lengthContext = lengthContext == null ? UiCssUnitResolutionContext.defaults() : lengthContext;
    }

    void commitScrollContainers(UiDomElement root, UiCssLayoutResult result) {
        commitScrollBox(root, result);
        for (UiDomNode node : root.children()) {
            if (node instanceof UiDomElement child) commitScrollContainers(child, result);
        }
    }

    private void commitScrollBox(UiDomElement element, UiCssLayoutResult result) {
        UiCssBox viewport = result.box(element).orElse(null);
        if (viewport == null) return;
        ContentExtent extent = contentExtent(element, viewport, result);
        ScrollPolicy policy = scrollPolicy(element, viewport, extent);
        if (!policy.scrollX && !policy.scrollY) return;
        float scrollX = scrollOffset(element, Axis.X, Math.max(0f, extent.width - viewport.width()));
        float scrollY = scrollOffset(element, Axis.Y, Math.max(0f, extent.height - viewport.height()));
        result.putScrollBox(element, new UiCssScrollBox(
                element.nodeId(), viewport.width(), viewport.height(), extent.width, extent.height(), scrollX, scrollY,
                policy.scrollX, policy.scrollY, policy.clipX, policy.clipY
        ));
    }

    private ContentExtent contentExtent(UiDomElement element, UiCssBox viewport, UiCssLayoutResult result) {
        float right = viewport.x() + viewport.width();
        float top = viewport.y() + viewport.height();
        for (UiDomNode node : element.children()) {
            if (node instanceof UiDomElement child) {
                UiCssBox childBox = result.box(child).orElse(null);
                if (childBox != null) {
                    right = Math.max(right, childBox.x() + childBox.width());
                    top = Math.max(top, childBox.y() + childBox.height());
                }
            }
        }
        for (UiCssLineBox line : result.lineBoxes(element)) {
            right = Math.max(right, line.x() + line.width());
            top = Math.max(top, line.y() + line.height());
        }
        for (UiCssInlineBox run : result.inlineBoxes(element)) {
            right = Math.max(right, run.x() + run.width());
            top = Math.max(top, run.y() + run.height());
        }
        return new ContentExtent(Math.max(viewport.width(), right - viewport.x()), Math.max(viewport.height(), top - viewport.y()));
    }

    private ScrollPolicy scrollPolicy(UiDomElement element, UiCssBox viewport, ContentExtent extent) {
        String xMode = overflowMode(element, Axis.X);
        String yMode = overflowMode(element, Axis.Y);
        boolean overflowX = extent.width > viewport.width() + 0.5f;
        boolean overflowY = extent.height > viewport.height() + 0.5f;
        boolean rootScroller = rootScroller(element);
        boolean scrollX = "scroll".equals(xMode) || ("auto".equals(xMode) && overflowX) || (rootScroller && "visible".equals(xMode) && overflowX);
        boolean scrollY = "scroll".equals(yMode) || ("auto".equals(yMode) && overflowY) || (rootScroller && "visible".equals(yMode) && overflowY);
        boolean clipX = scrollX || "hidden".equals(xMode) || "clip".equals(xMode) || "auto".equals(xMode) || "scroll".equals(xMode);
        boolean clipY = scrollY || "hidden".equals(yMode) || "clip".equals(yMode) || "auto".equals(yMode) || "scroll".equals(yMode);
        return new ScrollPolicy(scrollX, scrollY, clipX, clipY);
    }

    private String overflowMode(UiDomElement element, Axis axis) {
        String axisValue = axis == Axis.X ? firstStyle(element, "overflow-x") : firstStyle(element, "overflow-y");
        String value = axisValue.isBlank() ? firstStyle(element, "overflow") : axisValue;
        String normalized = value.isBlank() ? "visible" : value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "hidden", "clip", "auto", "scroll" -> normalized;
            default -> "visible";
        };
    }

    private boolean rootScroller(UiDomElement element) {
        return element.parent() == null || "html".equals(element.tagName()) || "body".equals(element.tagName());
    }

    private float scrollOffset(UiDomElement element, Axis axis, float max) {
        if (max <= 0f) return 0f;
        String raw = axis == Axis.X ? firstStyle(element, "scroll-left", "scroll-x") : firstStyle(element, "scroll-top", "scroll-y");
        if (raw.isBlank()) return 0f;
        try {
            float value = UiCssLength.parse(raw).resolve(lengthContext, max, 0f);
            return Math.max(0f, Math.min(max, value));
        } catch (RuntimeException ignored) {
            try { return Math.max(0f, Math.min(max, Float.parseFloat(raw.trim()))); }
            catch (RuntimeException ignoredAgain) { return 0f; }
        }
    }

    private String firstStyle(UiDomElement element, String... names) {
        for (String name : names) {
            String value = element.style(name, "");
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private enum Axis { X, Y }

    private record ContentExtent(float width, float height) { }

    private record ScrollPolicy(boolean scrollX, boolean scrollY, boolean clipX, boolean clipY) { }
}
