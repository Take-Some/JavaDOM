package dev.takesome.htmldom.css.units;

import java.util.Set;

/** Pluggable adapter for one CSS length unit. */
public interface UiCssUnitAdapter {
    String unit();

    default Set<String> aliases() {
        return Set.of();
    }

    float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback);

    default String cssText(float value) {
        return trim(value) + unit();
    }

    private static String trim(float value) {
        if (value == Math.rint(value)) return Integer.toString(Math.round(value));
        return Float.toString(value);
    }
}
