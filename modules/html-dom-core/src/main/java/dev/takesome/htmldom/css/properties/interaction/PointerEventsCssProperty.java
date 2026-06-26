package dev.takesome.htmldom.css.properties.interaction;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class PointerEventsCssProperty extends UiCssStringPropertySpec {
    public PointerEventsCssProperty() {
        super("pointer-events", Set.of(), true);
    }
}
