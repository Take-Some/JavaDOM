package dev.takesome.htmldom.css.internal.parse;

import dev.takesome.htmldom.css.UiCssKeyframesRule;
import dev.takesome.htmldom.css.UiFontFaceRule;

import java.util.Map;

/** Parses supported CSS at-rules into engine metadata descriptors. */
public final class UiCssAtRuleParser {
    private final UiCssDeclarationParser declarations;
    private final UiCssKeyframesParser keyframes;

    public UiCssAtRuleParser(
            UiCssBlockScanner blocks,
            UiCssDeclarationParser declarations,
            UiCssSelectorListParser selectors
    ) {
        this.declarations = declarations == null ? new UiCssDeclarationParser() : declarations;
        this.keyframes = new UiCssKeyframesParser(
                blocks == null ? new UiCssBlockScanner() : blocks,
                this.declarations,
                selectors == null ? new UiCssSelectorListParser() : selectors
        );
    }

    public boolean isFontFace(String head) {
        return head != null && head.trim().startsWith("@font-face");
    }

    public boolean isKeyframes(String head) {
        if (head == null) {
            return false;
        }
        String value = head.trim();
        return value.startsWith("@keyframes") || value.startsWith("@-webkit-keyframes");
    }

    public UiFontFaceRule fontFace(UiCssBlock block) {
        Map<String, String> map = declarations.declarationMap(block.body());
        return new UiFontFaceRule(
                map.getOrDefault("font-family", ""),
                map.getOrDefault("src", ""),
                parseInt(map.get("font-weight"), 400),
                map.getOrDefault("font-style", "normal"),
                map
        );
    }

    public UiCssKeyframesRule keyframes(UiCssBlock block) {
        return keyframes.parse(frameName(block.head()), block.body());
    }

    private String frameName(String head) {
        return (head == null ? "" : head)
                .replace("@-webkit-keyframes", "")
                .replace("@keyframes", "")
                .trim();
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
