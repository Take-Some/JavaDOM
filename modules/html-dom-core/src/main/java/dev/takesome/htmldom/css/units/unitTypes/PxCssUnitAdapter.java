package dev.takesome.htmldom.css.units.unitTypes;

import dev.takesome.htmldom.css.units.UiCssUnitAdapter;
import dev.takesome.htmldom.css.units.UiCssUnitResolutionContext;

public final class PxCssUnitAdapter implements UiCssUnitAdapter {
    public String unit() { return "px"; }
    public float resolve(float value, UiCssUnitResolutionContext context, float reference, float fallback) { return value; }
}
