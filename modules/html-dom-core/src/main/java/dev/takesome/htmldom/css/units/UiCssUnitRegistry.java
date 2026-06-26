package dev.takesome.htmldom.css.units;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.lowerTrimToEmpty;
import dev.takesome.htmldom.css.units.unitTypes.AutoCssUnitAdapter;
import dev.takesome.htmldom.css.units.unitTypes.PxCssUnitAdapter;
import dev.takesome.htmldom.css.units.unitTypes.PercentCssUnitAdapter;
import dev.takesome.htmldom.css.units.unitTypes.PtCssUnitAdapter;
import dev.takesome.htmldom.css.units.unitTypes.RemCssUnitAdapter;
import dev.takesome.htmldom.css.units.unitTypes.VwCssUnitAdapter;
import dev.takesome.htmldom.css.units.unitTypes.VhCssUnitAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/** Registry-backed CSS length unit parser/resolver. */
public final class UiCssUnitRegistry {
    private static final UiCssUnitRegistry BUILTINS = loadBuiltins();

    private final LinkedHashMap<String, UiCssUnitAdapter> adapters = new LinkedHashMap<>();

    public static UiCssUnitRegistry builtins() {
        return BUILTINS;
    }

    public static UiCssUnitRegistry loadBuiltins() {
        UiCssUnitRegistry registry = new UiCssUnitRegistry();
        registry.register(new AutoCssUnitAdapter());
        registry.register(new PxCssUnitAdapter());
        registry.register(new PercentCssUnitAdapter());
        registry.register(new PtCssUnitAdapter());
        registry.register(new RemCssUnitAdapter());
        registry.register(new VwCssUnitAdapter());
        registry.register(new VhCssUnitAdapter());
        ServiceLoader.load(UiCssUnitAdapter.class).forEach(registry::register);
        UiCssUnitDiagnostics.debugOnce("unit-registry-loaded", "UI CSS unit registry loaded units={}", registry.adapters.keySet());
        return registry;
    }

    public UiCssUnitRegistry register(UiCssUnitAdapter adapter) {
        if (adapter == null) return this;
        put(adapter.unit(), adapter);
        for (String alias : adapter.aliases()) put(alias, adapter);
        return this;
    }

    public Optional<UiCssUnitAdapter> find(String unit) {
        if (unit == null) return Optional.empty();
        return Optional.ofNullable(adapters.get(normalize(unit)));
    }

    public UiCssUnitAdapter require(String unit) {
        return find(unit).orElseThrow(() -> new IllegalArgumentException("Unknown CSS unit: " + unit));
    }

    public ParsedUnit split(String raw) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank()) return new ParsedUnit("", "auto");
        if ("auto".equals(value)) return new ParsedUnit("0", "auto");
        for (String suffix : suffixesByLength()) {
            if (suffix.isBlank()) continue;
            if (value.endsWith(suffix)) {
                String number = value.substring(0, value.length() - suffix.length()).trim();
                if (number.isBlank()) break;
                return new ParsedUnit(number, suffix);
            }
        }
        return new ParsedUnit(value, "px");
    }

    private List<String> suffixesByLength() {
        ArrayList<String> out = new ArrayList<>(adapters.keySet());
        out.remove("auto");
        out.sort(Comparator.comparingInt(String::length).reversed());
        return out;
    }

    private void put(String unit, UiCssUnitAdapter adapter) {
        String key = normalize(unit);
        if (key.isBlank()) return;
        UiCssUnitAdapter previous = adapters.get(key);
        if (previous != null && previous.getClass().equals(adapter.getClass())) {
            return;
        }
        adapters.put(key, adapter);
        if (previous != null) {
            UiCssUnitDiagnostics.warnOnce("unit-override:" + key, "UI CSS unit adapter override unit='{}' previous={} next={}", key, previous.getClass().getName(), adapter.getClass().getName());
        }
    }

    private String normalize(String value) {
        return lowerTrimToEmpty(value, Locale.ROOT);
    }

    public record ParsedUnit(String number, String unit) { }
}
