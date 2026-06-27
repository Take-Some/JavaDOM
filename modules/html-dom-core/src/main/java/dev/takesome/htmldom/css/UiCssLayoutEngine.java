package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.textOrEmpty;
import dev.takesome.htmldom.support.logging.HtmlDomLog;
import dev.takesome.htmldom.css.properties.layout.AlignItemsCssProperty;
import dev.takesome.htmldom.css.properties.layout.AlignSelfCssProperty;
import dev.takesome.htmldom.css.properties.layout.BottomCssProperty;
import dev.takesome.htmldom.css.properties.layout.BoundsCssProperty;
import dev.takesome.htmldom.css.properties.layout.BoxSizingCssProperty;
import dev.takesome.htmldom.css.properties.layout.DisplayCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexBasisCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexDirectionCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexGrowCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexShrinkCssProperty;
import dev.takesome.htmldom.css.properties.layout.FlexWrapCssProperty;
import dev.takesome.htmldom.css.properties.layout.GapCssProperty;
import dev.takesome.htmldom.css.properties.layout.HeightCssProperty;
import dev.takesome.htmldom.css.properties.layout.JustifyContentCssProperty;
import dev.takesome.htmldom.css.properties.layout.LayoutXCssProperty;
import dev.takesome.htmldom.css.properties.layout.LeftCssProperty;
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
import dev.takesome.htmldom.css.properties.layout.PositionCssProperty;
import dev.takesome.htmldom.css.properties.layout.ResolvedVerticalCssProperty;
import dev.takesome.htmldom.css.properties.layout.RightCssProperty;
import dev.takesome.htmldom.css.properties.layout.TopCssProperty;
import dev.takesome.htmldom.css.properties.layout.WidthCssProperty;
import dev.takesome.htmldom.css.properties.layout.XCssProperty;
import dev.takesome.htmldom.css.properties.layout.YCssProperty;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;
import dev.takesome.htmldom.dom.UiDomText;
import dev.takesome.htmldom.support.logging.HtmlDomLog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves registered CSS layout definitions into absolute pixel boxes. */
public final class UiCssLayoutEngine {
    private static final Logger LOG = HtmlDomLog.logger(UiCssLayoutEngine.class);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final Set<String> DEBUGGED = ConcurrentHashMap.newKeySet();

    private UiCssUnitResolutionContext lengthContext = UiCssUnitResolutionContext.defaults();
    private float viewportWidth;
    private float viewportHeight;

    private final DisplayCssProperty display;
    private final PositionCssProperty position;
    private final FlexDirectionCssProperty flexDirection;
    private final BoundsCssProperty bounds;
    private final XCssProperty x;
    private final YCssProperty y;
    private final LeftCssProperty left;
    private final TopCssProperty top;
    private final RightCssProperty right;
    private final BottomCssProperty bottom;
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
    private final GapCssProperty gap;
    private final JustifyContentCssProperty justifyContent;
    private final AlignItemsCssProperty alignItems;
    private final AlignSelfCssProperty alignSelf;
    private final FlexCssProperty flex;
    private final FlexGrowCssProperty flexGrow;
    private final FlexShrinkCssProperty flexShrink;
    private final FlexBasisCssProperty flexBasis;
    private final FlexWrapCssProperty flexWrap;
    private final LayoutXCssProperty layoutX;
    private final ResolvedVerticalCssProperty layoutY;
    private final UiIntrinsicTextMeasurer textMeasurer;
    private final UiCssInlineRunTextHook inlineRunTextHook;

    public UiCssLayoutEngine() {
        this(UiCssPropertyRegistry.loadBuiltins(), UiIntrinsicTextMeasurer.heuristic(), UiCssInlineRunTextHook.identity());
    }

    public UiCssLayoutEngine(UiCssPropertyRegistry registry) {
        this(registry, UiIntrinsicTextMeasurer.heuristic(), UiCssInlineRunTextHook.identity());
    }

    public UiCssLayoutEngine(UiCssPropertyRegistry registry, UiIntrinsicTextMeasurer textMeasurer) {
        this(registry, textMeasurer, UiCssInlineRunTextHook.identity());
    }

    public UiCssLayoutEngine(UiCssPropertyRegistry registry, UiIntrinsicTextMeasurer textMeasurer, UiCssInlineRunTextHook inlineRunTextHook) {
        Objects.requireNonNull(registry, "registry");
        this.textMeasurer = textMeasurer == null ? UiIntrinsicTextMeasurer.heuristic() : textMeasurer;
        this.inlineRunTextHook = inlineRunTextHook == null ? UiCssInlineRunTextHook.identity() : inlineRunTextHook;
        this.display = registry.requireType(DisplayCssProperty.class);
        this.position = registry.requireType(PositionCssProperty.class);
        this.flexDirection = registry.requireType(FlexDirectionCssProperty.class);
        this.bounds = registry.requireType(BoundsCssProperty.class);
        this.x = registry.requireType(XCssProperty.class);
        this.y = registry.requireType(YCssProperty.class);
        this.left = registry.requireType(LeftCssProperty.class);
        this.top = registry.requireType(TopCssProperty.class);
        this.right = registry.requireType(RightCssProperty.class);
        this.bottom = registry.requireType(BottomCssProperty.class);
        this.width = registry.requireType(WidthCssProperty.class);
        this.height = registry.requireType(HeightCssProperty.class);
        this.minWidth = registry.requireType(MinWidthCssProperty.class);
        this.minHeight = registry.requireType(MinHeightCssProperty.class);
        this.maxWidth = registry.requireType(MaxWidthCssProperty.class);
        this.maxHeight = registry.requireType(MaxHeightCssProperty.class);
        this.boxSizing = registry.requireType(BoxSizingCssProperty.class);
        this.margin = registry.requireType(MarginCssProperty.class);
        this.marginLeft = registry.requireType(MarginLeftCssProperty.class);
        this.marginRight = registry.requireType(MarginRightCssProperty.class);
        this.marginTop = registry.requireType(MarginTopCssProperty.class);
        this.marginBottom = registry.requireType(MarginBottomCssProperty.class);
        this.padding = registry.requireType(PaddingCssProperty.class);
        this.paddingLeft = registry.requireType(PaddingLeftCssProperty.class);
        this.paddingRight = registry.requireType(PaddingRightCssProperty.class);
        this.paddingTop = registry.requireType(PaddingTopCssProperty.class);
        this.paddingBottom = registry.requireType(PaddingBottomCssProperty.class);
        this.gap = registry.requireType(GapCssProperty.class);
        this.justifyContent = registry.requireType(JustifyContentCssProperty.class);
        this.alignItems = registry.requireType(AlignItemsCssProperty.class);
        this.alignSelf = registry.requireType(AlignSelfCssProperty.class);
        this.flex = registry.requireType(FlexCssProperty.class);
        this.flexGrow = registry.requireType(FlexGrowCssProperty.class);
        this.flexShrink = registry.requireType(FlexShrinkCssProperty.class);
        this.flexBasis = registry.requireType(FlexBasisCssProperty.class);
        this.flexWrap = registry.requireType(FlexWrapCssProperty.class);
        this.layoutX = registry.requireType(LayoutXCssProperty.class);
        this.layoutY = registry.requireType(ResolvedVerticalCssProperty.class);
    }

    public UiCssLayoutResult layout(UiDomDocument document, float viewportWidth, float viewportHeight) {
        Objects.requireNonNull(document, "document");
        UiCssLayoutResult result = new UiCssLayoutResult();
        UiDomElement root = document.root();
        float safeViewportW = Math.max(0f, viewportWidth);
        float safeViewportH = Math.max(0f, viewportHeight);
        this.viewportWidth = safeViewportW;
        this.viewportHeight = safeViewportH;
        float rootFontSize = rootFontSize(root, safeViewportW, safeViewportH);
        this.lengthContext = UiCssUnitResolutionContext.viewport(safeViewportW, safeViewportH, rootFontSize);
        debugOnce("length-context|" + safeViewportW + "x" + safeViewportH + "|" + rootFontSize,
                "UI CSS length context viewport={}x{} rootFontSize={}", safeViewportW, safeViewportH, rootFontSize);
        UiCssBox rootBox = resolveExplicitBox(root, null, 0f, 0f, safeViewportW, safeViewportH);
        writeBox(root, rootBox);
        result.put(root, rootBox);
        commitTextLines(root, rootBox, result);
        layoutChildren(root, rootBox, result);
        commitScrollContainers(root, result);
        return result;
    }

    private void layoutChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        if (display.read(parent).hidden()) return;
        if (display.read(parent).flex()) layoutFlexChildren(parent, parentBox, result);
        else layoutFlowChildren(parent, parentBox, result);
    }

    private void layoutFlowChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        Flow flow = flow(parent);
        Insets insets = padding(parent, parentBox);
        float resolvedGap = gap.read(parent, UiCssLength.ZERO, flow == Flow.ROW).resolve(lengthContext, flow == Flow.ROW ? parentBox.width() : parentBox.height(), 0f);
        float contentWidth = Math.max(0f, parentBox.width() - insets.left - insets.right);
        float cursor = flow == Flow.COLUMN ? textBlockHeight(parent, contentWidth) : 0f;
        for (UiDomNode childNode : parent.children()) {
            if (!(childNode instanceof UiDomElement child)) continue;
            if (inlineParticipant(child) && !outOfFlow(child)) continue;
            UiCssBox childBox = childBox(parentBox, insets, flow, cursor, child);
            Insets m = margin(child, parentBox);
            cursor += (flow == Flow.ROW ? childBox.width() + m.left + m.right : childBox.height() + m.top + m.bottom) + resolvedGap;
            commitChild(child, childBox, result);
        }
    }

    private void layoutFlexChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        Flow flow = flow(parent);
        Insets insets = padding(parent, parentBox);
        float mainSize = Math.max(0f, flow == Flow.ROW ? parentBox.width() - insets.left - insets.right : parentBox.height() - insets.top - insets.bottom);
        float crossSize = Math.max(0f, flow == Flow.ROW ? parentBox.height() - insets.top - insets.bottom : parentBox.width() - insets.left - insets.right);
        float resolvedGap = gap.read(parent, UiCssLength.ZERO, flow == Flow.ROW).resolve(lengthContext, mainSize, 0f);
        List<FlexLine> lines = flexLines(parent, parentBox, flow, mainSize, crossSize, resolvedGap);
        String justify = justifyContent.read(parent);
        String align = alignItems.read(parent);
        float crossCursor = 0f;
        debugOnce("flex|" + summary(parent) + '|' + flow + '|' + justify + '|' + align,
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
                UiCssBox box = relativeOffset(item.element, new UiCssBox(x0, y0, itemW, itemH), parentBox.width(), parentBox.height());
                commitChild(item.element, box, result);
                mainCursor += item.main + item.mainMargin(flow) + itemGap;
            }
            crossCursor += line.crossSize + resolvedGap;
        }
        for (UiDomNode childNode : parent.children()) {
            if (childNode instanceof UiDomElement child && outOfFlow(child)) commitChild(child, resolveOutOfFlowBox(child, parentBox), result);
        }
    }

    private List<FlexLine> flexLines(UiDomElement parent, UiCssBox parentBox, Flow flow, float mainSize, float crossSize, float gapValue) {
        ArrayList<FlexLine> lines = new ArrayList<>();
        FlexLine current = new FlexLine();
        boolean wrap = "wrap".equals(flexWrap.read(parent));
        for (UiDomNode childNode : parent.children()) {
            if (!(childNode instanceof UiDomElement child) || outOfFlow(child)) continue;
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
        Insets m = margin(element, parent);
        Float basis = flexBasisValue(element, mainRef);
        float mainFallback = flow == Flow.ROW ? preferredWidth(element, mainRef) : preferredHeight(element, crossRef, mainRef);
        float crossFallback = flow == Flow.ROW ? preferredHeight(element, mainRef, crossRef) : preferredWidth(element, crossRef);
        float main = basis == null ? (flow == Flow.ROW ? resolvedWidth(element, mainRef, mainFallback) : resolvedHeight(element, mainRef, mainFallback)) : basis;
        float cross = flow == Flow.ROW ? resolvedHeight(element, crossRef, crossFallback) : resolvedWidth(element, crossRef, crossFallback);
        float minMain = Math.max(0f, Math.min(main, mainFallback));
        return new FlexItem(element, m, Math.max(0f, main), minMain, Math.max(0f, cross), flexGrow(element), flexShrink(element));
    }

    private UiCssBox childBox(UiCssBox parentBox, Insets insets, Flow flow, float cursor, UiDomElement child) {
        if (display.read(child).hidden()) return new UiCssBox(parentBox.x(), parentBox.y(), 0f, 0f);
        if (outOfFlow(child)) return resolveOutOfFlowBox(child, parentBox);
        return flow == Flow.ROW ? resolveRowChild(child, parentBox, insets, cursor) : resolveColumnChild(child, parentBox, insets, cursor);
    }

    private void commitChild(UiDomElement child, UiCssBox box, UiCssLayoutResult result) {
        writeBox(child, box);
        result.put(child, box);
        commitTextLines(child, box, result);
        layoutChildren(child, box, result);
    }

    private UiCssBox resolveColumnChild(UiDomElement element, UiCssBox parent, Insets insets, float cursorY) {
        Insets m = margin(element, parent);
        float contentW = Math.max(0f, parent.width() - insets.left - insets.right - m.left - m.right);
        float x0 = parent.x() + insets.left + m.left;
        float w = resolvedWidth(element, contentW, contentW);
        float h = resolvedHeight(element, parent.height(), preferredHeight(element, contentW, parent.height()));
        float y0 = parent.y() + parent.height() - insets.top - cursorY - m.top - h;
        return relativeOffset(element, new UiCssBox(x0, y0, w, h), parent.width(), parent.height());
    }

    private UiCssBox resolveRowChild(UiDomElement element, UiCssBox parent, Insets insets, float cursorX) {
        Insets m = margin(element, parent);
        float contentH = Math.max(0f, parent.height() - insets.top - insets.bottom - m.top - m.bottom);
        float x0 = parent.x() + insets.left + cursorX + m.left;
        float y0 = parent.y() + insets.top + m.top;
        float w = resolvedWidth(element, parent.width(), preferredWidth(element, parent.width()));
        float h = resolvedHeight(element, contentH, contentH);
        return relativeOffset(element, new UiCssBox(x0, y0, w, h), parent.width(), parent.height());
    }

    private UiCssBox resolveOutOfFlowBox(UiDomElement element, UiCssBox parentBox) {
        String pos = positionValue(element);
        if ("fixed".equals(pos)) return resolveExplicitBox(element, null, 0f, 0f, viewportWidth, viewportHeight);
        UiCssBox containing = containingBlockFor(element, parentBox);
        return resolveExplicitBox(element, containing, containing.x(), containing.y(), containing.width(), containing.height());
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
        boolean intrinsicWidth = explicitBounds == null && intrinsicWidthRequested(element, rawWidth);
        boolean intrinsicHeight = explicitBounds == null && intrinsicHeightRequested(rawHeight);
        UiCssLength wLength = explicitBounds == null ? safeLength(rawWidth, UiCssLength.AUTO, intrinsicWidth) : explicitBounds.width();
        UiCssLength hLength = explicitBounds == null ? safeLength(rawHeight, UiCssLength.AUTO, intrinsicHeight) : explicitBounds.height();
        float fallbackW = parentBox == null ? referenceW : (intrinsicWidth ? intrinsicWidth(element, referenceW, 0f) : 0f);
        float fallbackH = parentBox == null ? referenceH : (intrinsicHeight ? intrinsicHeight(element, referenceW, referenceH, 0f) : 0f);
        float w = clampWidth(element, resolveBoxSizedWidth(element, wLength.resolve(lengthContext, referenceW, fallbackW), referenceW, explicitBounds != null || (!rawWidth.isBlank() && !intrinsicWidth)), referenceW);
        float h = clampHeight(element, resolveBoxSizedHeight(element, hLength.resolve(lengthContext, referenceH, fallbackH), referenceH, explicitBounds != null || (!rawHeight.isBlank() && !intrinsicHeight)), referenceH);
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
            return originY + resolveLength(element, bottom.name(), raw, referenceH, 0f);
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

    private UiCssBox relativeOffset(UiDomElement element, UiCssBox flowBox, float referenceW, float referenceH) {
        if (!position.read(element).relative()) return flowBox;
        float dx = explicitOffset(element, Axis.X, flowBox.width(), referenceW, UiCssLength.ZERO);
        float dy = explicitOffset(element, Axis.Y, flowBox.height(), referenceH, UiCssLength.ZERO);
        return new UiCssBox(flowBox.x() + dx, flowBox.y() + dy, flowBox.width(), flowBox.height());
    }

    private boolean outOfFlow(UiDomElement element) {
        return position.read(element).outOfFlow() || bounds.read(element).isPresent() || hasExplicitPosition(element);
    }

    private boolean hasExplicitPosition(UiDomElement element) {
        return !x.raw(element).isBlank() || !y.raw(element).isBlank() || !left.raw(element).isBlank() || !top.raw(element).isBlank() || !right.raw(element).isBlank() || !bottom.raw(element).isBlank();
    }

    private Flow flow(UiDomElement element) {
        String raw = flexDirection.raw(element).toLowerCase(Locale.ROOT);
        if (raw.contains("reverse")) warnOnce("flex-reverse|" + summary(element), "UI CSS flex reverse direction is not fully supported yet element={} raw='{}'; using non-reverse axis", summary(element), raw);
        return display.read(element).flex() && flexDirection.read(element).row() ? Flow.ROW : Flow.COLUMN;
    }

    private float rootFontSize(UiDomElement root, float viewportWidth, float viewportHeight) {
        String raw = textOrEmpty(root, item -> item.style("font-size", ""));
        if (raw == null || raw.isBlank()) return UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE;
        try {
            return Math.max(1f, UiCssLength.parse(raw).resolve(
                    UiCssUnitResolutionContext.viewport(viewportWidth, viewportHeight, UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE),
                    viewportWidth,
                    UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE
            ));
        } catch (RuntimeException ex) {
            warnOnce("root-font-size|" + raw, "UI CSS invalid root font-size raw='{}' fallback={} reason='{}'", raw, UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE, ex.getMessage());
            return UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE;
        }
    }

    private float explicitOffset(UiDomElement element, Axis axis, float size, float reference, UiCssLength fallback) {
        String raw = axis.primary(this).raw(element);
        if (!raw.isBlank()) return offset(element, axis, raw, size, reference);
        raw = axis.startProperty(this).raw(element);
        if (!raw.isBlank()) return offset(element, axis, raw, size, reference);
        raw = axis.endProperty(this).raw(element);
        if (!raw.isBlank()) return Math.max(0f, reference - size - resolveLength(element, axis.endProperty(this).name(), raw, reference, 0f));
        return fallback.resolve(lengthContext, reference, 0f);
    }

    private float offset(UiDomElement element, Axis axis, String raw, float size, float reference) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (axis.start(value)) return 0f;
        if (axis.end(value)) return reference - size;
        if (Axis.CENTER.matches(value)) return (reference - size) * 0.5f;
        try {
            return resolveLength(element, axis.primary(this).name(), value, reference, 0f);
        } catch (RuntimeException ex) {
            warnOnce("offset|" + raw, "UI CSS layout invalid offset raw='{}' axis={} reference={} size={} reason='{}'", raw, axis, reference, size, ex.getMessage());
            return 0f;
        }
    }

    private float resolvedWidth(UiDomElement element, float reference, float fallback) {
        String raw = width.raw(element);
        boolean intrinsic = intrinsicWidthRequested(element, raw);
        UiCssLength length = safeLength(raw, UiCssLength.AUTO, intrinsic);
        if (intrinsic) {
            float value = "min-content".equalsIgnoreCase(raw == null ? "" : raw.trim())
                    ? minContentWidth(element, reference)
                    : intrinsicWidth(element, reference, intrinsicKeyword(raw) ? 0f : fallback);
            debugOnce("intrinsic-width|" + summary(element),
                    "UI CSS intrinsic width element={} text='{}' width={} reference={} fallback={}",
                    summary(element), abbreviatedText(element), value, reference, fallback);
            return clampWidth(element, resolveBoxSizedWidth(element, value, reference, false), reference);
        }
        return clampWidth(element, resolveBoxSizedWidth(element, length.resolve(lengthContext, reference, fallback), reference, !raw.isBlank()), reference);
    }

    private float resolvedHeight(UiDomElement element, float reference, float fallback) {
        String raw = height.raw(element);
        boolean intrinsic = intrinsicHeightRequested(raw);
        UiCssLength length = safeLength(raw, UiCssLength.AUTO, intrinsic);
        if (intrinsic) {
            float value = intrinsicHeight(element, reference, reference, intrinsicKeyword(raw) ? 0f : fallback);
            debugOnce("intrinsic-height|" + summary(element),
                    "UI CSS intrinsic height element={} text='{}' height={} reference={} fallback={}",
                    summary(element), abbreviatedText(element), value, reference, fallback);
            return clampHeight(element, resolveBoxSizedHeight(element, value, reference, false), reference);
        }
        return clampHeight(element, resolveBoxSizedHeight(element, length.resolve(lengthContext, reference, fallback), reference, !raw.isBlank()), reference);
    }


    private UiCssLength safeLength(String raw, UiCssLength fallback, boolean intrinsic) {
        if (raw == null || raw.isBlank() || intrinsic) return fallback;
        try {
            return UiCssLength.parse(raw);
        } catch (RuntimeException exception) {
            warnInvalidLength(null, "length", raw, fallback == null ? UiCssLength.AUTO : fallback, exception);
            return fallback;
        }
    }

    private boolean intrinsicWidthRequested(UiDomElement element, String rawWidth) {
        if (intrinsicKeyword(rawWidth)) return true;
        String raw = element.style("fit-text", element.attribute("fit-text", ""));
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "true".equals(value) || "1".equals(value) || "yes".equals(value) || "on".equals(value) || "width".equals(value);
    }

    private boolean intrinsicHeightRequested(String rawHeight) {
        return intrinsicKeyword(rawHeight);
    }

    private boolean intrinsicKeyword(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "fit-content".equals(value) || "max-content".equals(value) || "min-content".equals(value);
    }

    private float intrinsicWidth(UiDomElement element, float reference, float fallback) {
        Insets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, reference), Math.max(1f, reference)));
        float textWidth = maxContentTextWidth(element);
        float childrenWidth = intrinsicChildrenWidth(element, Math.max(1f, reference));
        float extra = number(firstStyle(element, "fit-extra-width", "text-fit-extra", "intrinsic-extra"), 0f, "fit-extra-width", element);
        return Math.max(0f, Math.max(Math.max(textWidth, childrenWidth), fallback) + p.left + p.right + extra);
    }

    private float intrinsicHeight(UiDomElement element, float referenceW, float referenceH, float fallback) {
        Insets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, referenceW), Math.max(1f, referenceH)));
        float contentWidth = Math.max(1f, referenceW - p.left - p.right);
        float textHeight = textBlockHeight(element, contentWidth);
        float childrenHeight = intrinsicChildrenHeight(element, referenceW, referenceH);
        float contentHeight = textHeight + childrenHeight;
        return Math.max(0f, Math.max(contentHeight, fallback) + p.top + p.bottom);
    }

    private float intrinsicTextContentWidth(UiDomElement element) {
        return intrinsicTextMetrics(element).width();
    }

    private UiIntrinsicTextMetrics intrinsicTextMetrics(UiDomElement element) {
        String text = element.textContent();
        if (text == null || text.isBlank()) return UiIntrinsicTextMetrics.ZERO;
        String normalized = text.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) return UiIntrinsicTextMetrics.ZERO;
        float fallbackFontSize = fontSize(element, 1f);
        return textMeasurer.measure(normalized, fontId(element), fontScale(element), fallbackFontSize);
    }

    private float intrinsicChildrenWidth(UiDomElement element, float reference) {
        Flow childFlow = flow(element);
        float resolvedGap = gap.read(element, UiCssLength.ZERO, childFlow == Flow.ROW).resolve(lengthContext, reference, 0f);
        float row = 0f;
        float column = 0f;
        int count = 0;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child) || outOfFlow(child) || display.read(child).hidden() || (!display.read(element).flex() && inlineParticipant(child))) continue;
            Insets m = margin(child, new UiCssBox(0f, 0f, reference, reference));
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

    private float intrinsicChildrenHeight(UiDomElement element, float referenceW, float referenceH) {
        Flow childFlow = flow(element);
        float resolvedGap = gap.read(element, UiCssLength.ZERO, childFlow == Flow.ROW).resolve(lengthContext, childFlow == Flow.ROW ? referenceW : referenceH, 0f);
        float row = 0f;
        float column = 0f;
        int count = 0;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child) || outOfFlow(child) || display.read(child).hidden() || (!display.read(element).flex() && inlineParticipant(child))) continue;
            Insets m = margin(child, new UiCssBox(0f, 0f, referenceW, referenceH));
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

    private float preferredWidth(UiDomElement element, float reference) {
        String raw = width.raw(element);
        String keyword = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if ("min-content".equals(keyword)) return minContentWidth(element, reference);
        if ("max-content".equals(keyword)) return intrinsicWidth(element, reference, 0f);
        if (intrinsicWidthRequested(element, raw) || (hasInlineContent(element) && (raw.isBlank() || percentLength(raw)))) {
            return intrinsicWidth(element, reference, 0f);
        }
        if (!raw.isBlank()) return resolveLength(element, width.name(), raw, reference, 0f);
        return Math.max(maxContentTextWidth(element), intrinsicChildrenWidth(element, reference));
    }

    private boolean percentLength(String raw) {
        return raw != null && raw.trim().endsWith("%");
    }

    private float preferredHeight(UiDomElement element, float referenceW, float referenceH) {
        String raw = height.raw(element);
        if (intrinsicHeightRequested(raw)) return intrinsicHeight(element, referenceW, referenceH, 0f);
        if (!raw.isBlank()) return resolveLength(element, height.name(), raw, referenceH, 0f);
        if (hasInlineContent(element)) return intrinsicHeight(element, referenceW, referenceH, 0f);
        return intrinsicChildrenHeight(element, referenceW, referenceH);
    }

    private void commitTextLines(UiDomElement element, UiCssBox box, UiCssLayoutResult result) {
        InlineLayout inline = layoutInlineContent(element, box);
        if (!inline.lines.isEmpty()) {
            result.putLineBoxes(element, inline.lines);
            result.putInlineBoxes(element, inline.runs);
            debugOnce("inline-formatting|" + summary(element) + '|' + inline.lines.size() + '|' + inline.runs.size(),
                    "UI CSS inline formatting element={} lines={} runs={} box={}x{}", summary(element), inline.lines.size(), inline.runs.size(), box.width(), box.height());
        }
    }

    private InlineLayout layoutInlineContent(UiDomElement element, UiCssBox box) {
        Insets p = padding(element, box);
        float contentW = Math.max(0f, box.width() - p.left - p.right);
        if (contentW <= 0f) return InlineLayout.EMPTY;
        List<InlineLine> lines = wrapInlineLines(element, contentW);
        if (lines.isEmpty()) return InlineLayout.EMPTY;

        String align = lineAlign(element);
        float cursorTop = box.y() + box.height() - p.top;
        ArrayList<UiCssLineBox> lineBoxes = new ArrayList<>();
        ArrayList<UiCssInlineBox> inlineBoxes = new ArrayList<>();
        int lineIndex = 0;
        for (InlineLine line : lines) {
            float lineHeight = Math.max(1f, line.height);
            float baseline = Math.max(0f, line.baseline);
            float dx = "center".equals(align) ? (contentW - line.width) * 0.5f : "right".equals(align) || "end".equals(align) ? contentW - line.width : 0f;
            float lineY = cursorTop - lineHeight;
            StringBuilder text = new StringBuilder();
            int runIndex = 0;
            for (InlineToken run : line.runs) {
                if (run.lineBreak) continue;
                float runY = lineY + lineHeight - baseline - run.height + run.baseline + run.verticalOffset;
                float runX = box.x() + p.left + Math.max(0f, dx) + run.x;
                UiCssInlineRunText runText = resolveInlineRunText(element, run, runX, runY, lineIndex, runIndex++);
                inlineBoxes.add(new UiCssInlineBox(run.sourceNodeId, run.styleElement.nodeId(), runText.paintText(), run.atomic, runX, runY, run.width, run.height, run.baseline));
                if (!runText.lineText().isBlank()) {
                    if ((run.spaceBefore || run.leadingAdvance > 0f) && !text.isEmpty()) text.append(' ');
                    text.append(runText.lineText());
                }
                else if (run.atomic) text.append('\u25a1');
            }
            lineBoxes.add(new UiCssLineBox(text.toString(), box.x() + p.left + Math.max(0f, dx), lineY, line.width, lineHeight, baseline));
            cursorTop -= lineHeight;
            lineIndex++;
        }
        return new InlineLayout(lineBoxes, inlineBoxes);
    }

    private UiCssInlineRunText resolveInlineRunText(UiDomElement owner, InlineToken run, float runX, float runY, int lineIndex, int runIndex) {
        UiCssInlineRunText fallback = UiCssInlineRunText.text(run.text);
        if (run.lineBreak) return fallback;
        UiCssInlineRunTextEvent event = new UiCssInlineRunTextEvent(
                owner,
                run.styleElement,
                run.sourceNodeId,
                run.text,
                run.atomic,
                run.lineBreak,
                run.spaceBefore,
                run.leadingAdvance,
                runX,
                runY,
                run.width,
                run.height,
                run.baseline,
                lineIndex,
                runIndex
        );
        try {
            UiCssInlineRunText resolved = inlineRunTextHook.resolve(event);
            return resolved == null ? fallback : resolved;
        } catch (RuntimeException error) {
            warnOnce(
                    "inline-run-text-hook|" + summary(owner) + '|' + error.getClass().getName() + '|' + error.getMessage(),
                    "UI CSS inline run text hook failed element={} line={} run={} reason='{}'; using original inline text",
                    summary(owner), lineIndex, runIndex, error.getMessage()
            );
            return fallback;
        }
    }

    private float textBlockHeight(UiDomElement element, float contentWidth) {
        float out = 0f;
        for (InlineLine line : wrapInlineLines(element, Math.max(1f, contentWidth))) out += Math.max(1f, line.height);
        return out;
    }

    private List<InlineLine> wrapInlineLines(UiDomElement element, float contentWidth) {
        List<InlineToken> tokens = inlineTokens(element, inlineFragments(element));
        if (tokens.isEmpty()) return List.of();
        ArrayList<InlineLine> lines = new ArrayList<>();
        InlineLine current = new InlineLine();
        for (InlineToken token : tokens) {
            if (token.lineBreak) {
                lines.add(current.isEmpty() ? InlineLine.blank(lineHeight(element, fontSize(element, 1f))) : current);
                current = new InlineLine();
                continue;
            }
            float leading = token.spaceBefore && !current.isEmpty() ? token.spaceWidth : 0f;
            if (!current.isEmpty() && current.width + leading + token.width > contentWidth) {
                lines.add(current);
                current = new InlineLine();
                leading = 0f;
            }
            current.add(token, leading);
        }
        if (!current.isEmpty()) lines.add(current);
        return lines;
    }

    private List<InlineToken> inlineTokens(UiDomElement owner, List<InlineFragment> fragments) {
        if (fragments.isEmpty()) return List.of();
        String whiteSpace = firstStyle(owner, "white-space").toLowerCase(Locale.ROOT);
        boolean preserve = whiteSpace.equals("pre") || whiteSpace.equals("pre-wrap") || whiteSpace.equals("break-spaces");
        boolean nowrap = whiteSpace.equals("nowrap") || whiteSpace.equals("pre");
        ArrayList<InlineToken> out = new ArrayList<>();
        WhitespaceState state = new WhitespaceState();
        for (InlineFragment fragment : fragments) {
            if (fragment.atomic) {
                out.add(inlineToken(fragment, "", true, consumeSpace(state, out, preserve)));
                state.afterContent = true;
                state.pendingSpace = false;
                continue;
            }
            String text = fragment.text == null ? "" : fragment.text.replace("\r\n", "\n").replace('\r', '\n');
            if (text.isEmpty()) continue;
            if (preserve) appendPreservedInlineTokens(fragment, text, nowrap, out, state);
            else appendCollapsedInlineTokens(fragment, text, nowrap, out, state);
        }
        return out;
    }

    private void appendCollapsedInlineTokens(InlineFragment fragment, String text, boolean nowrap, ArrayList<InlineToken> out, WhitespaceState state) {
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                flushWord(fragment, word, out, state);
                if (ch == '\n' && !nowrap) {
                    if (!out.isEmpty()) out.add(InlineToken.lineBreak(fragment.styleElement));
                    state.afterContent = false;
                    state.pendingSpace = false;
                } else if (state.afterContent || word.length() > 0) {
                    state.pendingSpace = true;
                }
            } else {
                word.append(ch);
            }
        }
        flushWord(fragment, word, out, state);
    }

    private void appendPreservedInlineTokens(InlineFragment fragment, String text, boolean nowrap, ArrayList<InlineToken> out, WhitespaceState state) {
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' && !nowrap) {
                flushPreserved(fragment, run, out, state);
                out.add(InlineToken.lineBreak(fragment.styleElement));
                state.afterContent = false;
                state.pendingSpace = false;
            } else {
                run.append(ch);
            }
        }
        flushPreserved(fragment, run, out, state);
    }

    private void flushWord(InlineFragment fragment, StringBuilder word, ArrayList<InlineToken> out, WhitespaceState state) {
        if (word.isEmpty()) return;
        boolean spaceBefore = consumeSpace(state, out, false);
        out.add(inlineToken(fragment, word.toString(), false, spaceBefore));
        word.setLength(0);
        state.afterContent = true;
        state.pendingSpace = false;
    }

    private void flushPreserved(InlineFragment fragment, StringBuilder run, ArrayList<InlineToken> out, WhitespaceState state) {
        if (run.isEmpty()) return;
        out.add(inlineToken(fragment, run.toString(), false, false));
        run.setLength(0);
        state.afterContent = true;
        state.pendingSpace = false;
    }

    private boolean consumeSpace(WhitespaceState state, ArrayList<InlineToken> out, boolean preserve) {
        if (preserve) return false;
        boolean value = state.pendingSpace && !out.isEmpty();
        state.pendingSpace = false;
        return value;
    }

    private InlineToken inlineToken(InlineFragment fragment, String text, boolean atomic, boolean spaceBefore) {
        float size = fontSize(fragment.styleElement, 1f);
        float lineHeight = lineHeight(fragment.styleElement, size);
        float width = atomic ? atomicInlineWidth(fragment.styleElement, size) : textWidth(fragment.styleElement, text);
        float height = atomic ? atomicInlineHeight(fragment.styleElement, size, lineHeight) : lineHeight;
        float baseline = atomic ? atomicBaseline(fragment.styleElement, height) : Math.min(height, Math.max(0f, (height - size) * 0.5f + size * 0.86f));
        float spaceWidth = spaceBefore ? textWidth(fragment.styleElement, " ") : 0f;
        float verticalOffset = verticalAlignOffset(fragment.styleElement, size, height);
        return new InlineToken(fragment.sourceNodeId, fragment.styleElement, text, atomic, false, spaceBefore, width, height, baseline, verticalOffset, spaceWidth, 0f, 0f);
    }

    private List<InlineFragment> inlineFragments(UiDomElement element) {
        if (element == null) return List.of();
        if (display.read(element).flex()) return List.of();
        ArrayList<InlineFragment> out = new ArrayList<>();
        for (UiDomNode child : element.children()) collectInline(child, element, out);
        return out;
    }

    private void collectInline(UiDomNode node, UiDomElement styleElement, ArrayList<InlineFragment> out) {
        if (node instanceof UiDomText text) {
            out.add(new InlineFragment(text.nodeId(), styleElement, text.text(), false));
            return;
        }
        if (!(node instanceof UiDomElement element)) return;
        if (display.read(element).hidden() || outOfFlow(element)) return;
        if (!inlineParticipant(element)) return;
        if (atomicInline(element)) {
            out.add(new InlineFragment(element.nodeId(), element, "", true));
            return;
        }
        for (UiDomNode child : element.children()) collectInline(child, element, out);
    }

    private boolean inlineParticipant(UiDomElement element) {
        String displayValue = firstStyle(element, "display").toLowerCase(Locale.ROOT);
        if (displayValue.equals("inline") || displayValue.equals("inline-block")) return true;
        if (replacedInline(element)) return true;
        return inlineTag(element);
    }

    private boolean atomicInline(UiDomElement element) {
        return inlineBlock(element) || replacedInline(element) || (inlineLevel(element) && inlineBoxMetrics(element));
    }

    private boolean inlineLevel(UiDomElement element) {
        String displayValue = firstStyle(element, "display").toLowerCase(Locale.ROOT);
        return displayValue.equals("inline") || displayValue.equals("inline-block") || inlineTag(element);
    }

    private boolean inlineTag(UiDomElement element) {
        if (element == null) return false;
        return switch (element.tagName()) {
            case "a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "data", "dfn", "em", "i", "kbd", "label", "mark", "q", "s", "samp", "small", "span", "strong", "sub", "sup", "time", "u", "var" -> true;
            default -> false;
        };
    }

    private boolean inlineBlock(UiDomElement element) {
        return "inline-block".equals(firstStyle(element, "display").toLowerCase(Locale.ROOT));
    }

    private boolean inlineBoxMetrics(UiDomElement element) {
        if (element == null) return false;
        return meaningfulBoxValue(width.raw(element))
                || meaningfulBoxValue(height.raw(element))
                || meaningfulBoxValue(firstStyle(element, "padding", "padding-left", "padding-right", "padding-top", "padding-bottom"))
                || meaningfulBoxValue(firstStyle(element, "margin", "margin-left", "margin-right", "margin-top", "margin-bottom"))
                || meaningfulBoxValue(firstStyle(element, "border", "border-width", "border-color", "border-style"));
    }

    private boolean meaningfulBoxValue(String raw) {
        if (raw == null) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()
                || value.equals("auto")
                || value.equals("none")
                || value.equals("normal")
                || value.equals("transparent")
                || value.equals("initial")
                || value.equals("inherit")
                || value.equals("unset")) return false;
        boolean sawNumeric = false;
        for (String token : value.split("\s+")) {
            String part = token.trim();
            if (part.isBlank() || part.equals("none") || part.equals("solid") || part.equals("transparent")) continue;
            try {
                String numeric = part.replaceAll("[a-z%]+$", "");
                if (numeric.isBlank() || numeric.equals("+" ) || numeric.equals("-")) continue;
                float parsed = Float.parseFloat(numeric);
                sawNumeric = true;
                if (Math.abs(parsed) > 0.0001f) return true;
            } catch (RuntimeException ignored) {
                return true;
            }
        }
        return false && sawNumeric;
    }

    private boolean replacedInline(UiDomElement element) {
        if ("img".equals(element.tagName()) || "svg".equals(element.tagName())) return true;
        for (String token : element.classList().values()) {
            String key = token == null ? "" : token.trim().toLowerCase(Locale.ROOT).replace('_', '-');
            if (key.equals("fa") || key.equals("fas") || key.equals("far") || key.equals("fab") || key.equals("fa-solid") || key.equals("fa-regular") || key.equals("fa-brands")) return true;
            if (key.startsWith("fa-") && key.length() > 3) return true;
        }
        return false;
    }

    private float atomicInlineWidth(UiDomElement element, float fontSize) {
        UiCssBox reference = inlineBoxReference(fontSize, lineHeight(element, fontSize));
        Insets p = padding(element, reference);
        Insets m = margin(element, reference);
        String raw = width.raw(element);
        boolean explicit = !raw.isBlank() && !intrinsicWidthRequested(element, raw);
        float contentWidth;
        if (explicit) {
            try { contentWidth = Math.max(1f, UiCssLength.parse(raw).resolve(lengthContext, reference.width(), fontSize)); }
            catch (RuntimeException ignored) { contentWidth = Math.max(1f, fontSize); }
        } else if (replacedInline(element) && !inlineBlock(element) && !inlineBoxMetrics(element)) {
            contentWidth = Math.max(1f, fontSize * 1.12f);
        } else {
            contentWidth = Math.max(1f, Math.max(maxContentTextWidth(element), intrinsicChildrenWidth(element, Math.max(1f, fontSize * 12f))));
        }
        float borderBoxWidth = explicit && !"content-box".equals(boxSizing.read(element))
                ? contentWidth
                : contentWidth + p.left + p.right;
        return Math.max(1f, borderBoxWidth + m.left + m.right);
    }

    private float atomicInlineHeight(UiDomElement element, float fontSize, float fallbackLineHeight) {
        UiCssBox reference = inlineBoxReference(fontSize, fallbackLineHeight);
        Insets p = padding(element, reference);
        Insets m = margin(element, reference);
        String raw = height.raw(element);
        boolean explicit = !raw.isBlank() && !intrinsicHeightRequested(raw);
        float contentHeight;
        if (explicit) {
            try { contentHeight = Math.max(1f, UiCssLength.parse(raw).resolve(lengthContext, reference.height(), fallbackLineHeight)); }
            catch (RuntimeException ignored) { contentHeight = Math.max(1f, fallbackLineHeight); }
        } else {
            contentHeight = Math.max(1f, fallbackLineHeight);
        }
        float borderBoxHeight = explicit && !"content-box".equals(boxSizing.read(element))
                ? contentHeight
                : contentHeight + p.top + p.bottom;
        return Math.max(1f, borderBoxHeight + m.top + m.bottom);
    }

    private UiCssBox inlineBoxReference(float fontSize, float lineHeight) {
        return new UiCssBox(0f, 0f, Math.max(1f, fontSize * 12f), Math.max(1f, lineHeight));
    }

    private float atomicBaseline(UiDomElement element, float height) {
        String align = firstStyle(element, "vertical-align").toLowerCase(Locale.ROOT);
        if (align.equals("top") || align.equals("text-top")) return height;
        if (align.equals("bottom") || align.equals("text-bottom")) return 0f;
        if (align.equals("middle")) return height * 0.5f;
        return height * 0.8f;
    }

    private float verticalAlignOffset(UiDomElement element, float fontSize, float height) {
        String raw = firstStyle(element, "vertical-align").trim().toLowerCase(Locale.ROOT);
        if (raw.isBlank() || "baseline".equals(raw)) return 0f;
        if ("sub".equals(raw)) return -fontSize * 0.22f;
        if ("super".equals(raw)) return fontSize * 0.38f;
        if ("middle".equals(raw)) return fontSize * 0.12f;
        if ("top".equals(raw) || "text-top".equals(raw)) return fontSize * 0.18f;
        if ("bottom".equals(raw) || "text-bottom".equals(raw)) return -fontSize * 0.18f;
        try { return UiCssLength.parse(raw).resolve(lengthContext, fontSize, 0f); }
        catch (RuntimeException ignored) { return 0f; }
    }

    private boolean hasInlineContent(UiDomElement element) {
        for (InlineFragment fragment : inlineFragments(element)) {
            if (fragment.atomic || (fragment.text != null && !fragment.text.isBlank())) return true;
        }
        return false;
    }

    private String directText(UiDomElement element) {
        StringBuilder out = new StringBuilder();
        for (UiDomNode child : element.children()) {
            if (child instanceof UiDomText text) out.append(text.text()).append(' ');
        }
        return out.toString().trim();
    }

    private float maxContentTextWidth(UiDomElement element) {
        float line = 0f;
        float max = 0f;
        for (InlineToken token : inlineTokens(element, inlineFragments(element))) {
            if (token.lineBreak) {
                max = Math.max(max, line);
                line = 0f;
            } else {
                float leading = token.spaceBefore && line > 0f ? token.spaceWidth : 0f;
                line += leading + token.width;
            }
        }
        return Math.max(max, line);
    }

    private float minContentWidth(UiDomElement element, float reference) {
        Insets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, reference), Math.max(1f, reference)));
        float text = longestWordWidth(element);
        float children = minContentChildrenWidth(element, reference);
        return Math.max(text, children) + p.left + p.right;
    }

    private float minContentChildrenWidth(UiDomElement element, float reference) {
        float out = 0f;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child) || outOfFlow(child) || display.read(child).hidden() || inlineParticipant(child)) continue;
            Insets m = margin(child, new UiCssBox(0f, 0f, reference, reference));
            out = Math.max(out, minContentWidth(child, reference) + m.left + m.right);
        }
        return out;
    }

    private float longestWordWidth(UiDomElement element) {
        float max = 0f;
        for (InlineFragment fragment : inlineFragments(element)) {
            if (fragment.atomic) {
                max = Math.max(max, inlineToken(fragment, "", true, false).width);
                continue;
            }
            String text = fragment.text == null ? "" : fragment.text.replace('\n', ' ').replace('\r', ' ');
            for (String word : text.split("\\s+")) {
                if (!word.isBlank()) max = Math.max(max, textWidth(fragment.styleElement, word));
            }
        }
        return max;
    }

    private float textWidth(UiDomElement element, String text) {
        if (text == null || text.isEmpty()) return 0f;
        String fontId = fontId(element);
        float scale = fontScale(element);
        float size = fontSize(element, 1f);
        float measured = textMeasurer.measure(text, fontId, scale, size).width();
        if (text.trim().isEmpty()) {
            return Math.max(measured, textMeasurer.spaceWidth(fontId, scale, size));
        }
        return measured;
    }

    private float lineHeight(UiDomElement element, float fontSize) {
        String raw = firstStyle(element, "line-height").trim().toLowerCase(Locale.ROOT);
        if (raw.isBlank() || "normal".equals(raw)) return Math.max(1f, fontSize * 1.25f);
        try {
            if (raw.matches("[0-9]+(\\.[0-9]+)?")) return Math.max(1f, Float.parseFloat(raw) * fontSize);
            return Math.max(1f, UiCssLength.parse(raw).resolve(lengthContext, fontSize, fontSize * 1.25f));
        } catch (RuntimeException ignored) {
            return Math.max(1f, fontSize * 1.25f);
        }
    }

    private String lineAlign(UiDomElement element) {
        String raw = firstStyle(element, "text-align", "align").toLowerCase(Locale.ROOT);
        if (raw.isBlank() && "button".equals(element.tagName())) return "center";
        if (raw.equals("right") || raw.equals("end")) return "right";
        if (raw.equals("center")) return "center";
        return "left";
    }

    private boolean preserveWhitespace(UiDomElement element) {
        String raw = firstStyle(element, "white-space").toLowerCase(Locale.ROOT);
        return raw.equals("pre") || raw.equals("pre-wrap") || raw.equals("break-spaces");
    }

    private float fontSize(UiDomElement element, float scale) {
        String raw = firstStyle(element, "font-size");
        if (!raw.isBlank()) {
            try {
                return Math.max(1f, UiCssLength.parse(raw).resolve(lengthContext, 1f, 16f) * Math.max(0.01f, scale));
            } catch (RuntimeException ignored) {
            }
        }
        return (titleFont(element) ? 32f : 14f) * Math.max(0.01f, scale);
    }

    private boolean titleFont(UiDomElement element) {
        String font = firstStyle(element, "font-family", "font").toLowerCase(Locale.ROOT);
        String tag = element.tagName();
        return font.contains("title") || font.contains("pixel") || "h1".equals(tag) || "h2".equals(tag);
    }

    private String fontId(UiDomElement element) {
        String raw = firstStyle(element, "font-family", "font");
        return UiCssFontFamilyResolver.resolveEngineFontId(raw.isBlank() ? UiCssFontFamilyResolver.DEFAULT_STACK : raw, element.computedStyle());
    }

    private float fontScale(UiDomElement element) {
        String scale = firstStyle(element, "scale", "font-scale");
        if (!scale.isBlank()) {
            try { return Math.max(0.01f, Float.parseFloat(scale.trim())); }
            catch (RuntimeException ignored) { }
        }
        return 1f;
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
        String normalized = text.replace('\n', ' ').replace('\r', ' ').replaceAll("\s+", " ").trim();
        return normalized.length() <= 48 ? normalized : normalized.substring(0, 47) + "…";
    }

    private float resolveBoxSizedWidth(UiDomElement element, float value, float reference, boolean explicit) {
        if (!explicit || !"content-box".equals(boxSizing.read(element))) return value;
        Insets p = padding(element, new UiCssBox(0f, 0f, reference, reference));
        debugOnce("content-box-w|" + summary(element), "UI CSS content-box width expanded element={} width={} paddingLeft={} paddingRight={}", summary(element), value, p.left, p.right);
        return value + p.left + p.right;
    }

    private float resolveBoxSizedHeight(UiDomElement element, float value, float reference, boolean explicit) {
        if (!explicit || !"content-box".equals(boxSizing.read(element))) return value;
        Insets p = padding(element, new UiCssBox(0f, 0f, reference, reference));
        debugOnce("content-box-h|" + summary(element), "UI CSS content-box height expanded element={} height={} paddingTop={} paddingBottom={}", summary(element), value, p.top, p.bottom);
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
            warnOnce("minmax|" + summary(element) + '|' + axis, "UI CSS layout min/max conflict element={} axis={} min={} max={} reference={}; using min as effective max", summary(element), axis, minValue, maxValue, reference);
            maxValue = minValue;
        }
        if (minValue != null) out = Math.max(out, minValue);
        if (maxValue != null) out = Math.min(out, maxValue);
        return Math.max(0f, out);
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
            warnOnce("basis|" + raw + '|' + summary(element), "UI CSS layout invalid flex-basis element={} raw='{}' reference={}", summary(element), raw, reference);
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
            warnOnce("number|" + property + '|' + raw + '|' + summary(element), "UI CSS layout invalid number element={} property='{}' raw='{}' fallback={}", summary(element), property, raw, fallback);
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
        warnOnce(
                "layout-length|" + property + '|' + raw,
                "UI CSS layout invalid length element={} property='{}' raw='{}'; using fallback='{}' reason='{}'",
                summary(element),
                property,
                raw,
                fallback == null ? "auto" : fallback.cssText(),
                exception.getMessage()
        );
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

    private Insets margin(UiDomElement element, UiCssBox reference) {
        UiCssLength all = margin.read(element, UiCssLength.ZERO);
        float fallbackX = all.resolve(lengthContext, reference.width(), 0f);
        float fallbackY = all.resolve(lengthContext, reference.height(), 0f);
        float l = marginLeft.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float r = marginRight.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float t = marginTop.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        float b = marginBottom.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        return new Insets(l, t, r, b);
    }

    private Insets padding(UiDomElement element, UiCssBox reference) {
        UiCssLength all = padding.read(element, UiCssLength.ZERO);
        float fallbackX = all.resolve(lengthContext, reference.width(), 0f);
        float fallbackY = all.resolve(lengthContext, reference.height(), 0f);
        float l = paddingLeft.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float r = paddingRight.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float t = paddingTop.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        float b = paddingBottom.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        return new Insets(l, t, r, b);
    }

    private void commitScrollContainers(UiDomElement root, UiCssLayoutResult result) {
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
        String normalized = value.isBlank() ? "visible" : value.trim().toLowerCase(Locale.ROOT);
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

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private void writeBox(UiDomElement element, UiCssBox box) {
        element.setComputedStyle(layoutX.name(), px(box.x()));
        element.setComputedStyle(layoutY.name(), px(box.y()));
        element.setComputedStyle(width.name(), px(box.width()));
        element.setComputedStyle(height.name(), px(box.height()));
    }

    private String px(float value) {
        if (value == Math.rint(value)) return Math.round(value) + "px";
        return value + "px";
    }

    private String summary(UiDomElement element) {
        if (element == null) return "<null>";
        String id = element.id();
        return element.tagName() + (id.isBlank() ? "" : "#" + id);
    }

    private static void warnOnce(String key, String message, Object... args) {
        if (WARNED.add(key)) LOG.warn(message, args);
    }

    private static void debugOnce(String key, String message, Object... args) {
        if (DEBUGGED.add(key)) LOG.debug(message, args);
    }

    private enum Flow { ROW, COLUMN }

    private enum Axis {
        X, Y;
        static final CenterKeywords CENTER = new CenterKeywords();
        UiCssBasePropertySpec primary(UiCssLayoutEngine engine) { return this == X ? engine.x : engine.y; }
        UiCssBasePropertySpec startProperty(UiCssLayoutEngine engine) { return this == X ? engine.left : engine.top; }
        UiCssBasePropertySpec endProperty(UiCssLayoutEngine engine) { return this == X ? engine.right : engine.bottom; }
        boolean start(String value) { return this == X ? "left".equals(value) || "start".equals(value) : "top".equals(value) || "start".equals(value); }
        boolean end(String value) { return this == X ? "right".equals(value) || "end".equals(value) : "bottom".equals(value) || "end".equals(value); }
    }

    private static final class CenterKeywords {
        boolean matches(String value) { return "center".equals(value) || "middle".equals(value); }
    }

    private record ContentExtent(float width, float height) { }

    private record ScrollPolicy(boolean scrollX, boolean scrollY, boolean clipX, boolean clipY) { }

    private record InlineLayout(List<UiCssLineBox> lines, List<UiCssInlineBox> runs) {
        private static final InlineLayout EMPTY = new InlineLayout(List.of(), List.of());
    }

    private record InlineFragment(int sourceNodeId, UiDomElement styleElement, String text, boolean atomic) { }

    private static final class WhitespaceState {
        private boolean afterContent;
        private boolean pendingSpace;
    }

    private static final class InlineLine {
        private final ArrayList<InlineToken> runs = new ArrayList<>();
        private float width;
        private float height;
        private float baseline;

        private void add(InlineToken token, float leading) {
            float safeLeading = Math.max(0f, leading);
            InlineToken placed = token.at(width + safeLeading, safeLeading);
            runs.add(placed);
            width += safeLeading + placed.width;
            float top = Math.max(0f, placed.height - placed.baseline - placed.verticalOffset);
            float bottom = Math.max(0f, placed.baseline + placed.verticalOffset);
            baseline = Math.max(baseline, bottom);
            height = Math.max(height, top + baseline);
        }

        private boolean isEmpty() {
            return runs.isEmpty();
        }

        private static InlineLine blank(float height) {
            InlineLine line = new InlineLine();
            line.height = Math.max(1f, height);
            line.baseline = line.height * 0.8f;
            return line;
        }
    }

    private record InlineToken(int sourceNodeId, UiDomElement styleElement, String text, boolean atomic, boolean lineBreak, boolean spaceBefore, float width, float height, float baseline, float verticalOffset, float spaceWidth, float x, float leadingAdvance) {
        private InlineToken at(float x) {
            return at(x, leadingAdvance);
        }

        private InlineToken at(float x, float leadingAdvance) {
            return new InlineToken(sourceNodeId, styleElement, text, atomic, lineBreak, spaceBefore, width, height, baseline, verticalOffset, spaceWidth, x, Math.max(0f, leadingAdvance));
        }

        private static InlineToken lineBreak(UiDomElement styleElement) {
            return new InlineToken(styleElement.nodeId(), styleElement, "", false, true, false, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        }
    }

    private record Insets(float left, float top, float right, float bottom) { }

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
        private final Insets margin;
        private float main;
        private final float minMain;
        private final float cross;
        private final float grow;
        private final float shrink;
        private FlexItem(UiDomElement element, Insets margin, float main, float minMain, float cross, float grow, float shrink) {
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
