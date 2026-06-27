package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.properties.layout.AlignItemsCssProperty;
import dev.takesome.htmldom.css.properties.layout.AlignSelfCssProperty;
import dev.takesome.htmldom.css.properties.layout.DisplayCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexBasisCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexDirectionCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexGrowCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexShrinkCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexWrapCssProperty;
import dev.takesome.htmldom.css.properties.layout.GapCssProperty;
import dev.takesome.htmldom.css.properties.layout.JustifyContentCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginBottomCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginLeftCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginRightCssProperty;
import dev.takesome.htmldom.css.properties.layout.MarginTopCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingBottomCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingLeftCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingRightCssProperty;
import dev.takesome.htmldom.css.properties.layout.PaddingTopCssProperty;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;

import java.util.ArrayList;
import java.util.List;

/** Flex formatting phase extracted from the main layout orchestrator. */
final class UiCssFlexLayoutEngine {
    private final UiCssUnitResolutionContext lengthContext;
    private final DisplayCssProperty display;
    private final FlexDirectionCssProperty flexDirection;
    private final GapCssProperty gap;
    private final JustifyContentCssProperty justifyContent;
    private final AlignItemsCssProperty alignItems;
    private final AlignSelfCssProperty alignSelf;
    private final FlexCssProperty flex;
    private final FlexGrowCssProperty flexGrow;
    private final FlexShrinkCssProperty flexShrink;
    private final FlexBasisCssProperty flexBasis;
    private final FlexWrapCssProperty flexWrap;
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
    private final FlexBoxResolver resolver;
    private final DebugSink debugSink;
    private final WarnSink warnSink;

    UiCssFlexLayoutEngine(
            UiCssUnitResolutionContext lengthContext,
            DisplayCssProperty display,
            FlexDirectionCssProperty flexDirection,
            GapCssProperty gap,
            JustifyContentCssProperty justifyContent,
            AlignItemsCssProperty alignItems,
            AlignSelfCssProperty alignSelf,
            FlexCssProperty flex,
            FlexGrowCssProperty flexGrow,
            FlexShrinkCssProperty flexShrink,
            FlexBasisCssProperty flexBasis,
            FlexWrapCssProperty flexWrap,
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
            FlexBoxResolver resolver,
            DebugSink debugSink,
            WarnSink warnSink
    ) {
        this.lengthContext = lengthContext == null ? UiCssUnitResolutionContext.defaults() : lengthContext;
        this.display = display;
        this.flexDirection = flexDirection;
        this.gap = gap;
        this.justifyContent = justifyContent;
        this.alignItems = alignItems;
        this.alignSelf = alignSelf;
        this.flex = flex;
        this.flexGrow = flexGrow;
        this.flexShrink = flexShrink;
        this.flexBasis = flexBasis;
        this.flexWrap = flexWrap;
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
        this.resolver = resolver;
        this.debugSink = debugSink == null ? (key, message, args) -> { } : debugSink;
        this.warnSink = warnSink == null ? (key, message, args) -> { } : warnSink;
    }

    void layoutFlexChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        Flow flow = flow(parent);
        BoxInsets insets = padding(parent, parentBox);
        float mainSize = Math.max(0f, flow == Flow.ROW ? parentBox.width() - insets.left - insets.right : parentBox.height() - insets.top - insets.bottom);
        float crossSize = Math.max(0f, flow == Flow.ROW ? parentBox.height() - insets.top - insets.bottom : parentBox.width() - insets.left - insets.right);
        float resolvedGap = gap.read(parent, UiCssLength.ZERO, flow == Flow.ROW).resolve(lengthContext, mainSize, 0f);
        List<FlexLine> lines = flexLines(parent, parentBox, flow, mainSize, crossSize, resolvedGap);
        String justify = justifyContent.read(parent);
        String align = alignItems.read(parent);
        float crossCursor = 0f;
        debugSink.debug("flex|" + summary(parent) + '|' + flow + '|' + justify + '|' + align,
                "UI flex layout parent={} flow={} lines={} justify='{}' align='{}' wrap='{}' main={} cross={} gap={}",
                summary(parent), flow, lines.size(), justify, align, flexWrap.read(parent), mainSize, crossSize, resolvedGap);
        for (FlexLine line : lines) {
            float free = mainSize - line.mainSize(flow, resolvedGap);
            line.applyFlex(free);
            float used = line.mainSize(flow, resolvedGap);
            float remaining = Math.max(0f, mainSize - used);
            float mainCursor = justifyOffset(justify, remaining, line.items.size());
            float itemGap = justifyGap(justify, remaining, resolvedGap, line.items.size());
            for (FlexItem item : line.items) {
                float cross = item.cross;
                String itemAlign = alignSelf(item.element, align);
                if ("stretch".equals(itemAlign)) cross = Math.max(0f, line.crossSize - item.crossMargin(flow));
                float crossOffset = alignOffset(itemAlign, Math.max(0f, line.crossSize - cross - item.crossMargin(flow)));
                float itemW = flow == Flow.ROW ? item.main : cross;
                float itemH = flow == Flow.ROW ? cross : item.main;
                float x0 = flow == Flow.ROW
                        ? parentBox.x() + insets.left + mainCursor + item.margin.left
                        : parentBox.x() + insets.left + crossCursor + crossOffset + item.margin.left;
                float y0 = flow == Flow.ROW
                        ? parentBox.y() + parentBox.height() - insets.top - crossCursor - crossOffset - item.margin.top - itemH
                        : parentBox.y() + parentBox.height() - insets.top - mainCursor - item.margin.top - itemH;
                UiCssBox box = resolver.relativeOffset(item.element, new UiCssBox(x0, y0, itemW, itemH), parentBox.width(), parentBox.height());
                resolver.commitChild(item.element, box, result);
                mainCursor += item.main + item.mainMargin(flow) + itemGap;
            }
            crossCursor += line.crossSize + resolvedGap;
        }
        for (UiDomNode childNode : parent.children()) {
            if (childNode instanceof UiDomElement child && resolver.outOfFlow(child)) {
                resolver.commitChild(child, resolver.resolveOutOfFlowBox(child, parentBox), result);
            }
        }
    }

    private List<FlexLine> flexLines(UiDomElement parent, UiCssBox parentBox, Flow flow, float mainSize, float crossSize, float gapValue) {
        ArrayList<FlexLine> lines = new ArrayList<>();
        FlexLine current = new FlexLine();
        boolean wrap = "wrap".equals(flexWrap.read(parent));
        for (UiDomNode childNode : parent.children()) {
            if (!(childNode instanceof UiDomElement child) || resolver.outOfFlow(child)) continue;
            FlexItem item = flexItem(child, parentBox, flow, mainSize, crossSize);
            if (display.read(child).hidden()) item = item.hidden();
            float next = current.items.isEmpty() ? item.outerMain(flow) : current.mainSize(flow, gapValue) + gapValue + item.outerMain(flow);
            if (wrap && !current.items.isEmpty() && next > mainSize) {
                lines.add(current);
                current = new FlexLine();
            }
            current.add(item, flow);
        }
        if (!current.items.isEmpty()) lines.add(current);
        return lines;
    }

    private FlexItem flexItem(UiDomElement element, UiCssBox parent, Flow flow, float mainRef, float crossRef) {
        BoxInsets m = margin(element, parent);
        Float basis = flexBasisValue(element, mainRef);
        float mainFallback = flow == Flow.ROW ? resolver.preferredWidth(element, mainRef) : resolver.preferredHeight(element, crossRef, mainRef);
        float crossFallback = flow == Flow.ROW ? resolver.preferredHeight(element, mainRef, crossRef) : resolver.preferredWidth(element, crossRef);
        float main = basis == null ? (flow == Flow.ROW ? resolver.resolvedWidth(element, mainRef, mainFallback) : resolver.resolvedHeight(element, mainRef, mainFallback)) : basis;
        float cross = flow == Flow.ROW ? resolver.resolvedHeight(element, crossRef, crossFallback) : resolver.resolvedWidth(element, crossRef, crossFallback);
        float minMain = Math.max(0f, Math.min(main, mainFallback));
        return new FlexItem(element, m, Math.max(0f, main), minMain, Math.max(0f, cross), flexGrow(element), flexShrink(element));
    }

    private Flow flow(UiDomElement element) {
        return flexDirection.read(element).row() ? Flow.ROW : Flow.COLUMN;
    }

    private Float flexBasisValue(UiDomElement element, float reference) {
        String raw = flexBasis.raw(element);
        if (raw.isBlank()) {
            String shorthand = flex.raw(element);
            if (!shorthand.isBlank() && !"none".equalsIgnoreCase(shorthand.trim())) {
                String[] parts = shorthand.trim().split("\\s+");
                if (parts.length > 2) raw = parts[2];
            }
        }
        if (raw.isBlank() || "auto".equalsIgnoreCase(raw.trim())) return null;
        try {
            return Math.max(0f, UiCssLength.parse(raw).resolve(lengthContext, reference, 0f));
        } catch (RuntimeException ex) {
            warnSink.warn("basis|" + raw + '|' + summary(element), "UI CSS layout invalid flex-basis element={} raw='{}' reference={}", summary(element), raw, reference);
            return null;
        }
    }

    private float flexGrow(UiDomElement element) {
        String shorthand = flex.raw(element);
        if (!shorthand.isBlank()) {
            if ("none".equalsIgnoreCase(shorthand.trim())) return 0f;
            String[] parts = shorthand.trim().split("\\s+");
            if (parts.length > 0) return number(parts[0], 0f, "flex-grow", element);
        }
        return number(flexGrow.raw(element), 0f, "flex-grow", element);
    }

    private float flexShrink(UiDomElement element) {
        String shorthand = flex.raw(element);
        if (!shorthand.isBlank()) {
            if ("none".equalsIgnoreCase(shorthand.trim())) return 0f;
            String[] parts = shorthand.trim().split("\\s+");
            if (parts.length > 1) return number(parts[1], 1f, "flex-shrink", element);
        }
        return number(flexShrink.raw(element), 1f, "flex-shrink", element);
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

    private String alignSelf(UiDomElement element, String parentAlign) {
        String value = alignSelf.read(element);
        return "auto".equals(value) ? parentAlign : value;
    }

    private float justifyOffset(String justify, float remaining, int count) {
        if ("center".equals(justify)) return remaining * 0.5f;
        if ("flex-end".equals(justify)) return remaining;
        if ("space-around".equals(justify) && count > 0) return remaining / count * 0.5f;
        return 0f;
    }

    private float justifyGap(String justify, float remaining, float fallbackGap, int count) {
        if (count <= 1) return fallbackGap;
        if ("space-between".equals(justify)) return fallbackGap + remaining / (count - 1);
        if ("space-around".equals(justify)) return fallbackGap + remaining / count;
        return fallbackGap;
    }

    private float alignOffset(String align, float remaining) {
        if ("center".equals(align)) return remaining * 0.5f;
        if ("flex-end".equals(align)) return remaining;
        return 0f;
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

    private String summary(UiDomElement element) {
        if (element == null) return "<null>";
        String id = element.id();
        return element.tagName() + (id.isBlank() ? "" : "#" + id);
    }

    @FunctionalInterface
    interface DebugSink {
        void debug(String key, String message, Object... args);
    }

    @FunctionalInterface
    interface WarnSink {
        void warn(String key, String message, Object... args);
    }

    interface FlexBoxResolver {
        boolean outOfFlow(UiDomElement element);
        UiCssBox resolveOutOfFlowBox(UiDomElement element, UiCssBox parentBox);
        UiCssBox relativeOffset(UiDomElement element, UiCssBox box, float referenceW, float referenceH);
        void commitChild(UiDomElement child, UiCssBox box, UiCssLayoutResult result);
        float preferredWidth(UiDomElement element, float reference);
        float preferredHeight(UiDomElement element, float referenceW, float referenceH);
        float resolvedWidth(UiDomElement element, float reference, float fallback);
        float resolvedHeight(UiDomElement element, float reference, float fallback);
    }

    private enum Flow { ROW, COLUMN }

    private record BoxInsets(float left, float top, float right, float bottom) { }

    private static final class FlexLine {
        private final ArrayList<FlexItem> items = new ArrayList<>();
        private float crossSize;
        void add(FlexItem item, Flow flow) { items.add(item); crossSize = Math.max(crossSize, item.cross + item.crossMargin(flow)); }
        float mainSize(Flow flow, float gapValue) {
            float out = 0f;
            for (FlexItem item : items) out += item.outerMain(flow);
            return out + Math.max(0, items.size() - 1) * gapValue;
        }
        void applyFlex(float free) {
            if (items.isEmpty() || free == 0f) return;
            if (free > 0f) {
                float totalGrow = 0f;
                for (FlexItem item : items) totalGrow += item.grow;
                if (totalGrow <= 0f) return;
                for (FlexItem item : items) item.main = Math.max(0f, item.main + free * (item.grow / totalGrow));
                return;
            }
            float remaining = free;
            ArrayList<FlexItem> flexible = new ArrayList<>(items);
            while (remaining < -0.001f && !flexible.isEmpty()) {
                float totalShrink = 0f;
                for (FlexItem item : flexible) totalShrink += item.shrink * item.main;
                if (totalShrink <= 0f) return;
                ArrayList<FlexItem> next = new ArrayList<>();
                float applied = 0f;
                for (FlexItem item : flexible) {
                    float share = remaining * ((item.shrink * item.main) / totalShrink);
                    float minDelta = item.minMain - item.main;
                    float delta = Math.max(share, minDelta);
                    item.main = Math.max(item.minMain, item.main + delta);
                    applied += delta;
                    if (item.main > item.minMain + 0.001f) next.add(item);
                }
                if (Math.abs(applied) < 0.001f) return;
                remaining -= applied;
                flexible = next;
            }
        }
    }

    private static final class FlexItem {
        private final UiDomElement element;
        private final BoxInsets margin;
        private float main;
        private final float minMain;
        private final float cross;
        private final float grow;
        private final float shrink;
        private FlexItem(UiDomElement element, BoxInsets margin, float main, float minMain, float cross, float grow, float shrink) {
            this.element = element;
            this.margin = margin;
            this.main = main;
            this.minMain = Math.max(0f, minMain);
            this.cross = cross;
            this.grow = grow;
            this.shrink = shrink;
        }
        private FlexItem hidden() { return new FlexItem(element, margin, 0f, 0f, 0f, 0f, 0f); }
        private float outerMain(Flow flow) { return main + mainMargin(flow); }
        private float mainMargin(Flow flow) { return flow == Flow.ROW ? margin.left + margin.right : margin.top + margin.bottom; }
        private float crossMargin(Flow flow) { return flow == Flow.ROW ? margin.top + margin.bottom : margin.left + margin.right; }
    }
}
