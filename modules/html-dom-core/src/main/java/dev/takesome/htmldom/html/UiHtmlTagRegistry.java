package dev.takesome.htmldom.html;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

public final class UiHtmlTagRegistry {
    private final Map<String, UiHtmlTagSpec> tags = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public static UiHtmlTagRegistry loadBuiltins() {
        UiHtmlTagRegistry registry = new UiHtmlTagRegistry();
        ServiceLoader.load(UiHtmlTagSpec.class).forEach(registry::register);
        return registry;
    }

    public UiHtmlTagRegistry register(UiHtmlTagSpec spec) {
        String name = normalize(spec.name());
        tags.put(name, spec);
        for (String alias : spec.aliases()) aliases.put(normalize(alias), name);
        return this;
    }

    public UiHtmlTagSpec require(String name) {
        UiHtmlTagSpec spec = find(name);
        if (spec == null) throw new UiHtmlException("Unknown HTML tag: " + name);
        return spec;
    }

    /**
     * Resolves a tag for tolerant runtime composition. Unknown or removed tags
     * fall back to the closest native HTML-like component instead of aborting
     * the whole UI scene. Strict callers should continue using {@link #require(String)}.
     */
    public UiHtmlTagSpec resolveOrFallback(String name) {
        UiHtmlTagSpec spec = find(name);
        if (spec != null) return spec;
        String fallback = fallbackTagName(name);
        UiHtmlTagSpec fallbackSpec = find(fallback);
        if (fallbackSpec != null) return fallbackSpec;
        return require("div");
    }

    public UiHtmlTagSpec find(String name) {
        if (name == null || name.isBlank()) return null;
        return tags.get(canonicalName(name));
    }

    public String canonicalName(String name) {
        String normalized = normalize(name);
        return aliases.getOrDefault(normalized, normalized);
    }

    private String fallbackTagName(String name) {
        if (name == null || name.isBlank()) return "div";
        String normalized = normalize(name);
        return switch (normalized) {
            case "screen" -> "body";
            case "panel" -> "section";
            case "text" -> "span";
            case "image" -> "img";
            case "popup" -> "dialog";
            case "menu-list" -> "nav";
            case "ribbon" -> "img";
            default -> "div";
        };
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new UiHtmlException("HTML tag name must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
