package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.units.UiCssUnitAdapter;
import dev.takesome.htmldom.css.units.UiCssUnitRegistry;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;

import java.util.Locale;

/** Registry-backed CSS length value. */
public record UiCssLength(float value, String unit) {
    public static final UiCssLength AUTO = new UiCssLength(0f, "auto");
    public static final UiCssLength ZERO = new UiCssLength(0f, "px");

    public UiCssLength {
        unit = normalize(unit);
    }

    public static UiCssLength parse(String raw) {
        if (raw == null || raw.isBlank()) return AUTO;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        UiCssUnitRegistry.ParsedUnit parsed = UiCssUnitRegistry.builtins().split(value);
        if ("auto".equals(parsed.unit())) return AUTO;
        if (UiCssUnitRegistry.builtins().find(parsed.unit()).isEmpty()) {
            throw new IllegalArgumentException("Unknown CSS length unit: " + parsed.unit() + " in `" + raw + "`");
        }
        return new UiCssLength(parseFloat(parsed.number()), parsed.unit());
    }

    public boolean auto() {
        return "auto".equals(unit);
    }

    public float resolve(float reference, float fallback) {
        return resolve(UiCssUnitResolutionContext.defaults(), reference, fallback);
    }

    public float resolve(UiCssUnitResolutionContext context, float reference, float fallback) {
        UiCssUnitAdapter adapter = UiCssUnitRegistry.builtins().require(unit);
        return adapter.resolve(value, context == null ? UiCssUnitResolutionContext.defaults() : context, reference, fallback);
    }

    public String cssText() {
        return UiCssUnitRegistry.builtins().require(unit).cssText(value);
    }

    private static float parseFloat(String raw) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid CSS length: " + raw, ex);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("CSS unit must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
