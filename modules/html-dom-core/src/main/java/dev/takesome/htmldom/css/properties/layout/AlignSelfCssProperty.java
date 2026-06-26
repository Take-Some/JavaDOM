package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class AlignSelfCssProperty extends UiCssKeywordPropertySpec<String> {
    public AlignSelfCssProperty() {
        super("align-self", Set.of(), true, Map.of(
                "auto", "auto",
                "stretch", "stretch",
                "flex-start", "flex-start",
                "center", "center",
                "flex-end", "flex-end"
        ), "auto");
    }
}
