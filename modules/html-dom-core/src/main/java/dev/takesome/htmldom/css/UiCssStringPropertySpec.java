package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import dev.takesome.htmldom.dom.UiDomElement;
import java.util.Set;

public abstract class UiCssStringPropertySpec extends UiCssBasePropertySpec {
    protected UiCssStringPropertySpec(String cssName, Set<String> cssAliases, boolean fallbackAttribute) {
        super(cssName, cssAliases, fallbackAttribute);
    }

    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), "");
    }

    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        return UiCssValue.typed(name(), trimToEmpty(rawValue));
    }

    public String read(UiDomElement element, String fallbackValue) {
        String value = raw(element);
        return value.isBlank() ? fallbackValue : value;
    }
}
