package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.properties.layout.BottomCssProperty;
import dev.takesome.htmldom.css.properties.layout.BoundsCssProperty;
import dev.takesome.htmldom.css.properties.layout.HeightCssProperty;
import dev.takesome.htmldom.css.properties.layout.LeftCssProperty;
import dev.takesome.htmldom.css.properties.layout.PositionCssProperty;
import dev.takesome.htmldom.css.properties.layout.RightCssProperty;
import dev.takesome.htmldom.css.properties.layout.TopCssProperty;
import dev.takesome.htmldom.css.properties.layout.WidthCssProperty;
import dev.takesome.htmldom.css.properties.layout.XCssProperty;
import dev.takesome.htmldom.css.properties.layout.YCssProperty;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;
import dev.takesome.htmldom.dom.UiDomElement;

import java.util.Locale;

/** Resolves explicit, relative, and out-of-flow CSS positioning. */
final class UiCssPositioningEngine {
    private final UiCssUnitResolutionContext lengthContext;
    private final float viewportWidth;
    private final float viewportHeight;
    private final PositionCssProperty position;
    private final BoundsCssProperty bounds;
    private final XCssProperty x;
    private final YCssProperty y;
    private final LeftCssProperty left;
    private final TopCssProperty top;
    private final RightCssProperty right;
    private final BottomCssProperty bottom;
    private final WidthCssProperty width;
    private final HeightCssProperty height;
    private final SizingResolver sizing;
    private final WarnSink warnSink;

    UiCssPositioningEngine(
            UiCssUnitResolutionContext lengthContext,
            float viewportWidth,
            float viewportHeight,
            PositionCssProperty position,
            BoundsCssProperty bounds,
            XCssProperty x,
            YCssProperty y,
            LeftCssProperty left,
            TopCssProperty top,
            RightCssProperty right,
            BottomCssProperty bottom,
            WidthCssProperty width,
            HeightCssProperty height,
            SizingResolver sizing,
            WarnSink warnSink
    ) {
        this.lengthContext = lengthContext == null ? UiCssUnitResolutionContext.defaults() : lengthContext;
        this.viewportWidth = Math.max(0f, viewportWidth);
        this.viewportHeight = Math.max(0f, viewportHeight);
        this.position = position;
        this.bounds = bounds;
        this.x = x;
        this.y = y;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.width = width;
        this.height = height;
        this.sizing = sizing;
        this.warnSink = warnSink == null ? (key, message, args) -> { } : warnSink;
    }

    UiCssBox resolveInitialBox(UiDomElement element, float width, float height) {
        return resolveExplicitBox(element, null, 0f, 0f, Math.max(0f, width), Math.max(0f, height));
    }

    UiCssBox resolveOutOfFlowBox(UiDomElement element, UiCssBox parentBox) {
        String pos = positionValue(element);
        if ("fixed".equals(pos)) return resolveExplicitBox(element, null, 0f, 0f, viewportWidth, viewportHeight);
        UiCssBox containing = containingBlockFor(element, parentBox);
        return resolveExplicitBox(element, containing, containing.x(), containing.y(), containing.width(), containing.height());
    }

    UiCssBox relativeOffset(UiDomElement element, UiCssBox flowBox, float referenceW, float referenceH) {
        if (element == null || flowBox == null || !position.read(element).relative()) return flowBox;
        float dx = explicitOffset(element, Axis.X, flowBox.width(), referenceW, UiCssLength.ZERO);
        float dy = explicitOffset(element, Axis.Y, flowBox.height(), referenceH, UiCssLength.ZERO);
        return new UiCssBox(flowBox.x() + dx, flowBox.y() + dy, flowBox.width(), flowBox.height());
    }

    boolean outOfFlow(UiDomElement element) {
        return element != null && (position.read(element).outOfFlow() || bounds.read(element).isPresent() || hasExplicitPosition(element));
    }

    private UiCssBox containingBlockFor(UiDomElement element, UiCssBox fallback) {
        UiDomElement current = element == null ? null : element.parent();
        while (current != null) {
            if (positionedForContainingBlock(current)) {
                float x0 = length(current.style("layout-x", ""), fallback == null ? 0f : fallback.x());
                float y0 = length(current.style("layout-y", ""), fallback == null ? 0f : fallback.y());
                float w = length(current.style("width", ""), fallback == null ? viewportWidth : fallback.width());
                float h = length(current.style("height", ""), fallback == null ? viewportHeight : fallback.height());
                return new UiCssBox(x0, y0, Math.max(0f, w), Math.max(0f, h));
            }
            current = current.parent();
        }
        if (fallback != null) return fallback;
        return new UiCssBox(0f, 0f, viewportWidth, viewportHeight);
    }

    private boolean positionedForContainingBlock(UiDomElement element) {
        String pos = positionValue(element);
        if (pos.equals("relative") || pos.equals("absolute") || pos.equals("fixed") || pos.equals("sticky")) return true;
        String transform = element.style("transform", "").trim();
        return !transform.isBlank() && !"none".equalsIgnoreCase(transform);
    }

    private String positionValue(UiDomElement element) {
        return element == null ? "static" : element.style("position", "static").trim().toLowerCase(Locale.ROOT);
    }

    private float length(String raw, float fallback) {
        if (raw == null || raw.isBlank() || raw.startsWith("var(")) return fallback;
        String value = raw.trim().toLowerCase(Locale.ROOT).replace("px", "");
        int space = value.indexOf(' ');
        if (space > 0) value = value.substring(0, space);
        try { return Float.parseFloat(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private UiCssBox resolveExplicitBox(UiDomElement element, UiCssBox parentBox, float originX, float originY, float referenceW, float referenceH) {
        UiCssBounds explicitBounds = bounds.read(element).orElse(null);
        String rawWidth = explicitBounds == null ? width.raw(element) : "";
        String rawHeight = explicitBounds == null ? height.raw(element) : "";
        boolean intrinsicWidth = explicitBounds == null && sizing.intrinsicWidthRequested(element, rawWidth);
        boolean intrinsicHeight = explicitBounds == null && sizing.intrinsicHeightRequested(rawHeight);
        UiCssLength wLength = explicitBounds == null ? sizing.safeLength(rawWidth, UiCssLength.AUTO, intrinsicWidth) : explicitBounds.width();
        UiCssLength hLength = explicitBounds == null ? sizing.safeLength(rawHeight, UiCssLength.AUTO, intrinsicHeight) : explicitBounds.height();
        float fallbackW = parentBox == null ? referenceW : (intrinsicWidth ? sizing.intrinsicWidth(element, referenceW, 0f) : 0f);
        float fallbackH = parentBox == null ? referenceH : (intrinsicHeight ? sizing.intrinsicHeight(element, referenceW, referenceH, 0f) : 0f);
        float w = sizing.clampWidth(element, sizing.resolveBoxSizedWidth(element, wLength.resolve(lengthContext, referenceW, fallbackW), referenceW, explicitBounds != null || (!rawWidth.isBlank() && !intrinsicWidth)), referenceW);
        float h = sizing.clampHeight(element, sizing.resolveBoxSizedHeight(element, hLength.resolve(lengthContext, referenceH, fallbackH), referenceH, explicitBounds != null || (!rawHeight.isBlank() && !intrinsicHeight)), referenceH);
        float x0 = originX + resolveExplicitOffset(element, explicitBounds, Axis.X, w, referenceW);
        float y0 = resolveExplicitY(element, explicitBounds, originY, referenceH, h);
        return new UiCssBox(x0, y0, w, h);
    }

    private float resolveExplicitY(UiDomElement element, UiCssBounds explicitBounds, float originY, float referenceH, float height) {
        if (explicitBounds != null) {
            return originY + explicitBounds.y().resolve(lengthContext, referenceH, 0f);
        }
        String raw = y.raw(element);
        if (!raw.isBlank()) {
            return originY + referenceH - height - offset(element, Axis.Y, raw, height, referenceH);
        }
        raw = top.raw(element);
        if (!raw.isBlank()) {
            return originY + referenceH - height - offset(element, Axis.Y, raw, height, referenceH);
        }
        raw = bottom.raw(element);
        if (!raw.isBlank()) {
            return originY + sizing.resolveLength(element, bottom.name(), raw, referenceH, 0f);
        }
        return originY;
    }

    private float resolveExplicitOffset(UiDomElement element, UiCssBounds explicitBounds, Axis axis, float size, float reference) {
        if (explicitBounds != null) {
            UiCssLength resolved = axis == Axis.X ? explicitBounds.x() : explicitBounds.y();
            return resolved.resolve(lengthContext, reference, 0f);
        }
        return explicitOffset(element, axis, size, reference, UiCssLength.AUTO);
    }

    private float explicitOffset(UiDomElement element, Axis axis, float size, float reference, UiCssLength fallback) {
        String raw = primary(axis).raw(element);
        if (!raw.isBlank()) return offset(element, axis, raw, size, reference);
        raw = startProperty(axis).raw(element);
        if (!raw.isBlank()) return offset(element, axis, raw, size, reference);
        raw = endProperty(axis).raw(element);
        if (!raw.isBlank()) return Math.max(0f, reference - size - sizing.resolveLength(element, endProperty(axis).name(), raw, reference, 0f));
        return fallback.resolve(lengthContext, reference, 0f);
    }

    private float offset(UiDomElement element, Axis axis, String raw, float size, float reference) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (axis.start(value)) return 0f;
        if (axis.end(value)) return reference - size;
        if (Axis.CENTER.matches(value)) return (reference - size) * 0.5f;
        try {
            return sizing.resolveLength(element, primary(axis).name(), value, reference, 0f);
        } catch (RuntimeException ex) {
            warnSink.warn("offset|" + raw, "UI CSS layout invalid offset raw='{}' axis={} reference={} size={} reason='{}'", raw, axis, reference, size, ex.getMessage());
            return 0f;
        }
    }

    private boolean hasExplicitPosition(UiDomElement element) {
        return !x.raw(element).isBlank() || !y.raw(element).isBlank() || !left.raw(element).isBlank() || !top.raw(element).isBlank() || !right.raw(element).isBlank() || !bottom.raw(element).isBlank();
    }

    private UiCssBasePropertySpec primary(Axis axis) { return axis == Axis.X ? x : y; }
    private UiCssBasePropertySpec startProperty(Axis axis) { return axis == Axis.X ? left : top; }
    private UiCssBasePropertySpec endProperty(Axis axis) { return axis == Axis.X ? right : bottom; }

    interface SizingResolver {
        UiCssLength safeLength(String raw, UiCssLength fallback, boolean intrinsic);
        boolean intrinsicWidthRequested(UiDomElement element, String rawWidth);
        boolean intrinsicHeightRequested(String rawHeight);
        float intrinsicWidth(UiDomElement element, float reference, float fallback);
        float intrinsicHeight(UiDomElement element, float referenceW, float referenceH, float fallback);
        float resolveBoxSizedWidth(UiDomElement element, float value, float reference, boolean explicit);
        float resolveBoxSizedHeight(UiDomElement element, float value, float reference, boolean explicit);
        float clampWidth(UiDomElement element, float value, float reference);
        float clampHeight(UiDomElement element, float value, float reference);
        float resolveLength(UiDomElement element, String property, String raw, float reference, float fallback);
    }

    @FunctionalInterface
    interface WarnSink {
        void warn(String key, String message, Object... args);
    }

    private enum Axis {
        X, Y;
        static final CenterKeywords CENTER = new CenterKeywords();
        boolean start(String value) { return this == X ? "left".equals(value) || "start".equals(value) : "top".equals(value) || "start".equals(value); }
        boolean end(String value) { return this == X ? "right".equals(value) || "end".equals(value) : "bottom".equals(value) || "end".equals(value); }
    }

    private static final class CenterKeywords {
        boolean matches(String value) { return "center".equals(value) || "middle".equals(value); }
    }
}
