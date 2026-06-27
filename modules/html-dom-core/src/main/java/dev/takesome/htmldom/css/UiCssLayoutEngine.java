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
        UiCssBox rootBox = positioningEngine().resolveInitialBox(root, safeViewportW, safeViewportH);
        writeBox(root, rootBox);
        result.put(root, rootBox);
        commitTextLines(root, rootBox, result);
        layoutChildren(root, rootBox, result);
        new UiCssScrollExtentEngine(lengthContext).commitScrollContainers(root, result);
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

    private UiCssFlexLayoutEngine flexLayoutEngine() {
        return new UiCssFlexLayoutEngine(
                lengthContext,
                display,
                flexDirection,
                gap,
                justifyContent,
                alignItems,
                alignSelf,
                flex,
                flexGrow,
                flexShrink,
                flexBasis,
                flexWrap,
                margin,
                marginLeft,
                marginRight,
                marginTop,
                marginBottom,
                padding,
                paddingLeft,
                paddingRight,
                paddingTop,
                paddingBottom,
                new UiCssFlexLayoutEngine.FlexBoxResolver() {
                    @Override
                    public boolean outOfFlow(UiDomElement element) {
                        return UiCssLayoutEngine.this.outOfFlow(element);
                    }

                    @Override
                    public UiCssBox resolveOutOfFlowBox(UiDomElement element, UiCssBox parentBox) {
                        return UiCssLayoutEngine.this.resolveOutOfFlowBox(element, parentBox);
                    }

                    @Override
                    public UiCssBox relativeOffset(UiDomElement element, UiCssBox box, float referenceW, float referenceH) {
                        return UiCssLayoutEngine.this.relativeOffset(element, box, referenceW, referenceH);
                    }

                    @Override
                    public void commitChild(UiDomElement child, UiCssBox box, UiCssLayoutResult result) {
                        UiCssLayoutEngine.this.commitChild(child, box, result);
                    }

                    @Override
                    public float preferredWidth(UiDomElement element, float reference) {
                        return UiCssLayoutEngine.this.preferredWidth(element, reference);
                    }

                    @Override
                    public float preferredHeight(UiDomElement element, float referenceW, float referenceH) {
                        return UiCssLayoutEngine.this.preferredHeight(element, referenceW, referenceH);
                    }

                    @Override
                    public float resolvedWidth(UiDomElement element, float reference, float fallback) {
                        return UiCssLayoutEngine.this.resolvedWidth(element, reference, fallback);
                    }

                    @Override
                    public float resolvedHeight(UiDomElement element, float reference, float fallback) {
                        return UiCssLayoutEngine.this.resolvedHeight(element, reference, fallback);
                    }
                },
                UiCssLayoutEngine::debugOnce,
                UiCssLayoutEngine::warnOnce
        );
    }

    private void layoutFlexChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        flexLayoutEngine().layoutFlexChildren(parent, parentBox, result);
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

    private UiCssPositioningEngine positioningEngine() {
        return new UiCssPositioningEngine(
                lengthContext,
                viewportWidth,
                viewportHeight,
                position,
                bounds,
                x,
                y,
                left,
                top,
                right,
                bottom,
                width,
                height,
                new UiCssPositioningEngine.SizingResolver() {
                    @Override
                    public UiCssLength safeLength(String raw, UiCssLength fallback, boolean intrinsic) {
                        return UiCssLayoutEngine.this.safeLength(raw, fallback, intrinsic);
                    }

                    @Override
                    public boolean intrinsicWidthRequested(UiDomElement element, String rawWidth) {
                        return UiCssLayoutEngine.this.intrinsicWidthRequested(element, rawWidth);
                    }

                    @Override
                    public boolean intrinsicHeightRequested(String rawHeight) {
                        return UiCssLayoutEngine.this.intrinsicHeightRequested(rawHeight);
                    }

                    @Override
                    public float intrinsicWidth(UiDomElement element, float reference, float fallback) {
                        return UiCssLayoutEngine.this.intrinsicWidth(element, reference, fallback);
                    }

                    @Override
                    public float intrinsicHeight(UiDomElement element, float referenceW, float referenceH, float fallback) {
                        return UiCssLayoutEngine.this.intrinsicHeight(element, referenceW, referenceH, fallback);
                    }

                    @Override
                    public float resolveBoxSizedWidth(UiDomElement element, float value, float reference, boolean explicit) {
                        return UiCssLayoutEngine.this.resolveBoxSizedWidth(element, value, reference, explicit);
                    }

                    @Override
                    public float resolveBoxSizedHeight(UiDomElement element, float value, float reference, boolean explicit) {
                        return UiCssLayoutEngine.this.resolveBoxSizedHeight(element, value, reference, explicit);
                    }

                    @Override
                    public float clampWidth(UiDomElement element, float value, float reference) {
                        return UiCssLayoutEngine.this.clampWidth(element, value, reference);
                    }

                    @Override
                    public float clampHeight(UiDomElement element, float value, float reference) {
                        return UiCssLayoutEngine.this.clampHeight(element, value, reference);
                    }

                    @Override
                    public float resolveLength(UiDomElement element, String property, String raw, float reference, float fallback) {
                        return UiCssLayoutEngine.this.resolveLength(element, property, raw, reference, fallback);
                    }
                },
                UiCssLayoutEngine::warnOnce
        );
    }

    private UiCssBox resolveOutOfFlowBox(UiDomElement element, UiCssBox parentBox) {
        return positioningEngine().resolveOutOfFlowBox(element, parentBox);
    }

    private UiCssBox relativeOffset(UiDomElement element, UiCssBox flowBox, float referenceW, float referenceH) {
        return positioningEngine().relativeOffset(element, flowBox, referenceW, referenceH);
    }

    private boolean outOfFlow(UiDomElement element) {
        return positioningEngine().outOfFlow(element);
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

    private UiCssBoxSizingEngine boxSizingEngine() {
        return new UiCssBoxSizingEngine(
                lengthContext,
                display,
                flexDirection,
                gap,
                width,
                height,
                minWidth,
                minHeight,
                maxWidth,
                maxHeight,
                boxSizing,
                margin,
                marginLeft,
                marginRight,
                marginTop,
                marginBottom,
                padding,
                paddingLeft,
                paddingRight,
                paddingTop,
                paddingBottom,
                new UiCssBoxSizingEngine.InlineMetrics() {
                    @Override
                    public boolean hasInlineContent(UiDomElement element) {
                        return UiCssLayoutEngine.this.hasInlineContent(element);
                    }

                    @Override
                    public float maxContentTextWidth(UiDomElement element) {
                        return UiCssLayoutEngine.this.maxContentTextWidth(element);
                    }

                    @Override
                    public float longestWordWidth(UiDomElement element) {
                        return UiCssLayoutEngine.this.longestWordWidth(element);
                    }

                    @Override
                    public float textBlockHeight(UiDomElement element, float contentWidth) {
                        return UiCssLayoutEngine.this.textBlockHeight(element, contentWidth);
                    }
                },
                new UiCssBoxSizingEngine.LayoutPredicates() {
                    @Override
                    public boolean outOfFlow(UiDomElement element) {
                        return UiCssLayoutEngine.this.outOfFlow(element);
                    }

                    @Override
                    public boolean inlineParticipant(UiDomElement element) {
                        return UiCssLayoutEngine.this.inlineParticipant(element);
                    }
                },
                UiCssLayoutEngine::debugOnce,
                UiCssLayoutEngine::warnOnce
        );
    }

    private float resolvedWidth(UiDomElement element, float reference, float fallback) {
        return boxSizingEngine().resolvedWidth(element, reference, fallback);
    }

    private float resolvedHeight(UiDomElement element, float reference, float fallback) {
        return boxSizingEngine().resolvedHeight(element, reference, fallback);
    }

    private float intrinsicChildrenWidth(UiDomElement element, float reference) {
        return boxSizingEngine().intrinsicChildrenWidth(element, reference);
    }

    private float intrinsicChildrenHeight(UiDomElement element, float referenceW, float referenceH) {
        return boxSizingEngine().intrinsicChildrenHeight(element, referenceW, referenceH);
    }

    private float preferredWidth(UiDomElement element, float reference) {
        return boxSizingEngine().preferredWidth(element, reference);
    }

    private float preferredHeight(UiDomElement element, float referenceW, float referenceH) {
        return boxSizingEngine().preferredHeight(element, referenceW, referenceH);
    }

    private UiCssLength safeLength(String raw, UiCssLength fallback, boolean intrinsic) {
        return boxSizingEngine().safeLength(raw, fallback, intrinsic);
    }

    private boolean intrinsicWidthRequested(UiDomElement element, String rawWidth) {
        return boxSizingEngine().intrinsicWidthRequested(element, rawWidth);
    }

    private boolean intrinsicHeightRequested(String rawHeight) {
        return boxSizingEngine().intrinsicHeightRequested(rawHeight);
    }

    private float intrinsicWidth(UiDomElement element, float reference, float fallback) {
        return boxSizingEngine().intrinsicWidth(element, reference, fallback);
    }

    private float intrinsicHeight(UiDomElement element, float referenceW, float referenceH, float fallback) {
        return boxSizingEngine().intrinsicHeight(element, referenceW, referenceH, fallback);
    }

    private UiCssInlineFormattingEngine inlineFormatter() {
        return new UiCssInlineFormattingEngine(
                lengthContext,
                display,
                width,
                height,
                boxSizing,
                margin,
                marginLeft,
                marginRight,
                marginTop,
                marginBottom,
                padding,
                paddingLeft,
                paddingRight,
                paddingTop,
                paddingBottom,
                textMeasurer,
                inlineRunTextHook,
                this::intrinsicChildrenWidth,
                UiCssLayoutEngine::debugOnce,
                UiCssLayoutEngine::warnOnce
        );
    }

    private void commitTextLines(UiDomElement element, UiCssBox box, UiCssLayoutResult result) {
        inlineFormatter().commitTextLines(element, box, result);
    }

    private float textBlockHeight(UiDomElement element, float contentWidth) {
        return inlineFormatter().textBlockHeight(element, contentWidth);
    }

    private boolean inlineParticipant(UiDomElement element) {
        return inlineFormatter().inlineParticipant(element);
    }

    private boolean hasInlineContent(UiDomElement element) {
        return inlineFormatter().hasInlineContent(element);
    }

    private float maxContentTextWidth(UiDomElement element) {
        return inlineFormatter().maxContentTextWidth(element);
    }

    private float minContentWidth(UiDomElement element, float reference) {
        return boxSizingEngine().minContentWidth(element, reference);
    }

    private float longestWordWidth(UiDomElement element) {
        return inlineFormatter().longestWordWidth(element);
    }

    private float fontSize(UiDomElement element, float scale) {
        return inlineFormatter().fontSize(element, scale);
    }

    private String fontId(UiDomElement element) {
        return inlineFormatter().fontId(element);
    }

    private float fontScale(UiDomElement element) {
        return inlineFormatter().fontScale(element);
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
        return clamp(element, "X", value, minWidth.read(element, UiCssLength.AUTO), maxWidth.read(element, UiCssLength.AUTO), reference);
    }

    private float clampHeight(UiDomElement element, float value, float reference) {
        return clamp(element, "Y", value, minHeight.read(element, UiCssLength.AUTO), maxHeight.read(element, UiCssLength.AUTO), reference);
    }

    private float clamp(UiDomElement element, String axis, float value, UiCssLength min, UiCssLength max, float reference) {
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

    private record Insets(float left, float top, float right, float bottom) { }


}
