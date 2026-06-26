package dev.takesome.htmldom.css.properties.paint;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import dev.takesome.htmldom.css.UiBackgroundImage;
import dev.takesome.htmldom.css.UiCssBasePropertySpec;
import dev.takesome.htmldom.css.UiCssParseContext;
import dev.takesome.htmldom.css.UiCssValue;
import java.util.Set;

public final class BackgroundImageCssProperty extends UiCssBasePropertySpec {
    public BackgroundImageCssProperty() {
        super("back" + "ground-image", Set.of(), true);
    }

    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), UiBackgroundImage.none());
    }

    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        String value = trimToEmpty(rawValue);
        String source = value;
        if (value.startsWith("url(") && value.endsWith(")")) {
            source = value.substring(4, value.length() - 1).trim();
            if ((source.startsWith("\"") && source.endsWith("\"")) || (source.startsWith("'") && source.endsWith("'"))) {
                source = source.substring(1, source.length() - 1);
            }
        }
        return UiCssValue.typed(name(), source.isBlank() || "none".equalsIgnoreCase(source) ? UiBackgroundImage.none() : new UiBackgroundImage(source));
    }
}
