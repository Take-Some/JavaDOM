package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssKeywordPropertySpec;
import dev.takesome.htmldom.css.UiFlexDirection;
import java.util.Map;
import java.util.Set;

public final class FlexDirectionCssProperty extends UiCssKeywordPropertySpec<UiFlexDirection> {
    public FlexDirectionCssProperty() {
        super("flex-direction", Set.of(), true, Map.of(
                "row", new UiFlexDirection(true),
                "row-reverse", new UiFlexDirection(true),
                "column", new UiFlexDirection(false),
                "column-reverse", new UiFlexDirection(false)
        ), new UiFlexDirection(true));
    }
}
