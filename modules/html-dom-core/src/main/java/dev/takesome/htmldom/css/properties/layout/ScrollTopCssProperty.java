package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class ScrollTopCssProperty extends UiCssStringPropertySpec {
    public ScrollTopCssProperty() {
        super("scroll-top", Set.of("scroll-y"), true);
    }
}
