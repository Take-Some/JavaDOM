package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.properties.layout.BoxSizingCssProperty;
import dev.takesome.htmldom.css.properties.layout.DisplayCssProperty;
import dev.takesome.htmldom.css.properties.layout.HeightCssProperty;
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
import dev.takesome.htmldom.css.properties.layout.WidthCssProperty;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;
import dev.takesome.htmldom.dom.UiDomText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Inline formatting context: fragments, whitespace collapsing, line wrapping and inline run boxes. */
final class UiCssInlineFormattingEngine {
    private final UiCssUnitResolutionContext lengthContext;
    private final DisplayCssProperty display;
    private final WidthCssProperty width;
    private final HeightCssProperty height;
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
    private final UiIntrinsicTextMeasurer textMeasurer;
    private final UiCssInlineRunTextHook inlineRunTextHook;
    private final IntrinsicChildrenWidth intrinsicChildrenWidth;
    private final DebugSink debugSink;
    private final WarnSink warnSink;

    UiCssInlineFormattingEngine(
            UiCssUnitResolutionContext lengthContext,
            DisplayCssProperty display,
            WidthCssProperty width,
            HeightCssProperty height,
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
            UiIntrinsicTextMeasurer textMeasurer,
            UiCssInlineRunTextHook inlineRunTextHook,
            IntrinsicChildrenWidth intrinsicChildrenWidth,
            DebugSink debugSink,
            WarnSink warnSink
    ) {
        this.lengthContext = lengthContext == null ? UiCssUnitResolutionContext.defaults() : lengthContext;
        this.display = display;
        this.width = width;
        this.height = height;
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
        this.textMeasurer = textMeasurer == null ? UiIntrinsicTextMeasurer.heuristic() : textMeasurer;
        this.inlineRunTextHook = inlineRunTextHook == null ? UiCssInlineRunTextHook.identity() : inlineRunTextHook;
        this.intrinsicChildrenWidth = intrinsicChildrenWidth == null ? (element, reference) -> 0f : intrinsicChildrenWidth;
        this.debugSink = debugSink == null ? (key, message, args) -> { } : debugSink;
        this.warnSink = warnSink == null ? (key, message, args) -> { } : warnSink;
    }

    void commitTextLines(UiDomElement element, UiCssBox box, UiCssLayoutResult result) {
        InlineLayout inline = layoutInlineContent(element, box);
        if (!inline.lines.isEmpty()) {
            result.putLineBoxes(element, inline.lines);
            result.putInlineBoxes(element, inline.runs);
            debugSink.debug("inline-formatting|" + summary(element) + '|' + inline.lines.size() + '|' + inline.runs.size(),
                    "UI CSS inline formatting element={} lines={} runs={} box={}x{}", summary(element), inline.lines.size(), inline.runs.size(), box.width(), box.height());
        }
    }

    float textBlockHeight(UiDomElement element, float contentWidth) {
        float out = 0f;
        for (InlineLine line : wrapInlineLines(element, Math.max(1f, contentWidth))) out += Math.max(1f, line.height);
        return out;
    }

    boolean hasInlineContent(UiDomElement element) {
        for (InlineFragment fragment : inlineFragments(element)) {
            if (fragment.atomic || (fragment.text != null && !fragment.text.isBlank())) return true;
        }
        return false;
    }

    float maxContentTextWidth(UiDomElement element) {
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

    float longestWordWidth(UiDomElement element) {
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

    boolean inlineParticipant(UiDomElement element) {
        if (element == null) return false;
        UiDisplayMode mode = display.read(element);
        return !mode.hidden() && mode.inlineLevel();
    }

    float fontSize(UiDomElement element, float scale) {
        String raw = firstStyle(element, "font-size");
        if (!raw.isBlank()) {
            try {
                return Math.max(1f, UiCssLength.parse(raw).resolve(lengthContext, 1f, 16f) * Math.max(0.01f, scale));
            } catch (RuntimeException ignored) {
            }
        }
        return (titleFont(element) ? 32f : 14f) * Math.max(0.01f, scale);
    }

    String fontId(UiDomElement element) {
        String raw = firstStyle(element, "font-family", "font");
        return UiCssFontFamilyResolver.resolveEngineFontId(raw.isBlank() ? UiCssFontFamilyResolver.DEFAULT_STACK : raw, element.computedStyle());
    }

    float fontScale(UiDomElement element) {
        String scale = firstStyle(element, "scale", "font-scale");
        if (!scale.isBlank()) {
            try { return Math.max(0.01f, Float.parseFloat(scale.trim())); }
            catch (RuntimeException ignored) { }
        }
        return 1f;
    }

    private InlineLayout layoutInlineContent(UiDomElement element, UiCssBox box) {
        BoxInsets p = padding(element, box);
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
            warnSink.warn(
                    "inline-run-text-hook|" + summary(owner) + '|' + error.getClass().getName() + '|' + error.getMessage(),
                    "UI CSS inline run text hook failed element={} line={} run={} reason='{}'; using original inline text",
                    summary(owner), lineIndex, runIndex, error.getMessage()
            );
            return fallback;
        }
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

    private boolean atomicInline(UiDomElement element) {
        if (element == null) return false;
        UiDisplayMode mode = display.read(element);
        return !mode.hidden()
                && mode.inlineLevel()
                && (mode.atomicInline() || replacedInline(element) || inlineBoxMetrics(element));
    }

    private boolean inlineBlock(UiDomElement element) {
        return element != null && display.read(element).atomicInline();
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
        for (String token : value.split("\\s+")) {
            String part = token.trim();
            if (part.isBlank() || part.equals("none") || part.equals("solid") || part.equals("transparent")) continue;
            try {
                String numeric = part.replaceAll("[a-z%]+$", "");
                if (numeric.isBlank() || numeric.equals("+") || numeric.equals("-")) continue;
                if (Math.abs(Float.parseFloat(numeric)) > 0.0001f) return true;
            } catch (RuntimeException ignored) {
                return true;
            }
        }
        return false;
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
        BoxInsets p = padding(element, reference);
        BoxInsets m = margin(element, reference);
        String raw = width.raw(element);
        boolean explicit = !raw.isBlank() && !intrinsicWidthRequested(element, raw);
        float contentWidth;
        if (explicit) {
            try { contentWidth = Math.max(1f, UiCssLength.parse(raw).resolve(lengthContext, reference.width(), fontSize)); }
            catch (RuntimeException ignored) { contentWidth = Math.max(1f, fontSize); }
        } else if (replacedInline(element) && !inlineBlock(element) && !inlineBoxMetrics(element)) {
            contentWidth = Math.max(1f, fontSize * 1.12f);
        } else {
            contentWidth = Math.max(1f, Math.max(maxContentTextWidth(element), intrinsicChildrenWidth.resolve(element, Math.max(1f, fontSize * 12f))));
        }
        float borderBoxWidth = explicit && !"content-box".equals(boxSizing.read(element))
                ? contentWidth
                : contentWidth + p.left + p.right;
        return Math.max(1f, borderBoxWidth + m.left + m.right);
    }

    private float atomicInlineHeight(UiDomElement element, float fontSize, float fallbackLineHeight) {
        UiCssBox reference = inlineBoxReference(fontSize, fallbackLineHeight);
        BoxInsets p = padding(element, reference);
        BoxInsets m = margin(element, reference);
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

    private boolean titleFont(UiDomElement element) {
        String font = firstStyle(element, "font-family", "font").toLowerCase(Locale.ROOT);
        String tag = element.tagName();
        return font.contains("title") || font.contains("pixel") || "h1".equals(tag) || "h2".equals(tag);
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

    private boolean outOfFlow(UiDomElement element) {
        String pos = firstStyle(element, "position").toLowerCase(Locale.ROOT);
        return pos.equals("absolute") || pos.equals("fixed") || pos.equals("sticky");
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

    private String firstStyle(UiDomElement element, String... names) {
        for (String name : names) {
            String value = element.style(name, "");
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String summary(UiDomElement element) {
        if (element == null) return "<null>";
        String id = element.id();
        return element.tagName() + (id.isBlank() ? "" : "#" + id);
    }

    @FunctionalInterface
    interface IntrinsicChildrenWidth {
        float resolve(UiDomElement element, float reference);
    }

    @FunctionalInterface
    interface DebugSink {
        void debug(String key, String message, Object... args);
    }

    @FunctionalInterface
    interface WarnSink {
        void warn(String key, String message, Object... args);
    }

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

    private record BoxInsets(float left, float top, float right, float bottom) { }
}
