package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class ScrollLeftCssProperty extends UiCssStringPropertySpec {
    public ScrollLeftCssProperty() {
        super("scroll-left", Set.of("scroll-x"), true);
    }
}
