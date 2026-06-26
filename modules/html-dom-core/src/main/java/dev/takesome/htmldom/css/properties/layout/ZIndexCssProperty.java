package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

/** CSS stacking order property. */
public final class ZIndexCssProperty extends UiCssStringPropertySpec {
    public ZIndexCssProperty() {
        super("z-index", Set.of("z"), true);
    }
}
