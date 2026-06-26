package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class BoxSizingCssProperty extends UiCssKeywordPropertySpec<String> {
    public BoxSizingCssProperty() {
        super("box-sizing", Set.of(), true, Map.of(
                "content-box", "content-box",
                "border-box", "border-box"
        ), "content-box");
    }
}
