package dev.takesome.htmldom.markup.internal.model;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.textOrEmpty;
import dev.takesome.htmldom.dom.UiDomElement;

/**
 * Attribute and text helper operations for canonical DOM elements.
 */
public final class UiDomAttributes {
    private UiDomAttributes() {
    }

    public static String value(UiDomElement element, String name) {
        if (element == null || name == null || name.isBlank()) return "";
        return element.attribute(name.trim(), "");
    }

    public static boolean has(UiDomElement element, String name) {
        if (element == null || name == null || name.isBlank()) return false;
        return element.hasAttribute(name.trim());
    }

    public static boolean hasClass(UiDomElement element, String className) {
        if (element == null || className == null || className.isBlank()) return false;
        return element.classList().contains(className.trim());
    }

    public static String textOrValue(UiDomElement element, String name) {
        String attr = value(element, name);
        return attr.isBlank() ? textOrEmpty(element, UiDomElement::textContent).trim() : attr;
    }

    public static String firstValue(UiDomElement element, String... names) {
        if (names == null) return "";
        for (String name : names) {
            String value = value(element, name).trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

}
