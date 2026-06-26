package dev.takesome.htmldom.html;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

public final class UiHtmlAttributeRegistry {
    private final Map<String, UiHtmlAttributeSpec> attributes = new LinkedHashMap<>();

    public static UiHtmlAttributeRegistry loadBuiltins() {
        UiHtmlAttributeRegistry registry = new UiHtmlAttributeRegistry();
        ServiceLoader.load(UiHtmlAttributeSpec.class).forEach(registry::register);
        return registry;
    }

    public UiHtmlAttributeRegistry register(UiHtmlAttributeSpec spec) {
        attributes.put(normalize(spec.name()), spec);
        return this;
    }

    public UiHtmlAttributeSpec require(String name) {
        UiHtmlAttributeSpec spec = find(name);
        if (spec == null) throw new UiHtmlException("Unknown HTML attribute: " + name);
        return spec;
    }

    public UiHtmlAttributeSpec find(String name) {
        String normalized = normalize(name);
        UiHtmlAttributeSpec spec = attributes.get(normalized);
        if (spec != null) return spec;
        for (Map.Entry<String, UiHtmlAttributeSpec> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (!key.endsWith("*")) continue;
            String prefix = key.substring(0, key.length() - 1);
            if (normalized.startsWith(prefix)) return entry.getValue();
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new UiHtmlException("HTML attribute name must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
