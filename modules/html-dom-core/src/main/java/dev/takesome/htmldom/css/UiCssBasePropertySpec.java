package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;
import java.util.Locale;
import java.util.Set;

public abstract class UiCssBasePropertySpec implements UiCssPropertySpec {
    private final String cssName;
    private final Set<String> cssAliases;
    private final boolean fallbackAttribute;

    protected UiCssBasePropertySpec(String cssName, Set<String> cssAliases, boolean fallbackAttribute) {
        if (cssName == null || cssName.isBlank()) throw new IllegalArgumentException("CSS property name must not be blank");
        this.cssName = normalize(cssName);
        this.cssAliases = cssAliases == null ? Set.of() : Set.copyOf(cssAliases);
        this.fallbackAttribute = fallbackAttribute;
    }

    @Override
    public final String name() { return cssName; }

    @Override
    public final Set<String> aliases() { return cssAliases; }

    @Override
    public final boolean attributeFallback() { return fallbackAttribute; }

    public final String raw(UiDomElement element) { return readRaw(element); }

    protected final String readRaw(UiDomElement element) {
        if (element == null) return "";
        String direct = element.style(cssName, "");
        if (!direct.isBlank()) return direct;
        for (String alias : cssAliases) {
            String aliasValue = element.style(alias, "");
            if (!aliasValue.isBlank()) return aliasValue;
        }
        return "";
    }

    protected static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
