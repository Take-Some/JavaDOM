package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Locale;

/** Paint phases for the desktop Java2D renderer. */
public final class HtmlDomPaintEngine {
    private boolean paintingFixedLayer;

    public boolean paintingFixedLayer() {
        return paintingFixedLayer;
    }

    public boolean fixed(UiDomElement element) {
        return element != null && "fixed".equals(element.style("position", "").trim().toLowerCase(Locale.ROOT));
    }

    public boolean hasFixedAncestor(UiDomElement element) {
        UiDomElement current = element == null ? null : element.parent();
        while (current != null) {
            if (fixed(current)) return true;
            current = current.parent();
        }
        return false;
    }

    public void paintFixedLayer(Graphics2D g, UiDomDocument document, FixedElementPainter painter) {
        if (g == null || document == null || painter == null) return;
        paintingFixedLayer = true;
        try {
            for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
                if (fixed(element) && !hasFixedAncestor(element)) painter.paint(g, element);
            }
        } finally {
            paintingFixedLayer = false;
        }
    }

    public void paintBackground(Graphics2D g, UiDomElement element, Rectangle r) {
        int radius = Math.round(length(first(element, "border-radius", "border-top-left-radius"), 0));
        paintShadow(g, element, r, radius);
        fillBackground(g, element, r, radius);
    }

    public void paintBorder(Graphics2D g, UiDomElement element, Rectangle r) {
        int radius = Math.round(length(first(element, "border-radius", "border-top-left-radius"), 0));
        String borderRaw = element.style("border", "");
        float borderWidth = length(element.style("border-width", ""), -1);
        if (borderWidth < 0f) borderWidth = borderWidth(borderRaw);
        Color border = color(element.style("border-color", ""), null);
        if (border == null) border = borderColor(borderRaw);
        if (borderWidth > 0f && border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(Math.max(1f, borderWidth)));
            g.draw(new RoundRectangle2D.Float(r.x, r.y, Math.max(0, r.width - 1), Math.max(0, r.height - 1), radius, radius));
        }
    }

    public void paintOutline(Graphics2D g, UiDomElement element, Rectangle r, boolean focused) {
        if (focused) {
            int radius = Math.round(length(first(element, "border-radius", "border-top-left-radius"), 0));
            g.setColor(color("#50b7ff", Color.CYAN));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(r.x - 2, r.y - 2, r.width + 3, r.height + 3, radius + 4, radius + 4));
        }
        String raw = element.style("outline", "");
        float outlineWidth = length(element.style("outline-width", ""), -1);
        if (outlineWidth < 0f) outlineWidth = borderWidth(raw);
        Color outline = color(element.style("outline-color", ""), null);
        if (outline == null) outline = borderColor(raw);
        if (outlineWidth <= 0f || outline == null) return;
        int radius = Math.round(length(first(element, "border-radius", "border-top-left-radius"), 0));
        float grow = Math.max(1f, outlineWidth);
        g.setColor(outline);
        g.setStroke(new BasicStroke(Math.max(1f, outlineWidth)));
        g.draw(new RoundRectangle2D.Float(r.x - grow, r.y - grow, r.width + grow * 2f - 1f, r.height + grow * 2f - 1f, radius + grow, radius + grow));
    }

    public void paintProgress(Graphics2D g, UiDomElement element, Rectangle r) {
        float max = number(element.attribute("max", "100"), 100f);
        float value = number(element.attribute("value", "0"), 0f);
        float ratio = max <= 0f ? 0f : Math.max(0f, Math.min(1f, value / max));
        int radius = Math.max(6, r.height / 2);
        g.setColor(color("#0c1524", Color.DARK_GRAY));
        g.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, radius, radius));
        g.setColor(color("#50b7ff", Color.BLUE));
        g.fill(new RoundRectangle2D.Float(r.x, r.y, Math.round(r.width * ratio), r.height, radius, radius));
    }

    public boolean clipsContents(UiDomElement element) {
        String overflow = first(element, "overflow", "overflow-x", "overflow-y").toLowerCase(Locale.ROOT);
        return overflow.equals("hidden") || overflow.equals("clip") || overflow.equals("auto") || overflow.equals("scroll");
    }

    public Shape clipShape(UiDomElement element, Rectangle rect) {
        int radius = Math.round(length(first(element, "border-radius", "border-top-left-radius"), 0));
        if (radius <= 0) return rect;
        return new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, radius, radius);
    }

    public Color color(String raw, Color fallback) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank() || value.equals("transparent") || value.startsWith("var(")) return fallback;
        if (value.startsWith("#")) {
            try {
                String hex = value.substring(1);
                if (hex.length() == 3) hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
                if (hex.length() == 6) return new Color(Integer.parseInt(hex, 16));
                if (hex.length() == 8) return new Color((int) Long.parseLong(hex, 16), true);
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        if (value.startsWith("rgb")) {
            try {
                int start = value.indexOf('('), end = value.indexOf(')');
                if (start >= 0 && end > start) {
                    String[] parts = value.substring(start + 1, end).split(",");
                    int r = Math.round(Float.parseFloat(parts[0].trim()));
                    int g = Math.round(Float.parseFloat(parts[1].trim()));
                    int b = Math.round(Float.parseFloat(parts[2].trim()));
                    int a = parts.length > 3 ? alpha(parts[3].trim()) : 255;
                    return new Color(clamp(r), clamp(g), clamp(b), clamp(a));
                }
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return switch (value) {
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            default -> fallback;
        };
    }

    public float length(String raw, float fallback) {
        if (raw == null || raw.isBlank() || raw.startsWith("var(")) return fallback;
        String value = raw.trim().toLowerCase(Locale.ROOT).replace("px", "");
        int space = value.indexOf(' ');
        if (space > 0) value = value.substring(0, space);
        try { return Float.parseFloat(value); } catch (RuntimeException ignored) { return fallback; }
    }

    public float number(String raw, float fallback) {
        try { return Float.parseFloat(raw == null ? "" : raw.trim()); } catch (RuntimeException ignored) { return fallback; }
    }

    public String first(UiDomElement element, String... names) {
        for (String name : names) {
            String value = element.style(name, "").trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    public Color borderColor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.trim().split(" ");
        for (String part : parts) {
            Color parsed = color(part, null);
            if (parsed != null) return parsed;
        }
        return null;
    }

    public float borderWidth(String raw) {
        if (raw == null || raw.isBlank()) return 0f;
        String[] parts = raw.trim().split(" ");
        for (String part : parts) {
            float value = length(part, -1f);
            if (value >= 0f) return value;
        }
        return 0f;
    }

    private void paintShadow(Graphics2D g, UiDomElement element, Rectangle r, int radius) {
        String raw = element.style("box-shadow", "").trim();
        if (raw.isBlank() || raw.equalsIgnoreCase("none")) return;
        String firstShadow = raw.split(",")[0].trim();
        String[] parts = firstShadow.split(" ");
        float ox = 0f, oy = 0f, blur = 0f;
        int numeric = 0;
        Color shadow = new Color(0, 0, 0, 90);
        for (String part : parts) {
            if (part.isBlank() || "inset".equalsIgnoreCase(part)) continue;
            Color parsed = color(part, null);
            if (parsed != null) { shadow = parsed; continue; }
            float value = length(part, Float.NaN);
            if (Float.isNaN(value)) continue;
            if (numeric == 0) ox = value;
            else if (numeric == 1) oy = value;
            else if (numeric == 2) blur = value;
            numeric++;
        }
        int steps = Math.max(1, Math.min(10, Math.round(blur / 3f)));
        for (int i = steps; i >= 1; i--) {
            float t = i / (float) steps;
            int alpha = Math.max(8, Math.round(shadow.getAlpha() * 0.16f * t));
            g.setColor(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), alpha));
            float grow = i * Math.max(1f, blur / Math.max(1f, steps));
            g.fill(new RoundRectangle2D.Float(r.x + ox - grow, r.y + oy - grow, r.width + grow * 2f, r.height + grow * 2f, radius + grow, radius + grow));
        }
    }

    private void fillBackground(Graphics2D g, UiDomElement element, Rectangle r, int radius) {
        String raw = first(element, "background", "background-image", "background-color");
        if (raw.toLowerCase(Locale.ROOT).contains("linear-gradient")) {
            fillSmoothLinear(g, raw, r, radius);
            return;
        }
        Color bg = color(raw, null);
        if (bg == null) return;
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, radius, radius));
    }

    private void fillSmoothLinear(Graphics2D g, String raw, Rectangle r, int radius) {
        ArrayList<Color> colors = gradientColors(raw);
        if (colors.isEmpty()) return;
        Color c0 = colors.get(0);
        Color c1 = colors.size() > 1 ? colors.get(colors.size() - 1) : c0;
        Shape oldClip = g.getClip();
        Shape clip = radius > 0 ? new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, radius, radius) : r;
        g.clip(clip);
        int steps;
        boolean horizontal = gradientHorizontal(raw);
        if (horizontal) {
            steps = Math.max(1, r.width);
            for (int x = 0; x < steps; x++) {
                float t = steps <= 1 ? 0f : x / (float) (steps - 1);
                g.setColor(mix(c0, c1, t));
                g.fillRect(r.x + x, r.y, 1, r.height);
            }
        } else {
            steps = Math.max(1, r.height);
            for (int y = 0; y < steps; y++) {
                float t = steps <= 1 ? 0f : y / (float) (steps - 1);
                g.setColor(mix(c0, c1, t));
                g.fillRect(r.x, r.y + y, r.width, 1);
            }
        }
        g.setClip(oldClip);
    }

    private ArrayList<Color> gradientColors(String raw) {
        ArrayList<Color> colors = new ArrayList<>();
        int a0 = raw.indexOf('(');
        int a1 = raw.lastIndexOf(')');
        String body = a0 >= 0 && a1 > a0 ? raw.substring(a0 + 1, a1) : raw;
        int depth = 0;
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '(') depth++;
            if (ch == ')') depth--;
            if (ch == ',' && depth <= 0) {
                addGradientColor(colors, token.toString());
                token.setLength(0);
            } else token.append(ch);
        }
        addGradientColor(colors, token.toString());
        return colors;
    }

    private void addGradientColor(ArrayList<Color> colors, String token) {
        String value = token == null ? "" : token.trim();
        if (value.isBlank()) return;
        if (value.startsWith("to ") || value.endsWith("deg") || value.endsWith("turn")) return;
        Color parsed = color(value.split(" ")[0], null);
        if (parsed != null) colors.add(parsed);
    }

    private boolean gradientHorizontal(String raw) {
        String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        return value.contains("to right") || value.contains("90deg") || value.contains(".25turn");
    }

    private Color mix(Color a, Color b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k)
        );
    }

    private int alpha(String raw) {
        float value = Float.parseFloat(raw);
        return value <= 1f ? Math.round(value * 255f) : Math.round(value);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @FunctionalInterface
    public interface FixedElementPainter {
        void paint(Graphics2D g, UiDomElement element);
    }
}
