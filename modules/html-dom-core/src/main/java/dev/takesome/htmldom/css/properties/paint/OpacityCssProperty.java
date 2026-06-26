package dev.takesome.htmldom.css.properties.paint;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class OpacityCssProperty extends UiCssStringPropertySpec {
    public OpacityCssProperty() {
        super("opacity", Set.of(), true);
    }
}
