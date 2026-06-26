package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomElement;

import java.util.LinkedHashMap;
import java.util.Map;

final class HtmlDomDevToolsInlineStyle {
    private HtmlDomDevToolsInlineStyle() { }

    static void applyRuntimeComputedStyle(UiDomElement element, String property, String value) {
        if (element == null || property == null || property.isBlank()) return;
        LinkedHashMap<String, String> style = parse(element.attribute("style", ""));
        String key = property.trim();
        String next = value == null ? "" : value.trim();
        if (next.isBlank()) style.remove(key);
        else style.put(key, next);
        String inline = serialize(style);
        if (inline.isBlank()) element.removeAttribute("style");
        else element.setAttribute("style", inline);
        element.setComputedStyle(key, next);
    }

    static LinkedHashMap<String, String> parse(String raw) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return out;
        for (String item : raw.split(";")) {
            int colon = item.indexOf(':');
            if (colon <= 0) continue;
            String key = item.substring(0, colon).trim();
            String value = item.substring(colon + 1).trim();
            if (!key.isBlank() && !value.isBlank()) out.put(key, value);
        }
        return out;
    }

    static String serialize(LinkedHashMap<String, String> style) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : style.entrySet()) {
            if (entry.getKey().isBlank() || entry.getValue().isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(entry.getKey()).append(": ").append(entry.getValue()).append(';');
        }
        return out.toString();
    }
}
