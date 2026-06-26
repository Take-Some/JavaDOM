package dev.takesome.htmldom.markup.internal.parse.tree;

import dev.takesome.htmldom.html.UiHtmlTagSpec;
import dev.takesome.htmldom.markup.internal.parse.UiHtmlParseSession;
import dev.takesome.htmldom.markup.internal.parse.scanner.UiHtmlToken;

import java.util.Locale;
import java.util.Set;

/** Validates tag/attribute vocabulary without constructing runtime nodes. */
final class UiHtmlStartTagValidator {
    private final UiHtmlParseSession session;

    UiHtmlStartTagValidator(UiHtmlParseSession session) {
        this.session = session;
    }

    void validate(UiHtmlToken token) {
        UiHtmlTagSpec spec = session.tags().find(token.name());
        if (spec == null) {
            UiHtmlTagSpec fallback = session.tags().resolveOrFallback(token.name());
            session.warn(
                    "html.unknown-tag-fallback",
                    "Unknown HTML tag <" + token.name() + ">; using <" + fallback.name() + "> runtime fallback",
                    token.offset(),
                    Math.max(1, token.length())
            );
            spec = fallback;
        }
        for (String attribute : token.attributes().keySet()) {
            if (!isAttributeAllowed(spec, attribute)) {
                session.warn(
                        "html.unsupported-attribute",
                        "Unsupported attribute `" + attribute + "` on <" + token.name() + ">; kept for compatibility",
                        token.offset(),
                        Math.max(1, token.length())
                );
            }
        }
    }

    private boolean isAttributeAllowed(UiHtmlTagSpec spec, String attribute) {
        if (spec == null || attribute == null || attribute.isBlank()) {
            return false;
        }
        String name = attribute.trim().toLowerCase(Locale.ROOT);
        Set<String> allowed = spec.allowedAttributes();
        if (allowed.contains(name)) {
            return true;
        }
        return name.startsWith("data-") && allowed.contains("data-*");
    }
}
