package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class OverflowYCssProperty extends UiCssKeywordPropertySpec<String> {
    public OverflowYCssProperty() {
        super("overflow-y", Set.of(), true, Map.of(
                "visible", "visible",
                "hidden", "hidden",
                "scroll", "scroll",
                "auto", "auto"
        ), "visible");
    }
}
