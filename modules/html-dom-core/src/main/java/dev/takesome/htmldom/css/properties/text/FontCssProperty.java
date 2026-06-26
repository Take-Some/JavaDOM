package dev.takesome.htmldom.css.properties.text;

import dev.takesome.htmldom.css.UiCssDeclaration;
import dev.takesome.htmldom.css.UiCssParseContext;
import dev.takesome.htmldom.css.UiCssShorthandPropertySpec;
import dev.takesome.htmldom.css.UiCssShorthandSupport;
import dev.takesome.htmldom.css.UiCssStringPropertySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FontCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {
    private static final Set<String> WEIGHTS = Set.of("normal", "bold", "bolder", "lighter", "100", "200", "300", "400", "500", "600", "700", "800", "900");

    public FontCssProperty() {
        super("font", Set.of(), true);
    }

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {
        List<String> tokens = UiCssShorthandSupport.tokens(rawValue);
        if (tokens.isEmpty()) return List.of();

        String weight = "";
        String size = "";
        ArrayList<String> family = new ArrayList<>();

        for (String token : tokens) {
            String lower = UiCssShorthandSupport.lower(token);
            if (weight.isBlank() && WEIGHTS.contains(lower)) {
                weight = token;
            } else if (size.isBlank() && (lower.contains("px") || lower.contains("pt") || lower.contains("em") || lower.contains("rem") || lower.matches("[0-9]+"))) {
                size = token;
            } else {
                family.add(token);
            }
        }

        ArrayList<UiCssDeclaration> out = new ArrayList<>();
        if (!weight.isBlank()) out.add(new UiCssDeclaration("font-weight", weight));
        if (!size.isBlank()) out.add(new UiCssDeclaration("font-size", size));
        if (!family.isEmpty()) out.add(new UiCssDeclaration("font-family", String.join(" ", family)));
        return out;
    }
}
