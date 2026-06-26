package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;
import java.util.Set;

public abstract class UiCssLengthPropertySpec extends UiCssBasePropertySpec {
    protected UiCssLengthPropertySpec(String cssName, Set<String> cssAliases, boolean fallbackAttribute) {
        super(cssName, cssAliases, fallbackAttribute);
    }

    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), UiCssLength.AUTO);
    }

    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        return UiCssValue.typed(name(), UiCssLength.parse(rawValue));
    }

    public UiCssLength read(UiDomElement element, UiCssLength fallbackLength) {
        String raw = readRaw(element);
        return raw.isBlank() ? fallbackLength : UiCssLength.parse(raw);
    }
}
