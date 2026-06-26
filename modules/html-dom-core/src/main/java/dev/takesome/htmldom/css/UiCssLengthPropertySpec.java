package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.support.logging.HtmlDomLog;
import dev.takesome.htmldom.support.logging.HtmlDomLog.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class UiCssLengthPropertySpec extends UiCssBasePropertySpec {
    private static final Logger LOG = HtmlDomLog.logger(UiCssLengthPropertySpec.class);
    private static final Set<String> WARNED_INVALID_LENGTHS = ConcurrentHashMap.newKeySet();

    protected UiCssLengthPropertySpec(String cssName, Set<String> cssAliases, boolean fallbackAttribute) {
        super(cssName, cssAliases, fallbackAttribute);
    }

    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), UiCssLength.AUTO);
    }

    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        return UiCssValue.typed(name(), parseOrFallback(rawValue, UiCssLength.AUTO));
    }

    public UiCssLength read(UiDomElement element, UiCssLength fallbackLength) {
        String raw = readRaw(element);
        UiCssLength fallback = fallbackLength == null ? UiCssLength.AUTO : fallbackLength;
        return raw.isBlank() ? fallback : parseOrFallback(raw, fallback);
    }

    private UiCssLength parseOrFallback(String raw, UiCssLength fallback) {
        try {
            return UiCssLength.parse(raw);
        } catch (RuntimeException exception) {
            warnInvalid(raw, fallback, exception);
            return fallback;
        }
    }

    private void warnInvalid(String raw, UiCssLength fallback, RuntimeException exception) {
        String key = name() + "|" + raw;
        if (!WARNED_INVALID_LENGTHS.add(key)) return;
        LOG.warn(
                "UI CSS invalid length property='{}' raw='{}'; using fallback='{}' reason='{}'",
                name(),
                raw,
                fallback == null ? "auto" : fallback.cssText(),
                exception.getMessage()
        );
    }
}
