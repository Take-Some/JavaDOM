package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.lowerTrimToEmpty;
import java.util.Locale;

public final class UiCssParseContext {
    public String keyword(String raw) {
        return lowerTrimToEmpty(raw, Locale.ROOT);
    }
}
