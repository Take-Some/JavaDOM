package dev.takesome.htmldom.css.properties.interaction;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class CursorCssProperty extends UiCssKeywordPropertySpec<String> {
    public CursorCssProperty() {
        super("cursor", Set.of(), true, Map.of(
                "default", "default",
                "pointer", "pointer",
                "text", "text",
                "move", "move",
                "resize", "resize"
        ), "default");
    }
}
