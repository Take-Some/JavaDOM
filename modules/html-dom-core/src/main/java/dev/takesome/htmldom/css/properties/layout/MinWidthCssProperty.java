package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class MinWidthCssProperty extends UiCssLengthPropertySpec {
    public MinWidthCssProperty() {
        super("min-width", Set.of(), true);
    }
}
