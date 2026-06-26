package dev.takesome.htmldom.css.properties.paint;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class BackgroundColorCssProperty extends UiCssStringPropertySpec {
    public BackgroundColorCssProperty() {
        super("back" + "ground-color", Set.of(), true);
    }
}
