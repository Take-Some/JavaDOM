package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
/** Single CSS property declaration. */
public record UiCssDeclaration(String property, String value) {
    public UiCssDeclaration {
        property = normalizeProperty(property);
        value = trimToEmpty(value);
    }

    private static String normalizeProperty(String property) {
        if (property == null || property.isBlank()) throw new IllegalArgumentException("CSS property must not be blank");
        return property.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
