package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import dev.takesome.htmldom.css.UiPositionMode;
import java.util.Map;
import java.util.Set;

public final class PositionCssProperty extends UiCssKeywordPropertySpec<UiPositionMode> {
    public PositionCssProperty() {
        super("position", Set.of(), true, Map.of(
                "static", new UiPositionMode(false, false),
                "relative", new UiPositionMode(true, false),
                "absolute", new UiPositionMode(false, true),
                "fixed", new UiPositionMode(false, true),
                "sticky", new UiPositionMode(false, false)
        ), new UiPositionMode(false, false));
    }
}
