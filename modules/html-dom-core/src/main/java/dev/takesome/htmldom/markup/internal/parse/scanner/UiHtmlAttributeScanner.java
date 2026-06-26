package dev.takesome.htmldom.markup.internal.parse.scanner;

import dev.takesome.htmldom.markup.internal.parse.text.UiHtmlTextDecoder;

import java.util.LinkedHashMap;
import java.util.Map;

/** Attribute/name scanner for one already-isolated start tag body. */
final class UiHtmlAttributeScanner {
    private final UiHtmlNameCanonicalizer names;

    UiHtmlAttributeScanner(UiHtmlNameCanonicalizer names) {
        this.names = names;
    }

    UiHtmlStartTagData parseStartTag(String raw) {
        if (raw == null) {
            return null;
        }
        String content = raw.trim();
        if (content.isBlank() || content.startsWith("/")) {
            return null;
        }

        boolean selfClosing = false;
        int slash = UiHtmlTagSyntax.lastNonWhitespace(content);
        if (slash >= 0 && content.charAt(slash) == '/') {
            selfClosing = true;
            content = content.substring(0, slash).trim();
        }
        if (content.isBlank()) {
            return null;
        }

        int index = 0;
        while (index < content.length() && UiHtmlTagSyntax.isNameChar(content.charAt(index))) {
            index++;
        }
        if (index <= 0) {
            return null;
        }

        String name = names.canonical(content.substring(0, index));
        return new UiHtmlStartTagData(name, parseAttributes(content, index), selfClosing);
    }

    private Map<String, String> parseAttributes(String content, int index) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        int i = index;
        while (i < content.length()) {
            i = UiHtmlTagSyntax.skipWhitespace(content, i);
            if (i >= content.length() || content.charAt(i) == '/') {
                break;
            }

            int nameStart = i;
            while (i < content.length()) {
                char ch = content.charAt(i);
                if (Character.isWhitespace(ch) || ch == '=' || ch == '/' || ch == '>') {
                    break;
                }
                i++;
            }
            if (i <= nameStart) {
                break;
            }

            String name = names.canonical(content.substring(nameStart, i));
            i = UiHtmlTagSyntax.skipWhitespace(content, i);

            String value = "true";
            if (i < content.length() && content.charAt(i) == '=') {
                i++;
                i = UiHtmlTagSyntax.skipWhitespace(content, i);
                AttributeValue parsed = readAttributeValue(content, i);
                value = parsed.value();
                i = parsed.nextIndex();
            }
            if (!name.isBlank()) {
                attributes.put(name, UiHtmlTextDecoder.decodeEntities(value));
            }
        }
        return attributes;
    }

    private AttributeValue readAttributeValue(String content, int index) {
        if (index >= content.length()) {
            return new AttributeValue("", index);
        }
        char first = content.charAt(index);
        if (first == '"' || first == '\'') {
            int i = index + 1;
            StringBuilder out = new StringBuilder();
            while (i < content.length()) {
                char ch = content.charAt(i);
                if (ch == first) {
                    return new AttributeValue(out.toString(), i + 1);
                }
                out.append(ch);
                i++;
            }
            return new AttributeValue(out.toString(), i);
        }

        int i = index;
        while (i < content.length()) {
            char ch = content.charAt(i);
            if (Character.isWhitespace(ch) || ch == '/' || ch == '>') {
                break;
            }
            i++;
        }
        return new AttributeValue(content.substring(index, i), i);
    }

    private record AttributeValue(String value, int nextIndex) {
    }
}
