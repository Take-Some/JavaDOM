package dev.takesome.htmldom.css.units.unitTypes;

import java.util.Set;

import dev.takesome.htmldom.css.units.UiCssUnitAdapter;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;

public final class VwCssUnitAdapter implements UiCssUnitAdapter {
    public String unit() { return "vw"; }
    public Set<String> aliases() { return Set.of("wv"); }
    public float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback) {
        UiCssUnitResolutionContext safe = context == null ? UiCssUnitResolutionContext.defaults() : context;
        return safe.viewportWidth() * value / 100f;
    }
}
