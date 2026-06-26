package dev.takesome.htmldom.css.internal.parse;

import dev.takesome.htmldom.css.UiCssKeyframe;
import dev.takesome.htmldom.css.UiCssKeyframesRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parses @keyframes body blocks into offset/declaration descriptors. */
public final class UiCssKeyframesParser {
    private final UiCssBlockScanner blocks;
    private final UiCssDeclarationParser declarations;
    private final UiCssSelectorListParser selectors;

    public UiCssKeyframesParser(
            UiCssBlockScanner blocks,
            UiCssDeclarationParser declarations,
            UiCssSelectorListParser selectors
    ) {
        this.blocks = blocks == null ? new UiCssBlockScanner() : blocks;
        this.declarations = declarations == null ? new UiCssDeclarationParser() : declarations;
        this.selectors = selectors == null ? new UiCssSelectorListParser() : selectors;
    }

    public UiCssKeyframesRule parse(String name, String block) {
        LinkedHashMap<Double, LinkedHashMap<String, String>> frames = new LinkedHashMap<>();
        int index = 0;
        while (block != null && index < block.length()) {
            index = blocks.skipWhitespace(block, index);
            if (index >= block.length()) {
                break;
            }
            UiCssBlock frameBlock = blocks.nextBlock(block, index);
            if (frameBlock == null) {
                break;
            }
            Map<String, String> frameDeclarations = declarations.declarationMap(frameBlock.body());
            for (String selector : selectors.split(frameBlock.head())) {
                double offset = frameOffset(selector);
                frames.computeIfAbsent(offset, ignored -> new LinkedHashMap<>()).putAll(frameDeclarations);
            }
            index = frameBlock.nextIndex();
        }

        ArrayList<UiCssKeyframe> out = new ArrayList<>();
        frames.forEach((offset, frameDeclarations) -> out.add(new UiCssKeyframe(offset, frameDeclarations)));
        return new UiCssKeyframesRule(name, out);
    }

    private double frameOffset(String selector) {
        if (selector == null || selector.isBlank()) {
            return 0.0;
        }
        String value = selector.trim().toLowerCase(java.util.Locale.ROOT);
        if ("from".equals(value)) {
            return 0.0;
        }
        if ("to".equals(value)) {
            return 1.0;
        }
        if (value.endsWith("%")) {
            try {
                double percentage = Double.parseDouble(value.substring(0, value.length() - 1).trim());
                return Math.max(0.0, Math.min(1.0, percentage / 100.0));
            } catch (RuntimeException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
