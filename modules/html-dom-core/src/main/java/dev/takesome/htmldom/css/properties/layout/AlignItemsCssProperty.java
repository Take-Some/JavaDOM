package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class AlignItemsCssProperty extends UiCssKeywordPropertySpec<String> {
    public AlignItemsCssProperty() {
        super("align-items", Set.of(), true, Map.of(
                "stretch", "stretch",
                "flex-start", "flex-start",
                "center", "center",
                "flex-end", "flex-end"
        ), "stretch");
    }
}
