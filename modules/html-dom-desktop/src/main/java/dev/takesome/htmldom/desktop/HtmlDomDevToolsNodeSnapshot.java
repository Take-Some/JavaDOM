package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;
import dev.takesome.htmldom.dom.UiDomText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HtmlDomDevToolsNodeSnapshot {
    private HtmlDomDevToolsNodeSnapshot() { }

    static NodeCopy text(String text) {
        return new TextCopy(text);
    }

    static NodeCopy copy(UiDomNode node) {
        if (node instanceof UiDomText text) return new TextCopy(text.text());
        UiDomElement element = (UiDomElement) node;
        ArrayList<NodeCopy> children = new ArrayList<>();
        for (UiDomNode child : element.children()) children.add(copy(child));
        return new ElementCopy(element.tagName(), new LinkedHashMap<>(element.attributes()), List.copyOf(children));
    }

    interface NodeCopy { UiDomNode create(UiDomDocument document); }

    record TextCopy(String text) implements NodeCopy {
        @Override public UiDomNode create(UiDomDocument document) { return document.createText(text); }
    }

    record ElementCopy(String tagName, Map<String, String> attributes, List<NodeCopy> children) implements NodeCopy {
        @Override public UiDomNode create(UiDomDocument document) {
            UiDomElement element = document.createElement(tagName);
            for (Map.Entry<String, String> entry : attributes.entrySet()) element.setAttribute(entry.getKey(), entry.getValue());
            for (NodeCopy child : children) element.appendChild(child.create(document));
            return element;
        }
    }
}
