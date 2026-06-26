package dev.takesome.htmldom.css;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Resolves CSS font-family stacks into engine font-role ids. */
public final class UiCssFontFamilyResolver {
    public static final String DEFAULT_STACK = "system-ui, sans-serif";
    public static final String DEFAULT_FONT_ID = "default";

    private static final Set<String> SANS_SERIF = Set.of(
            "system-ui", "ui-sans-serif", "sans-serif", "-apple-system", "blinkmacsystemfont", "default"
    );
    private static final Set<String> SERIF = Set.of("serif", "ui-serif");
    private static final Set<String> FIXED_WIDTH = Set.of("monospace", "ui-monospace", "monaco", "consolas", "courier", "courier new");

    private UiCssFontFamilyResolver() {
    }

    public static String resolveEngineFontId(String rawFamily, Map<String, String> style) {
        String base = firstResolvedFamily(rawFamily);
        return withFaceStyle(base, style);
    }

    public static String firstResolvedFamily(String rawFamily) {
        for (String family : families(rawFamily)) {
            String resolved = genericFamily(family);
            if (!resolved.isBlank()) return resolved;
            if (!family.isBlank()) return family;
        }
        return DEFAULT_FONT_ID;
    }

    public static List<String> families(String rawFamily) {
        String source = rawFamily == null || rawFamily.isBlank() ? DEFAULT_STACK : rawFamily.trim();
        ArrayList<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == 39 || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == ',') {
                addFamily(out, current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        addFamily(out, current.toString());
        if (out.isEmpty()) out.add(DEFAULT_FONT_ID);
        return List.copyOf(out);
    }

    private static String genericFamily(String family) {
        String normalized = normalizeFamily(family);
        if (SANS_SERIF.contains(normalized) || SERIF.contains(normalized) || FIXED_WIDTH.contains(normalized)) {
            return DEFAULT_FONT_ID;
        }
        return "";
    }

    private static String withFaceStyle(String base, Map<String, String> style) {
        String safeBase = base == null || base.isBlank() ? DEFAULT_FONT_ID : base.trim();
        String weight = value(style, "font-weight").toLowerCase(Locale.ROOT);
        String fontStyle = value(style, "font-style").toLowerCase(Locale.ROOT);
        boolean bold = "bold".equals(weight) || "bolder".equals(weight) || "700".equals(weight) || "800".equals(weight) || "900".equals(weight);
        boolean italic = "italic".equals(fontStyle) || "oblique".equals(fontStyle);
        if (!bold && !italic) return safeBase;
        String suffix = bold && italic ? "Bold Italic" : bold ? "Bold" : "Italic";
        String normalized = safeBase.toLowerCase(Locale.ROOT);
        return normalized.endsWith(" " + suffix.toLowerCase(Locale.ROOT)) ? safeBase : safeBase + " " + suffix;
    }

    private static void addFamily(ArrayList<String> out, String raw) {
        String family = stripQuotes(raw);
        if (!family.isBlank()) out.add(family);
    }

    private static String normalizeFamily(String raw) {
        return stripQuotes(raw).replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String stripQuotes(String value) {
        String out = value == null ? "" : value.trim();
        while (out.length() >= 2) {
            char first = out.charAt(0);
            char last = out.charAt(out.length() - 1);
            if ((first == '"' && last == '"') || (first == 39 && last == 39)) {
                out = out.substring(1, out.length() - 1).trim();
                continue;
            }
            break;
        }
        return out;
    }

    private static String value(Map<String, String> style, String key) {
        if (style == null || key == null) return "";
        return style.getOrDefault(key, "").trim();
    }
}
