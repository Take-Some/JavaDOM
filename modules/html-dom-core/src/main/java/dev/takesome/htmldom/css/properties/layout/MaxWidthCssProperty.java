package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class MaxWidthCssProperty extends UiCssLengthPropertySpec {
    public MaxWidthCssProperty() {
        super("max-width", Set.of(), true);
    }
}
