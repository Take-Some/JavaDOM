package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;
import java.util.Map;
import java.util.Set;

public abstract class UiCssKeywordPropertySpec<T> extends UiCssBasePropertySpec {
    private final Map<String, T> allowed;
    private final T fallbackValue;

    protected UiCssKeywordPropertySpec(String cssName, Set<String> cssAliases, boolean fallbackAttribute, Map<String, T> allowed, T fallbackValue) {
        super(cssName, cssAliases, fallbackAttribute);
        this.allowed = Map.copyOf(allowed);
        this.fallbackValue = fallbackValue;
    }

    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), fallbackValue);
    }

    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        String keyword = context.keyword(rawValue);
        return UiCssValue.typed(name(), allowed.getOrDefault(keyword, fallbackValue));
    }

    public T read(UiDomElement element) {
        String raw = readRaw(element);
        return raw.isBlank() ? fallbackValue : allowed.getOrDefault(normalize(raw), fallbackValue);
    }
}
