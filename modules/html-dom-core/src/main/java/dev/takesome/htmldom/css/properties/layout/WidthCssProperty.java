package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class WidthCssProperty extends UiCssLengthPropertySpec {
    public WidthCssProperty() {
        super("width", Set.of("w"), true);
    }
}
