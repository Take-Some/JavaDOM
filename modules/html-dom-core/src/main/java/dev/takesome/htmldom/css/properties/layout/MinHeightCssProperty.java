package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class MinHeightCssProperty extends UiCssLengthPropertySpec {
    public MinHeightCssProperty() {
        super("min-height", Set.of(), true);
    }
}
