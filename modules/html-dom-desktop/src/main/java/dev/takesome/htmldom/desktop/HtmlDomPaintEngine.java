package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Paint phases for the desktop Java2D renderer. */
public final class HtmlDomPaintEngine {
    private static final Map<String, BufferedImage> BACKGROUND_IMAGE_CACHE = new ConcurrentHashMap<>();
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
        String borderStyle = HtmlDomBorderStyles.borderStyle(element.style("border-style", ""), borderRaw);
        Color border = color(element.style("border-color", ""), null);
        if (border == null) border = borderColor(borderRaw);
        if (borderWidth > 0f && border != null && !borderStyle.equals("none") && !borderStyle.equals("hidden")) {
            g.setColor(border);
            g.setStroke(HtmlDomBorderStyles.borderStroke(borderWidth, borderStyle));
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
        Color named = HtmlDomNamedColors.lookup(value);
        return named == null ? fallback : named;
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

    public String borderStyle(String explicit, String shorthand) {
        return HtmlDomBorderStyles.borderStyle(explicit, shorthand);
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
        String image = first(element, "background-image", "background");
        if (containsUrl(image)) {
            if (fillBackgroundImage(g, element, image, r, radius)) {
                return;
            }
        }
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

    private boolean fillBackgroundImage(Graphics2D g, UiDomElement element, String raw, Rectangle r, int radius) {
        String resource = urlResource(raw);
        if (resource.isBlank()) return false;
        BufferedImage image = loadBackgroundImage(resource);
        if (image == null) return false;
        Rectangle target = backgroundImageTarget(element, image, r);
        Shape oldClip = g.getClip();
        Shape clip = radius > 0 ? new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, radius, radius) : r;
        try {
            g.clip(clip);
            g.drawImage(image, target.x, target.y, target.width, target.height, null);
        } finally {
            g.setClip(oldClip);
        }
        return true;
    }

    private Rectangle backgroundImageTarget(UiDomElement element, BufferedImage image, Rectangle r) {
        String size = first(element, "background-size").trim().toLowerCase(Locale.ROOT);
        float scale;
        if ("contain".equals(size)) {
            scale = Math.min(r.width / (float) image.getWidth(), r.height / (float) image.getHeight());
        } else if ("auto".equals(size) || size.isBlank()) {
            scale = 1f;
        } else {
            scale = Math.max(r.width / (float) image.getWidth(), r.height / (float) image.getHeight());
        }
        int w = Math.max(1, Math.round(image.getWidth() * scale));
        int h = Math.max(1, Math.round(image.getHeight() * scale));
        String position = first(element, "background-position").trim().toLowerCase(Locale.ROOT);
        int x = r.x;
        int y = r.y;
        if (position.contains("right")) x = r.x + r.width - w;
        else if (position.contains("center") || position.isBlank()) x = r.x + (r.width - w) / 2;
        if (position.contains("bottom")) y = r.y + r.height - h;
        else if (position.contains("center") || position.isBlank()) y = r.y + (r.height - h) / 2;
        return new Rectangle(x, y, w, h);
    }

    private BufferedImage loadBackgroundImage(String resource) {
        String key = normalizeImageUrl(stripQueryAndFragment(resource));
        if (key.isBlank()) return null;
        return BACKGROUND_IMAGE_CACHE.computeIfAbsent(key, this::readBackgroundImage);
    }

    private BufferedImage readBackgroundImage(String resource) {
        Path directPath = imageFilePath(resource);
        if (directPath != null && Files.isRegularFile(directPath)) {
            try {
                return ImageIO.read(directPath.toFile());
            } catch (IOException ignored) {
                return null;
            }
        }

        String classpathResource = stripLeadingSlash(resource);
        try (InputStream stream = HtmlDomPaintEngine.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (stream != null) return ImageIO.read(stream);
        } catch (IOException ignored) {
            return null;
        }

        Path relativePath = imageFilePath(classpathResource);
        if (relativePath != null && Files.isRegularFile(relativePath)) {
            try {
                return ImageIO.read(relativePath.toFile());
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    private Path imageFilePath(String resource) {
        if (resource == null || resource.isBlank()) return null;
        String value = resource.trim().replace('\\', '/');
        try {
            if (value.toLowerCase(Locale.ROOT).startsWith("file:")) return Path.of(URI.create(value));
            return Path.of(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String normalizeImageUrl(String resource) {
        return resource == null ? "" : resource.trim().replace('\\', '/');
    }

    private boolean containsUrl(String raw) {
        return raw != null && raw.toLowerCase(Locale.ROOT).contains("url(");
    }

    private String urlResource(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        int start = value.toLowerCase(Locale.ROOT).indexOf("url(");
        if (start < 0) return "";
        int bodyStart = start + 4;
        int bodyEnd = value.indexOf(')', bodyStart);
        if (bodyEnd < 0) return "";
        String out = value.substring(bodyStart, bodyEnd).trim();
        if ((out.startsWith("\"") && out.endsWith("\"")) || (out.startsWith("'") && out.endsWith("'"))) {
            out = out.substring(1, out.length() - 1).trim();
        }
        return out;
    }

    private String stripQueryAndFragment(String path) {
        if (path == null) return "";
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int cut = -1;
        if (query >= 0) cut = query;
        if (fragment >= 0) cut = cut < 0 ? fragment : Math.min(cut, fragment);
        return cut < 0 ? path : path.substring(0, cut);
    }

    private String stripLeadingSlash(String path) {
        String value = path == null ? "" : path.trim().replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        return value;
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
        Color parsed = color(gradientColorToken(value), null);
        if (parsed != null) colors.add(parsed);
    }

    private String gradientColorToken(String value) {
        String raw = value == null ? "" : value.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("rgb")) {
            int end = raw.indexOf(')');
            return end >= 0 ? raw.substring(0, end + 1) : raw;
        }
        int space = raw.indexOf(' ');
        return space < 0 ? raw : raw.substring(0, space);
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
