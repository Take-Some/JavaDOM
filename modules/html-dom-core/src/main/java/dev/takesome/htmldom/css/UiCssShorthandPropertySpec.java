package dev.takesome.htmldom.css;

import java.util.List;

/** CSS shorthand property that expands into canonical longhand declarations. */
public interface UiCssShorthandPropertySpec extends UiCssPropertySpec {
    List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue);
}
