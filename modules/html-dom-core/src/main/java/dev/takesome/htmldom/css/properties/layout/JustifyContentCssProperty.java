package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class JustifyContentCssProperty extends UiCssKeywordPropertySpec<String> {
    public JustifyContentCssProperty() {
        super("justify-content", Set.of(), true, Map.of(
                "flex-start", "flex-start",
                "center", "center",
                "flex-end", "flex-end",
                "space-between", "space-between",
                "space-around", "space-around"
        ), "flex-start");
    }
}
