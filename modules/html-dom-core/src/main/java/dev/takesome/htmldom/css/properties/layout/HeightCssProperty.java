package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssLengthPropertySpec;
import java.util.Set;

public final class HeightCssProperty extends UiCssLengthPropertySpec {
    public HeightCssProperty() {
        super("height", Set.of("h"), true);
    }
}
