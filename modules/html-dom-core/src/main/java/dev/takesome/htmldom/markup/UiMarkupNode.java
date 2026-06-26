package dev.takesome.htmldom.markup;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.emptyIfNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable parsed HtmlDom markup element. */
public final class UiMarkupNode {
    private final String tag;
    private final Map<String, String> attributes;
    private final List<UiMarkupNode> children;
    private final String text;

    public UiMarkupNode(String tag, Map<String, String> attributes, List<UiMarkupNode> children, String text) {
        this.tag = requireTag(tag);
        this.attributes = Collections.unmodifiableMap(attributes == null ? Map.of() : Map.copyOf(attributes));
        this.children = Collections.unmodifiableList(children == null ? List.of() : List.copyOf(children));
        this.text = emptyIfNull(text);
    }

    public String tag() {
        return tag;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public List<UiMarkupNode> children() {
        return children;
    }

    public String text() {
        return text;
    }

    private static String requireTag(String tag) {
        String value = Objects.requireNonNull(tag, "tag").trim().toLowerCase(java.util.Locale.ROOT);
        if (value.isBlank()) throw new IllegalArgumentException("markup tag must not be blank");
        return value;
    }
}
