package dev.takesome.htmldom.css.properties.paint;

import dev.takesome.htmldom.css.UiCssDeclaration;
import dev.takesome.htmldom.css.UiCssParseContext;
import dev.takesome.htmldom.css.UiCssShorthandPropertySpec;
import dev.takesome.htmldom.css.UiCssShorthandSupport;
import dev.takesome.htmldom.css.UiCssStringPropertySpec;

import java.util.List;
import java.util.Set;

public final class BorderRadiusCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {
    public BorderRadiusCssProperty() {
        super("border-radius", Set.of(), true);
    }

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {
        List<String> tokens = UiCssShorthandSupport.tokens(rawValue);
        if (tokens.isEmpty()) return List.of();
        String topLeft = tokens.get(0);
        String topRight = tokens.size() > 1 ? tokens.get(1) : topLeft;
        String bottomRight = tokens.size() > 2 ? tokens.get(2) : topLeft;
        String bottomLeft = tokens.size() > 3 ? tokens.get(3) : topRight;
        return List.of(
                new UiCssDeclaration("border-top-left-radius", topLeft),
                new UiCssDeclaration("border-top-right-radius", topRight),
                new UiCssDeclaration("border-bottom-right-radius", bottomRight),
                new UiCssDeclaration("border-bottom-left-radius", bottomLeft)
        );
    }
}
