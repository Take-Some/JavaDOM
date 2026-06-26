package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class OverflowXCssProperty extends UiCssKeywordPropertySpec<String> {
    public OverflowXCssProperty() {
        super("overflow-x", Set.of(), true, Map.of(
                "visible", "visible",
                "hidden", "hidden",
                "scroll", "scroll",
                "auto", "auto"
        ), "visible");
    }
}
