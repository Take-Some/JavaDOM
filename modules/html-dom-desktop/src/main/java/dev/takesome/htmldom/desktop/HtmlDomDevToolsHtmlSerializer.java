package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;
import dev.takesome.htmldom.dom.UiDomText;

import java.util.Map;

final class HtmlDomDevToolsHtmlSerializer {
    private HtmlDomDevToolsHtmlSerializer() { }

    static String serializeDocument(UiDomDocument document) {
        StringBuilder out = new StringBuilder(32_000);
        serializeNode(document.documentElement(), out, 0);
        return out.toString();
    }

    static String serializeNodeToString(UiDomNode node) {
        StringBuilder out = new StringBuilder(4096);
        serializeNode(node, out, 0);
        return out.toString();
    }

    private static void serializeNode(UiDomNode node, StringBuilder out, int depth) {
        if (node instanceof UiDomText text) {
            String value = text.text();
            if (value == null || value.trim().isBlank()) return;
            indent(out, depth).append(escapeText(value.trim())).append('\n');
            return;
        }
        UiDomElement element = (UiDomElement) node;
        indent(out, depth).append('<').append(element.tagName());
        for (Map.Entry<String, String> entry : element.attributes().entrySet()) {
            out.append(' ').append(entry.getKey()).append("=\"").append(escapeAttribute(entry.getValue())).append('"');
        }
        if (voidElement(element.tagName())) {
            out.append('>').append('\n');
            return;
        }
        out.append('>').append('\n');
        for (UiDomNode child : element.children()) serializeNode(child, out, depth + 1);
        indent(out, depth).append("</").append(element.tagName()).append('>').append('\n');
    }

    private static StringBuilder indent(StringBuilder out, int depth) {
        return out.append("    ".repeat(Math.max(0, depth)));
    }

    private static String escapeText(String value) {
        return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttribute(String value) {
        return escapeText(value).replace("\"", "&quot;");
    }

    private static boolean voidElement(String tagName) {
        return switch (tagName) {
            case "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr" -> true;
            default -> false;
        };
    }
}
