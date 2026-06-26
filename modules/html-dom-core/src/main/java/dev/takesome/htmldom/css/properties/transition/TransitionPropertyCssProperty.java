package dev.takesome.htmldom.css.properties.transition;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class TransitionPropertyCssProperty extends UiCssStringPropertySpec {
    public TransitionPropertyCssProperty() {
        super("transition-property", Set.of(), true);
    }
}
