package dev.takesome.htmldom.html;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public abstract class UiHtmlBaseTagSpec implements UiHtmlTagSpec {
    private static final Set<String> COMMON_ALLOWED_ATTRIBUTES = Set.of(
            "i18n-key",
            "i18n-args",
            "i18n-format"
    );

    private final String tagName;
    private final Set<String> tagAliases;
    private final String composerId;
    private final Set<String> allowedAttributes;

    protected UiHtmlBaseTagSpec(String tagName, Set<String> tagAliases, String composerId, Set<String> allowedAttributes) {
        this.tagName = normalize(tagName);
        this.tagAliases = tagAliases == null ? Set.of() : Set.copyOf(tagAliases);
        this.composerId = composerId;
        this.allowedAttributes = mergeAttributes(allowedAttributes);
    }

    public final String name() { return tagName; }
    public final Set<String> aliases() { return tagAliases; }
    public final String composerId() { return composerId; }
    public final Set<String> allowedAttributes() { return allowedAttributes; }

    private static Set<String> mergeAttributes(Set<String> specific) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (specific != null) out.addAll(specific);
        out.addAll(COMMON_ALLOWED_ATTRIBUTES);
        return Set.copyOf(out);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new UiHtmlException("HTML tag name must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
