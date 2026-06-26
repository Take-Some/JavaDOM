package dev.takesome.htmldom.css.internal.parse;

import dev.takesome.htmldom.css.UiCssKeyframesRule;
import dev.takesome.htmldom.css.UiCssRule;
import dev.takesome.htmldom.css.UiFontFaceRule;
import dev.takesome.htmldom.css.UiStylesheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Stylesheet-level parser orchestration. No cascade, property registry or runtime style application. */
public final class UiCssStylesheetParser {
    private final UiCssCommentStripper comments;
    private final UiCssBlockScanner blocks;
    private final UiCssDeclarationParser declarations;
    private final UiCssSelectorListParser selectors;
    private final UiCssAtRuleParser atRules;

    public UiCssStylesheetParser() {
        this(
                new UiCssCommentStripper(),
                new UiCssBlockScanner(),
                new UiCssDeclarationParser(),
                new UiCssSelectorListParser()
        );
    }

    public UiCssStylesheetParser(
            UiCssCommentStripper comments,
            UiCssBlockScanner blocks,
            UiCssDeclarationParser declarations,
            UiCssSelectorListParser selectors
    ) {
        this.comments = comments == null ? new UiCssCommentStripper() : comments;
        this.blocks = blocks == null ? new UiCssBlockScanner() : blocks;
        this.declarations = declarations == null ? new UiCssDeclarationParser() : declarations;
        this.selectors = selectors == null ? new UiCssSelectorListParser() : selectors;
        this.atRules = new UiCssAtRuleParser(this.blocks, this.declarations, this.selectors);
    }

    public UiStylesheet parse(String source) {
        if (source == null || source.isBlank()) {
            return UiStylesheet.empty();
        }

        String css = comments.strip(source);
        ArrayList<UiCssRule> rules = new ArrayList<>();
        ArrayList<UiFontFaceRule> fontFaces = new ArrayList<>();
        LinkedHashMap<String, UiCssKeyframesRule> keyframes = new LinkedHashMap<>();

        int order = 0;
        int index = 0;
        while (index < css.length()) {
            index = blocks.skipWhitespace(css, index);
            if (index >= css.length()) {
                break;
            }

            UiCssBlock block = blocks.nextBlock(css, index);
            if (block == null) {
                break;
            }

            if (atRules.isFontFace(block.head())) {
                fontFaces.add(atRules.fontFace(block));
                index = block.nextIndex();
                continue;
            }
            if (atRules.isKeyframes(block.head())) {
                UiCssKeyframesRule rule = atRules.keyframes(block);
                if (!rule.empty()) {
                    keyframes.put(rule.name(), rule);
                }
                index = block.nextIndex();
                continue;
            }

            var declarationList = declarations.declarations(block.body());
            if (!block.head().isBlank() && !declarationList.isEmpty()) {
                for (String selector : selectors.split(block.head())) {
                    if (!selector.isBlank()) {
                        rules.add(new UiCssRule(selector, declarationList, order++));
                    }
                }
            }
            index = block.nextIndex();
        }

        return new UiStylesheet(rules, fontFaces, keyframes);
    }
}
