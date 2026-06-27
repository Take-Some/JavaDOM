package dev.takesome.htmldom.css.properties.layout;

import dev.takesome.htmldom.css.UiCssLength;
import dev.takesome.htmldom.css.UiCssLengthPropertySpec;
import dev.takesome.htmldom.css.UiCssParseContext;
import dev.takesome.htmldom.css.UiCssValue;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.support.logging.HtmlDomLog;
import dev.takesome.htmldom.support.logging.HtmlDomLog.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** CSS gap shorthand. HtmlDom currently exposes one layout gap, resolved from row/column syntax by flow axis. */
public final class GapCssProperty extends UiCssLengthPropertySpec {
    private static final Logger LOG = HtmlDomLog.logger(GapCssProperty.class);
    private static final Set<String> WARNED_INVALID_GAPS = ConcurrentHashMap.newKeySet();

    public GapCssProperty() {
        super("gap", Set.of(), true);
    }

    @Override
    public UiCssValue parse(UiCssParseContext context, String rawValue) {
        return UiCssValue.typed(name(), parseGap(mainAxisGap(rawValue, true), UiCssLength.ZERO, rawValue));
    }

    public UiCssLength read(UiDomElement element, UiCssLength fallbackLength, boolean rowFlow) {
        String raw = readRaw(element);
        UiCssLength fallback = fallbackLength == null ? UiCssLength.ZERO : fallbackLength;
        return raw.isBlank() ? fallback : parseGap(mainAxisGap(raw, rowFlow), fallback, raw);
    }

    @Override
    public UiCssLength read(UiDomElement element, UiCssLength fallbackLength) {
        String raw = readRaw(element);
        UiCssLength fallback = fallbackLength == null ? UiCssLength.ZERO : fallbackLength;
        return raw.isBlank() ? fallback : parseGap(mainAxisGap(raw, true), fallback, raw);
    }

    private UiCssLength parseGap(String value, UiCssLength fallback, String raw) {
        try {
            validateRawGap(raw);
            return UiCssLength.parse(value);
        } catch (RuntimeException exception) {
            warnInvalid(raw, fallback, exception);
            return fallback;
        }
    }

    private String mainAxisGap(String raw, boolean rowFlow) {
        if (raw == null || raw.isBlank()) return "0px";
        String[] parts = raw.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        String selected = rowFlow ? parts[1] : parts[0];
        if (LOG.debugEnabled()) {
            LOG.debug("UI CSS gap shorthand raw='{}' flow='{}' selected='{}'", raw, rowFlow ? "row" : "column", selected);
        }
        return selected;
    }


    private void validateRawGap(String raw) {
        if (raw == null || raw.isBlank()) return;
        String[] parts = raw.trim().split("\\s+");
        if (parts.length > 2) throw new IllegalArgumentException("gap accepts one or two length values");
        for (String part : parts) {
            if (invalidBareNumber(part)) throw new IllegalArgumentException("Non-zero CSS gap lengths need an explicit unit: " + part);
        }
    }

    private boolean invalidBareNumber(String token) {
        if (token == null) return false;
        String value = token.trim();
        if (value.isBlank()) return false;
        boolean decimalPoint = false;
        int start = value.charAt(0) == '+' || value.charAt(0) == '-' ? 1 : 0;
        if (start >= value.length()) return false;
        for (int index = start; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '.') {
                if (decimalPoint) return false;
                decimalPoint = true;
                continue;
            }
            if (!Character.isDigit(ch)) return false;
        }
        try {
            return Float.parseFloat(value) != 0f;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private void warnInvalid(String raw, UiCssLength fallback, RuntimeException exception) {
        String key = raw == null ? "" : raw;
        if (!WARNED_INVALID_GAPS.add(key)) return;
        LOG.warn(
                "UI CSS invalid gap raw='{}'; using fallback='{}' reason='{}'",
                raw,
                fallback == null ? "0px" : fallback.cssText(),
                exception.getMessage()
        );
    }
}
