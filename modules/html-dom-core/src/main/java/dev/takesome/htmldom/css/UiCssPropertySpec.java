package dev.takesome.htmldom.css;

import java.util.Set;

public interface UiCssPropertySpec {
    String name();
    Set<String> aliases();
    UiCssValue initialValue();
    UiCssValue parse(UiCssParseContext context, String rawValue);
    default boolean attributeFallback() { return false; }
    default UiCssDefinitionStatus status() { return UiCssDefinitionStatus.STABLE; }
    default String replacement() { return ""; }
}
