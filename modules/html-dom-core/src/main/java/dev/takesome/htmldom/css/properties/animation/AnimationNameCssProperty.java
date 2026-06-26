package dev.takesome.htmldom.css.properties.animation;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import java.util.Set;

public final class AnimationNameCssProperty extends UiCssStringPropertySpec {
    public AnimationNameCssProperty() {
        super("animation-name", Set.of(), true);
    }
}
