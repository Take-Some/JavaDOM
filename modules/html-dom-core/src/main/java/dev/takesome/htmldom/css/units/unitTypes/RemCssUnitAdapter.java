package dev.takesome.htmldom.css.units.unitTypes;

import dev.takesome.htmldom.css.units.UiCssUnitAdapter;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;

public final class RemCssUnitAdapter implements UiCssUnitAdapter {
    public String unit() { return "rem"; }
    public float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback) {
        UiCssUnitResolutionContext safe = context == null ? UiCssUnitResolutionContext.defaults() : context;
        return value * safe.rootFontSize();
    }
}
