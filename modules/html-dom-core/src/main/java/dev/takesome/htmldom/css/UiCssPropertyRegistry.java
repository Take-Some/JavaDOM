package dev.takesome.htmldom.css;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public final class UiCssPropertyRegistry {
    private final Map<String, UiCssPropertySpec> specs = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();
    private final Map<Class<?>, UiCssPropertySpec> byType = new LinkedHashMap<>();
    private boolean frozen;

    public static UiCssPropertyRegistry loadBuiltins() {
        UiCssPropertyRegistry registry = new UiCssPropertyRegistry();
        ServiceLoader.load(UiCssPropertySpec.class).forEach(registry::register);
        return registry.freeze();
    }

    public UiCssPropertyRegistry register(UiCssPropertySpec spec) {
        if (frozen) throw new UiCssException("CSS property registry is frozen");
        String name = normalize(spec.name());
        specs.put(name, spec);
        byType.put(spec.getClass(), spec);
        for (String alias : spec.aliases()) aliases.put(normalize(alias), name);
        return this;
    }

    public Optional<UiCssPropertySpec> find(String name) {
        return Optional.ofNullable(specs.get(canonicalName(name)));
    }

    public UiCssPropertySpec require(String name) {
        UiCssPropertySpec spec = specs.get(canonicalName(name));
        if (spec == null) throw new UiCssException("Unknown CSS property: " + name);
        return spec;
    }

    public <T extends UiCssPropertySpec> T requireType(Class<T> type) {
        UiCssPropertySpec spec = byType.get(type);
        if (spec == null) throw new UiCssException("CSS property spec is not registered: " + type.getName());
        return type.cast(spec);
    }

    public String canonicalName(String name) {
        String normalized = normalize(name);
        return aliases.getOrDefault(normalized, normalized);
    }

    public List<UiCssPropertySpec> definitions() {
        return List.copyOf(specs.values());
    }

    public List<String> attributeFallbackNames() {
        ArrayList<String> names = new ArrayList<>();
        for (UiCssPropertySpec spec : specs.values()) {
            if (!spec.attributeFallback()) continue;
            names.add(spec.name());
            names.addAll(spec.aliases());
        }
        return List.copyOf(names);
    }

    public UiCssPropertyRegistry freeze() {
        frozen = true;
        return this;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new UiCssException("CSS property name must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
