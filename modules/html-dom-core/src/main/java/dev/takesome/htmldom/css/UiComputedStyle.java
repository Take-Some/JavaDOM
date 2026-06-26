package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;

import java.util.Collections;
import java.util.Map;

/** Immutable view over an element computed style map. */
public record UiComputedStyle(Map<String, String> values) {
    public UiComputedStyle {
        values = Collections.unmodifiableMap(values == null ? Map.of() : Map.copyOf(values));
    }

    public static UiComputedStyle from(UiDomElement element) {
        if (element == null) return new UiComputedStyle(Map.of());
        return new UiComputedStyle(element.computedStyle());
    }

    public boolean has(String property) {
        return values.containsKey(normalize(property));
    }

    public String get(String property) {
        return get(property, "");
    }

    public String get(String property, String fallback) {
        String value = values.get(normalize(property));
        return value == null ? fallback : value;
    }

    public UiCssLength length(String property, UiCssLength fallback) {
        String value = get(property, "");
        if (value.isBlank()) return fallback;
        try {
            return UiCssLength.parse(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String normalize(String property) {
        if (property == null || property.isBlank()) throw new IllegalArgumentException("style property must not be blank");
        return property.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
