package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import dev.takesome.htmldom.css.UiDisplayMode;
import java.util.Map;
import java.util.Set;

public final class DisplayCssProperty extends UiCssKeywordPropertySpec<UiDisplayMode> {
    public DisplayCssProperty() {
        super("display", Set.of(), true, Map.of(
                "none", new UiDisplayMode(true, false),
                "block", new UiDisplayMode(false, false),
                "inline", new UiDisplayMode(false, false),
                "inline-block", new UiDisplayMode(false, false),
                "flex", new UiDisplayMode(false, true)
        ), new UiDisplayMode(false, false));
    }
}
