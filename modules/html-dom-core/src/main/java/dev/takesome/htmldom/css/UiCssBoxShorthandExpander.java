package dev.takesome.htmldom.css;

import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Expands CSS box-model shorthands for raw style maps used after parsing. */
public final class UiCssBoxShorthandExpander {
    private UiCssBoxShorthandExpander() {
    }

    public static LinkedHashMap<String, String> expand(Map<String, String> source) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) return out;
        source.forEach((property, value) -> putExpanded(out, property, value));
        return out;
    }

    public static void putExpanded(Map<String, String> target, String property, String value) {
        if (target == null) return;
        String key = normalizeProperty(property);
        String safeValue = trimToEmpty(value);
        if (key.isBlank() || safeValue.isBlank()) return;

        target.put(key, safeValue);
        if ("margin".equals(key) || "padding".equals(key)) expandBoxEdges(target, key, safeValue);
    }

    private static void expandBoxEdges(Map<String, String> target, String prefix, String value) {
        List<String> tokens = UiCssShorthandSupport.tokens(value);
        if (tokens.isEmpty()) return;
        String top = tokens.get(0);
        String right = tokens.size() > 1 ? tokens.get(1) : top;
        String bottom = tokens.size() > 2 ? tokens.get(2) : top;
        String left = tokens.size() > 3 ? tokens.get(3) : right;

        target.put(prefix + "-top", top);
        target.put(prefix + "-right", right);
        target.put(prefix + "-bottom", bottom);
        target.put(prefix + "-left", left);
    }

    private static String normalizeProperty(String property) {
        return trimToEmpty(property).toLowerCase(Locale.ROOT);
    }
}
