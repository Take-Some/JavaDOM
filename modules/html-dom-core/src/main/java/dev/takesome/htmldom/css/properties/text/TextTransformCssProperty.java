package dev.takesome.htmldom.css.properties.text;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import java.util.Map;
import java.util.Set;

public final class TextTransformCssProperty extends UiCssKeywordPropertySpec<String> {
    public TextTransformCssProperty() {
        super("text-transform", Set.of(), true, Map.of(
                "none", "none",
                "uppercase", "uppercase",
                "lowercase", "lowercase",
                "capitalize", "capitalize"
        ), "none");
    }
}
