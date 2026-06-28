package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Locale;

/** Computes property-aware dirty repaint regions for runtime compositor/paint effects. */
final class HtmlDomDirtyRegionEngine {
    Rectangle dirtyRect(UiDomElement element, Rectangle rect, Collection<String> properties, HtmlDomTransformEngine transformEngine) {
        if (element == null || rect == null || rect.width <= 0 || rect.height <= 0) return null;
        Rectangle base = new Rectangle(rect);
        Collection<String> props = properties == null ? java.util.List.of() : properties;
        Rectangle dirty = new Rectangle(base);
        if (hasTransformProperty(props) || hasActiveTransform(element)) {
            dirty = union(dirty, transformEngine == null ? base : transformEngine.transformedBounds(element, base));
        }
        int shadow = shadowExpansion(element.style("box-shadow", ""));
        if (shadow > 0 && (props.isEmpty() || props.contains("box-shadow") || props.contains("*"))) dirty = grow(dirty, shadow, shadow);
        int border = borderExpansion(element);
        if (border > 0 && needsBorderExpansion(props)) dirty = grow(dirty, border, border);
        return dirty;
    }

    private boolean hasTransformProperty(Collection<String> properties) {
        return properties.contains("transform") || properties.contains("translate") || properties.contains("scale") || properties.contains("rotate") || properties.contains("*");
    }

    private boolean hasActiveTransform(UiDomElement element) {
        return active(element.style("transform", "")) || active(element.style("translate", "")) || active(element.style("scale", "")) || active(element.style("rotate", ""));
    }

    private boolean active(String raw) {
        String value = raw == null ? "" : raw.trim();
        return !value.isBlank() && !"none".equalsIgnoreCase(value);
    }

    private boolean needsBorderExpansion(Collection<String> properties) {
        if (properties.isEmpty() || properties.contains("*")) return true;
        for (String property : properties) {
            if (property == null) continue;
            String p = property.toLowerCase(Locale.ROOT);
            if (p.startsWith("border") || p.startsWith("outline")) return true;
        }
        return false;
    }

    private int borderExpansion(UiDomElement element) {
        int out = 0;
        out = Math.max(out, lengthPx(element.style("border-width", "")));
        out = Math.max(out, lengthPx(element.style("border-top-width", "")));
        out = Math.max(out, lengthPx(element.style("border-right-width", "")));
        out = Math.max(out, lengthPx(element.style("border-bottom-width", "")));
        out = Math.max(out, lengthPx(element.style("border-left-width", "")));
        out = Math.max(out, lengthPx(element.style("outline-width", "")));
        return out;
    }

    private int shadowExpansion(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return 0;
        int max = 0;
        for (String shadow : splitTopLevelComma(value)) {
            java.util.List<Integer> lengths = shadowLengths(shadow);
            if (lengths.size() < 2) continue;
            int offsetX = Math.abs(lengths.get(0));
            int offsetY = Math.abs(lengths.get(1));
            int blur = lengths.size() > 2 ? Math.max(0, lengths.get(2)) : 0;
            int spread = lengths.size() > 3 ? Math.max(0, lengths.get(3)) : 0;
            max = Math.max(max, Math.max(offsetX, offsetY) + blur + spread);
        }
        return max;
    }

    private java.util.List<Integer> shadowLengths(String shadow) {
        java.util.ArrayList<Integer> out = new java.util.ArrayList<>();
        for (String token : shadow.trim().split("\\s+")) {
            if (token.isBlank() || token.equalsIgnoreCase("inset")) continue;
            Integer value = maybeLengthPx(token);
            if (value != null) out.add(value);
        }
        return out;
    }

    private java.util.List<String> splitTopLevelComma(String raw) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        StringBuilder token = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '(') depth++;
            else if (ch == ')') depth--;
            if (ch == ',' && depth == 0) {
                if (!token.isEmpty()) out.add(token.toString().trim());
                token.setLength(0);
                continue;
            }
            token.append(ch);
        }
        if (!token.isEmpty()) out.add(token.toString().trim());
        return out;
    }

    private int lengthPx(String raw) {
        Integer value = maybeLengthPx(raw);
        return value == null ? 0 : Math.max(0, value);
    }

    private Integer maybeLengthPx(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) return null;
        if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
        try {
            return (int) Math.ceil(Double.parseDouble(value));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Rectangle grow(Rectangle rect, int horizontal, int vertical) {
        Rectangle out = new Rectangle(rect);
        out.grow(Math.max(0, horizontal), Math.max(0, vertical));
        return out;
    }

    private Rectangle union(Rectangle a, Rectangle b) {
        if (b == null) return a;
        return a == null ? new Rectangle(b) : a.union(b);
    }
}
