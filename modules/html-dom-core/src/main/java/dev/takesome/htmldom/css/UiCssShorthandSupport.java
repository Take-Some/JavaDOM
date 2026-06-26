package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.lowerTrimToEmpty;
import java.util.List;
import java.util.Locale;

/** Small tokenizer helpers for CSS shorthand definition files. */
public final class UiCssShorthandSupport {
    private UiCssShorthandSupport() {
    }

    public static List<String> tokens(String raw) {
        return UiCssTokenSplitter.splitTopLevelWhitespace(raw);
    }

    public static String lower(String value) {
        return lowerTrimToEmpty(value, Locale.ROOT);
    }

    public static boolean lengthLike(String value) {
        String v = lower(value);
        if (v.isBlank()) return false;
        if (v.endsWith("px") || v.endsWith("%") || v.endsWith("em") || v.endsWith("rem")) return true;
        return v.matches("[-+]?[0-9]*\\.?[0-9]+");
    }

    public static boolean colorLike(String value) {
        String v = lower(value);
        return v.startsWith("#") || v.startsWith("rgb(") || v.startsWith("rgba(") || "transparent".equals(v) || "white".equals(v) || "black".equals(v) || "red".equals(v) || "green".equals(v) || "blue".equals(v) || "yellow".equals(v) || "orange".equals(v) || "purple".equals(v) || "gray".equals(v) || "grey".equals(v);
    }
}
