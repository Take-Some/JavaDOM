package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
/** Typed CSS background-image value. */
public record UiBackgroundImage(String source) {
    private static final UiBackgroundImage NONE = new UiBackgroundImage("");

    public UiBackgroundImage {
        source = trimToEmpty(source);
    }

    public static UiBackgroundImage none() {
        return NONE;
    }

    public boolean noneValue() {
        return source.isBlank();
    }
}
