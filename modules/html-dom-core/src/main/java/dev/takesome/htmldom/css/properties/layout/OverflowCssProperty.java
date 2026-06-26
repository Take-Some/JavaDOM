package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class OverflowCssProperty extends UiCssKeywordPropertySpec<String> {
    public OverflowCssProperty() {
        super("overflow", Set.of(), true, Map.of(
                "visible", "visible",
                "hidden", "hidden",
                "scroll", "scroll",
                "auto", "auto"
        ), "visible");
    }
}
