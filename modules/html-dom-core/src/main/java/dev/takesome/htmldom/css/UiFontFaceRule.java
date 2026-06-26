package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import java.util.Collections;
import java.util.Map;

/** Parsed @font-face declaration. */
public record UiFontFaceRule(String family, String source, int weight, String style, Map<String, String> declarations) {
    public UiFontFaceRule {
        family = stripQuotes(trimToEmpty(family));
        source = stripUrl(trimToEmpty(source));
        weight = weight <= 0 ? 400 : weight;
        style = style == null || style.isBlank() ? "normal" : style.trim().toLowerCase(java.util.Locale.ROOT);
        declarations = Collections.unmodifiableMap(declarations == null ? Map.of() : Map.copyOf(declarations));
    }

    private static String stripQuotes(String value) {
        if (value.length() < 2) return value;
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String stripUrl(String value) {
        String out = value.trim();
        if (out.startsWith("url(") && out.endsWith(")")) out = out.substring(4, out.length() - 1).trim();
        return stripQuotes(out);
    }
}
