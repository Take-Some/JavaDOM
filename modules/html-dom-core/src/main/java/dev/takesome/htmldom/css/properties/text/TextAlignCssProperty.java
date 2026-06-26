package dev.takesome.htmldom.css.properties.text;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class TextAlignCssProperty extends UiCssStringPropertySpec {
    public TextAlignCssProperty() {
        super("text-align", Set.of("align"), true);
    }
}
