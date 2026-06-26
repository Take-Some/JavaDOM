package dev.takesome.htmldom.css;

/** Typed CSS value owned by a concrete CSS property definition. */
public record UiCssValue(String property, Object value) {
    public UiCssValue {
        if (property == null || property.isBlank()) throw new IllegalArgumentException("CSS value property must not be blank");
    }

    public static UiCssValue typed(String property, Object value) {
        return new UiCssValue(property, value);
    }

    public <T> T require(Class<T> type) {
        if (!type.isInstance(value)) throw new UiCssException("CSS value `" + property + "` is not " + type.getSimpleName());
        return type.cast(value);
    }
}
