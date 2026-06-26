package dev.takesome.htmldom.css.properties.paint;

import dev.takesome.htmldom.css.UiCssDeclaration;
import dev.takesome.htmldom.css.UiCssParseContext;
import dev.takesome.htmldom.css.UiCssShorthandPropertySpec;
import dev.takesome.htmldom.css.UiCssShorthandSupport;
import dev.takesome.htmldom.css.UiCssStringPropertySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BorderCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {
    private static final Set<String> STYLES = Set.of("none", "hidden", "dotted", "dashed", "solid", "double", "groove", "ridge", "inset", "outset");
    private static final Set<String> WIDTHS = Set.of("thin", "medium", "thick");

    public BorderCssProperty() {
        super("border", Set.of(), true);
    }

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {
        String width = "";
        String style = "";
        ArrayList<String> color = new ArrayList<>();
        for (String token : UiCssShorthandSupport.tokens(rawValue)) {
            String lower = UiCssShorthandSupport.lower(token);
            if (width.isBlank() && (WIDTHS.contains(lower) || UiCssShorthandSupport.lengthLike(lower))) {
                width = token;
            } else if (style.isBlank() && STYLES.contains(lower)) {
                style = token;
            } else {
                color.add(token);
            }
        }
        ArrayList<UiCssDeclaration> out = new ArrayList<>();
        if (!width.isBlank()) out.add(new UiCssDeclaration("border-width", width));
        if (!style.isBlank()) out.add(new UiCssDeclaration("border-style", style));
        if (!color.isEmpty()) out.add(new UiCssDeclaration("border-color", String.join(" ", color)));
        return out;
    }
}
