package dev.takesome.htmldom.markup.internal.parse.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mutable parse tree node used only inside the parser tree builder. */
final class UiHtmlParseNode {
    final String tag;
    final Map<String, String> attributes;
    final ArrayList<UiHtmlParseNode> children;
    final StringBuilder text;

    UiHtmlParseNode(String tag, Map<String, String> attributes) {
        this(tag, attributes, new ArrayList<>(), new StringBuilder());
    }

    UiHtmlParseNode(String tag, Map<String, String> attributes, List<UiHtmlParseNode> children, StringBuilder text) {
        this.tag = tag;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        this.children = new ArrayList<>(children == null ? List.of() : children);
        this.text = new StringBuilder(text == null ? "" : text.toString());
    }
}
