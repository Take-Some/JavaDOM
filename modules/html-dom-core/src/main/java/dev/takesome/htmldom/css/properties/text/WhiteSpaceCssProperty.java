package dev.takesome.htmldom.css.properties.text;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class WhiteSpaceCssProperty extends UiCssKeywordPropertySpec<String> {
    public WhiteSpaceCssProperty() {
        super("white-space", Set.of(), true, Map.of(
                "normal", "normal",
                "nowrap", "nowrap",
                "pre", "pre",
                "pre-wrap", "pre-wrap",
                "pre-line", "pre-line"
        ), "normal");
    }
}
