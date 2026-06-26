package dev.takesome.htmldom.markup.internal.parse.syntax;

import java.util.Set;

/** Parser-level syntax profile: void tags, raw-text tags and optional-end-tag families. */
public final class UiHtmlSyntaxProfile {
    private final Set<String> voidTags;
    private final Set<String> rawTextTags;
    private final Set<String> rcDataTags;
    private final Set<String> headingTags;
    private final Set<String> booleanAttributes;

    public UiHtmlSyntaxProfile(
            Set<String> voidTags,
            Set<String> rawTextTags,
            Set<String> rcDataTags,
            Set<String> headingTags,
            Set<String> booleanAttributes
    ) {
        this.voidTags = Set.copyOf(voidTags == null ? Set.of() : voidTags);
        this.rawTextTags = Set.copyOf(rawTextTags == null ? Set.of() : rawTextTags);
        this.rcDataTags = Set.copyOf(rcDataTags == null ? Set.of() : rcDataTags);
        this.headingTags = Set.copyOf(headingTags == null ? Set.of() : headingTags);
        this.booleanAttributes = Set.copyOf(booleanAttributes == null ? Set.of() : booleanAttributes);
    }

    public boolean isVoidTag(String tagName) {
        return voidTags.contains(tagName);
    }

    public boolean isRawTextTag(String tagName) {
        return rawTextTags.contains(tagName);
    }

    public boolean isRcDataTag(String tagName) {
        return rcDataTags.contains(tagName);
    }

    public boolean requiresRawTextRead(String tagName) {
        return isRawTextTag(tagName) || isRcDataTag(tagName);
    }

    public boolean preservesText(String tagName) {
        return "pre".equals(tagName) || "code".equals(tagName) || "textarea".equals(tagName);
    }

    public boolean isHeadingTag(String tagName) {
        return headingTags.contains(tagName);
    }

    public boolean isBooleanAttribute(String attributeName) {
        return booleanAttributes.contains(attributeName);
    }

    public static UiHtmlSyntaxProfile helixDefault() {
        return new UiHtmlSyntaxProfile(
                Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"),
                Set.of("style", "template"),
                Set.of("title", "textarea", "pre", "code"),
                Set.of("h1", "h2", "h3", "h4", "h5", "h6"),
                Set.of("disabled", "checked", "required", "readonly")
        );
    }
}
