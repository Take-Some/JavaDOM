package dev.takesome.htmldom.css.properties.interaction;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class VisibilityCssProperty extends UiCssStringPropertySpec {
    public VisibilityCssProperty() {
        super("visibility", Set.of(), true);
    }
}
