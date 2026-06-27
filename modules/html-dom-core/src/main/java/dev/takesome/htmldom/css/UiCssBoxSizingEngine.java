package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.properties.layout.BoxSizingCssProperty;
import dev.takesome.htmldom.css.properties.layout.DisplayCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexDirectionCssProperty;
import dev.takesome.htmldom.css.properties.layout.GapCssProperty;
import dev.takesome.htmldom.css.properties.layout.HeightCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginBottomCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginLeftCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginRightCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginTopCssProperty;
import dev.takesome.htmldom.css.properties.layout.MaxHeightCssProperty;
import dev.takesome.htmldom.css.properties.layout.MaxWidthCssProperty;
import dev.takesome.htmldom.css.properties.layout.MinHeightCssProperty;
import dev.takesome.htmldom.css.properties.layout.MinWidthCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingBottomCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingLeftCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingRightCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingTopCssProperty;
import dev.takesome.htmldom.css.properties.layout.WidthCssProperty;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;

import java.util.Locale;

/** Resolves CSS preferred/intrinsic/used sizes and box-sizing constraints. */
final class UiCssBoxSizingEngine {
    private final UiCssUnitResolutionContext lengthContext;
    private final DisplayCssProperty display;
    private final FlexDirectionCssProperty flexDirection;
    private final GapCssProperty gap;
    private final WidthCssProperty width;
    private final HeightCssProperty height;
    private final MinWidthCssProperty minWidth;
    private final MinHeightCssProperty minHeight;
    private final MaxWidthCssProperty maxWidth;
    private final MaxHeightCssProperty maxHeight;
    private final BoxSizingCssProperty boxSizing;
    private final MarginCssProperty margin;
    private final MarginLeftCssProperty marginLeft;
    private final MarginRightCssProperty marginRight;
    private final MarginTopCssProperty marginTop;
    private final MarginBottomCssProperty marginBottom;
    private final PaddingCssProperty padding;
    private final PaddingLeftCssProperty paddingLeft;
    private final PaddingRightCssProperty paddingRight;
    private final PaddingTopCssProperty paddingTop;
    private final PaddingBottomCssProperty paddingBottom;
    private final InlineMetrics inlineMetrics;
    private final LayoutPredicates predicates;
    private final DebugSink debugSink;
    private final WarnSink warnSink;

    UiCssBoxSizingEngine(
            UiCssUnitResolutionContext lengthContext,
            DisplayCssProperty display,
            FlexDirectionCssProperty flexDirection,
            GapCssProperty gap,
            WidthCssProperty width,
            HeightCssProperty height,
            MinWidthCssProperty minWidth,
            MinHeightCssProperty minHeight,
            MaxWidthCssProperty maxWidth,
            MaxHeightCssProperty maxHeight,
            BoxSizingCssProperty boxSizing,
            MarginCssProperty margin,
            MarginLeftCssProperty marginLeft,
            MarginRightCssProperty marginRight,
            MarginTopCssProperty marginTop,
            MarginBottomCssProperty marginBottom,
            PaddingCssProperty padding,
            PaddingLeftCssProperty paddingLeft,
            PaddingRightCssProperty paddingRight,
            PaddingTopCssProperty paddingTop,
            PaddingBottomCssProperty paddingBottom,
            InlineMetrics inlineMetrics,
            LayoutPredicates predicates,
            DebugSink debugSink,
            WarnSink warnSink
    ) {
        this.lengthContext = lengthContext == null ? UiCssUnitResolutionContext.defaults() : lengthContext;
        this.display = display;
        this.flexDirection = flexDirection;
        this.gap = gap;
        this.width = width;
        this.height = height;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.boxSizing = boxSizing;
        this.margin = margin;
        this.marginLeft = marginLeft;
        this.marginRight = marginRight;
        this.marginTop = marginTop;
        this.marginBottom = marginBottom;
        this.padding = padding;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.inlineMetrics = inlineMetrics;
        this.predicates = predicates;
        this.debugSink = debugSink == null ? (key, message, args) -> { } : debugSink;
        this.warnSink = warnSink == null ? (key, message, args) -> { } : warnSink;
    }

    float resolvedWidth(UiDomElement element, float reference, float fallback) {
        String raw = width.raw(element);
        boolean intrinsic = intrinsicWidthRequested(element, raw);
        UiCssLength length = safeLength(raw, UiCssLength.AUTO, intrinsic);
        if (intrinsic) {
            float value = "min-content".equalsIgnoreCase(raw == null ? "" : raw.trim())
                    ? minContentWidth(element, reference)
                    : intrinsicWidth(element, reference, intrinsicKeyword(raw) ? 0f : fallback);
            debugSink.debug("intrinsic-width|" + summary(element),
                    "UI CSS intrinsic width element={} text='{}' width={} reference={} fallback={}",
                    summary(element), abbreviatedText(element), value, reference, fallback);
            return clampWidth(element, resolveBoxSizedWidth(element, value, reference, false), reference);
        }
        return clampWidth(element, resolveBoxSizedWidth(element, length.resolve(lengthContext, reference, fallback), reference, !raw.isBlank()), reference);
    }

    float resolvedHeight(UiDomElement element, float reference, float fallback) {
        String raw = height.raw(element);
        boolean intrinsic = intrinsicHeightRequested(raw);
        UiCssLength length = safeLength(raw, UiCssLength.AUTO, intrinsic);
        if (intrinsic) {
            float value = intrinsicHeight(element, reference, reference, intrinsicKeyword(raw) ? 0f : fallback);
            debugSink.debug("intrinsic-height|" + summary(element),
                    "UI CSS intrinsic height element={} text='{}' height={} reference={} fallback={}",
                    summary(element), abbreviatedText(element), value, reference, fallback);
            return clampHeight(element, resolveBoxSizedHeight(element, value, reference, false), reference);
        }
        return clampHeight(element, resolveBoxSizedHeight(element, length.resolve(lengthContext, reference, fallback), reference, !raw.isBlank()), reference);
    }

    float preferredWidth(UiDomElement element, float reference) {
        String raw = width.raw(element);
        String keyword = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if ("min-content".equals(keyword)) return minContentWidth(element, reference);
        if ("max-content".equals(keyword)) return intrinsicWidth(element, reference, 0f);
        if (intrinsicWidthRequested(element, raw) || (inlineMetrics.hasInlineContent(element) && (raw.isBlank() || percentLength(raw)))) {
            return intrinsicWidth(element, reference, 0f);
        }
        if (!raw.isBlank()) return resolveLength(element, width.name(), raw, reference, 0f);
        return Math.max(inlineMetrics.maxContentTextWidth(element), intrinsicChildrenWidth(element, reference));
    }

    float preferredHeight(UiDomElement element, float referenceW, float referenceH) {
        String raw = height.raw(element);
        if (intrinsicHeightRequested(raw)) return intrinsicHeight(element, referenceW, referenceH, 0f);
        if (!raw.isBlank()) return resolveLength(element, height.name(), raw, referenceH, 0f);
        if (inlineMetrics.hasInlineContent(element)) return intrinsicHeight(element, referenceW, referenceH, 0f);
        return intrinsicChildrenHeight(element, referenceW, referenceH);
    }

    float intrinsicChildrenWidth(UiDomElement element, float reference) {
        Flow childFlow = flow(element);
        float resolvedGap = gap.read(element, UiCssLength.ZERO, childFlow == Flow.ROW).resolve(lengthContext, reference, 0f);
        float row = 0f;
        float column = 0f;
        int count = 0;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child)
                    || predicates.outOfFlow(child)
                    || display.read(child).hidden()
                    || (!display.read(element).flex() && predicates.inlineParticipant(child))) continue;
            BoxInsets m = margin(child, new UiCssBox(0f, 0f, reference, reference));
            float childWidth = preferredWidth(child, reference) + m.left + m.right;
            if (childFlow == Flow.ROW) {
                if (count > 0) row += resolvedGap;
                row += childWidth;
            } else {
                column = Math.max(column, childWidth);
            }
            count++;
        }
        return childFlow == Flow.ROW ? row : column;
    }

    float intrinsicChildrenHeight(UiDomElement element, float referenceW, float referenceH) {
        Flow childFlow = flow(element);
        float resolvedGap = gap.read(element, UiCssLength.ZERO, childFlow == Flow.ROW).resolve(lengthContext, childFlow == Flow.ROW ? referenceW : referenceH, 0f);
        float row = 0f;
        float column = 0f;
        int count = 0;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child)
                    || predicates.outOfFlow(child)
                    || display.read(child).hidden()
                    || (!display.read(element).flex() && predicates.inlineParticipant(child))) continue;
            BoxInsets m = margin(child, new UiCssBox(0f, 0f, referenceW, referenceH));
            float childHeight = preferredHeight(child, referenceW, referenceH) + m.top + m.bottom;
            if (childFlow == Flow.ROW) {
                row = Math.max(row, childHeight);
            } else {
                if (count > 0) column += resolvedGap;
                column += childHeight;
            }
            count++;
        }
        return childFlow == Flow.ROW ? row : column;
    }

    float minContentWidth(UiDomElement element, float reference) {
        BoxInsets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, reference), Math.max(1f, reference)));
        float text = inlineMetrics.longestWordWidth(element);
        float children = minContentChildrenWidth(element, reference);
        return Math.max(text, children) + p.left + p.right;
    }

    float intrinsicWidth(UiDomElement element, float reference, float fallback) {
        BoxInsets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, reference), Math.max(1f, reference)));
        float textWidth = inlineMetrics.maxContentTextWidth(element);
        float childrenWidth = intrinsicChildrenWidth(element, Math.max(1f, reference));
        float extra = number(firstStyle(element, "fit-extra-width", "text-fit-extra", "intrinsic-extra"), 0f, "fit-extra-width", element);
        return Math.max(0f, Math.max(Math.max(textWidth, childrenWidth), fallback) + p.left + p.right + extra);
    }

    float intrinsicHeight(UiDomElement element, float referenceW, float referenceH, float fallback) {
        BoxInsets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, referenceW), Math.max(1f, referenceH)));
        float contentWidth = Math.max(1f, referenceW - p.left - p.right);
        float textHeight = inlineMetrics.textBlockHeight(element, contentWidth);
        float childrenHeight = intrinsicChildrenHeight(element, referenceW, referenceH);
        float contentHeight = textHeight + childrenHeight;
        return Math.max(0f, Math.max(contentHeight, fallback) + p.top + p.bottom);
    }

    private float minContentChildrenWidth(UiDomElement element, float reference) {
        float out = 0f;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child)
                    || predicates.outOfFlow(child)
                    || display.read(child).hidden()
                    || predicates.inlineParticipant(child)) continue;
            BoxInsets m = margin(child, new UiCssBox(0f, 0f, reference, reference));
            out = Math.max(out, minContentWidth(child, reference) + m.left + m.right);
        }
        return out;
    }

    UiCssLength safeLength(String raw, UiCssLength fallback, boolean intrinsic) {
        if (raw == null || raw.isBlank() || intrinsic) return fallback;
        try {
            return UiCssLength.parse(raw);
        } catch (RuntimeException exception) {
            warnInvalidLength(null, "length", raw, fallback == null ? UiCssLength.AUTO : fallback, exception);
            return fallback;
        }
    }

    boolean intrinsicWidthRequested(UiDomElement element, String rawWidth) {
        if (intrinsicKeyword(rawWidth)) return true;
        String raw = element.style("fit-text", element.attribute("fit-text", ""));
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "true".equals(value) || "1".equals(value) || "yes".equals(value) || "on".equals(value) || "width".equals(value);
    }

    boolean intrinsicHeightRequested(String rawHeight) {
        return intrinsicKeyword(rawHeight);
    }

    private boolean intrinsicKeyword(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "fit-content".equals(value) || "max-content".equals(value) || "min-content".equals(value);
    }

    private boolean percentLength(String raw) {
        return raw != null && raw.trim().endsWith("%");
    }

    private float resolveBoxSizedWidth(UiDomElement element, float value, float reference, boolean explicit) {
        if (!explicit || !"content-box".equals(boxSizing.read(element))) return value;
        BoxInsets p = padding(element, new UiCssBox(0f, 0f, reference, reference));
        debugSink.debug("content-box-w|" + summary(element), "UI CSS content-box width expanded element={} width={} paddingLeft={} paddingRight={}", summary(element), value, p.left, p.right);
        return value + p.left + p.right;
    }

    private float resolveBoxSizedHeight(UiDomElement element, float value, float reference, boolean explicit) {
        if (!explicit || !"content-box".equals(boxSizing.read(element))) return value;
        BoxInsets p = padding(element, new UiCssBox(0f, 0f, reference, reference));
        debugSink.debug("content-box-h|" + summary(element), "UI CSS content-box height expanded element={} height={} paddingTop={} paddingBottom={}", summary(element), value, p.top, p.bottom);
        return value + p.top + p.bottom;
    }

    private float clampWidth(UiDomElement element, float value, float reference) {
        return clamp(element, Axis.X, value, minWidth.read(element, UiCssLength.AUTO), maxWidth.read(element, UiCssLength.AUTO), reference);
    }

    private float clampHeight(UiDomElement element, float value, float reference) {
        return clamp(element, Axis.Y, value, minHeight.read(element, UiCssLength.AUTO), maxHeight.read(element, UiCssLength.AUTO), reference);
    }

    private float clamp(UiDomElement element, Axis axis, float value, UiCssLength min, UiCssLength max, float reference) {
        float out = value;
        Float minValue = min.auto() ? null : min.resolve(lengthContext, reference, out);
        Float maxValue = max.auto() ? null : max.resolve(lengthContext, reference, out);
        if (minValue != null && maxValue != null && minValue > maxValue) {
            warnSink.warn("minmax|" + summary(element) + '|' + axis, "UI CSS layout min/max conflict element={} axis={} min={} max={} reference={}; using min as effective max", summary(element), axis, minValue, maxValue, reference);
            maxValue = minValue;
        }
        if (minValue != null) out = Math.max(out, minValue);
        if (maxValue != null) out = Math.min(out, maxValue);
        return Math.max(0f, out);
    }

    private float number(String raw, float fallback, String property, UiDomElement element) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Math.max(0f, Float.parseFloat(raw.trim()));
        } catch (RuntimeException ex) {
            warnSink.warn("number|" + property + '|' + raw + '|' + summary(element), "UI CSS layout invalid number element={} property='{}' raw='{}' fallback={}", summary(element), property, raw, fallback);
            return fallback;
        }
    }

    private float resolveLength(UiDomElement element, String property, String raw, float reference, float fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return UiCssLength.parse(raw).resolve(lengthContext, reference, fallback);
        } catch (RuntimeException exception) {
            warnInvalidLength(element, property, raw, new UiCssLength(fallback, "px"), exception);
            return fallback;
        }
    }

    private void warnInvalidLength(UiDomElement element, String property, String raw, UiCssLength fallback, RuntimeException exception) {
        warnSink.warn(
                "layout-length|" + property + '|' + raw,
                "UI CSS layout invalid length element={} property='{}' raw='{}'; using fallback='{}' reason='{}'",
                summary(element),
                property,
                raw,
                fallback == null ? "auto" : fallback.cssText(),
                exception.getMessage()
        );
    }

    private BoxInsets margin(UiDomElement element, UiCssBox reference) {
        UiCssLength all = margin.read(element, UiCssLength.ZERO);
        float fallbackX = all.resolve(lengthContext, reference.width(), 0f);
        float fallbackY = all.resolve(lengthContext, reference.height(), 0f);
        float l = marginLeft.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float r = marginRight.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float t = marginTop.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        float b = marginBottom.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        return new BoxInsets(l, t, r, b);
    }

    private BoxInsets padding(UiDomElement element, UiCssBox reference) {
        UiCssLength all = padding.read(element, UiCssLength.ZERO);
        float fallbackX = all.resolve(lengthContext, reference.width(), 0f);
        float fallbackY = all.resolve(lengthContext, reference.height(), 0f);
        float l = paddingLeft.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float r = paddingRight.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float t = paddingTop.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        float b = paddingBottom.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        return new BoxInsets(l, t, r, b);
    }

    private Flow flow(UiDomElement element) {
        return flexDirection.read(element).row() ? Flow.ROW : Flow.COLUMN;
    }

    private String firstStyle(UiDomElement element, String... names) {
        for (String name : names) {
            String value = element.style(name, "");
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String abbreviatedText(UiDomElement element) {
        String text = element.textContent();
        if (text == null) return "";
        String normalized = text.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return normalized.length() <= 48 ? normalized : normalized.substring(0, 47) + "…";
    }

    private String summary(UiDomElement element) {
        if (element == null) return "<null>";
        String id = element.id();
        return element.tagName() + (id.isBlank() ? "" : "#" + id);
    }

    @FunctionalInterface
    interface InlineMetrics {
        boolean hasInlineContent(UiDomElement element);
        default float maxContentTextWidth(UiDomElement element) { return 0f; }
        default float longestWordWidth(UiDomElement element) { return 0f; }
        default float textBlockHeight(UiDomElement element, float contentWidth) { return 0f; }
    }

    interface LayoutPredicates {
        boolean outOfFlow(UiDomElement element);
        boolean inlineParticipant(UiDomElement element);
    }

    @FunctionalInterface
    interface DebugSink {
        void debug(String key, String message, Object... args);
    }

    @FunctionalInterface
    interface WarnSink {
        void warn(String key, String message, Object... args);
    }

    private enum Flow { ROW, COLUMN }
    private enum Axis { X, Y }
    private record BoxInsets(float left, float top, float right, float bottom) { }
}
