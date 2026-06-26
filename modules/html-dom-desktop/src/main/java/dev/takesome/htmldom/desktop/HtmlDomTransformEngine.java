package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Applies CSS transform/translate/scale/rotate to Java2D painting and hit-test geometry. */
public final class HtmlDomTransformEngine {
    public boolean apply(Graphics2D g, UiDomElement element, Rectangle r) {
        if (g == null || element == null || r == null) return false;
        AffineTransform tx = transform(element, r);
        if (tx == null || tx.isIdentity()) return false;
        g.transform(tx);
        return true;
    }

    public AffineTransform transform(UiDomElement element, Rectangle r) {
        AffineTransform ops = new AffineTransform();
        appendTranslateProperty(ops, element.style("translate", ""), r);
        appendRotateProperty(ops, element.style("rotate", ""));
        appendScaleProperty(ops, element.style("scale", ""));
        appendTransformFunctions(ops, element.style("transform", ""), r);
        if (ops.isIdentity()) return new AffineTransform();
        double ox = originX(element.style("transform-origin", ""), r);
        double oy = originY(element.style("transform-origin", ""), r);
        AffineTransform out = new AffineTransform();
        out.translate(ox, oy);
        out.concatenate(ops);
        out.translate(-ox, -oy);
        return out;
    }

    private void appendTransformFunctions(AffineTransform tx, String raw, Rectangle r) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return;
        for (FunctionCall fn : functions(value)) {
            String name = fn.name.toLowerCase(Locale.ROOT);
            List<String> args = splitArgs(fn.args);
            switch (name) {
                case "translate" -> tx.translate(length(arg(args, 0), r.width, 0), length(arg(args, 1), r.height, 0));
                case "translatex" -> tx.translate(length(arg(args, 0), r.width, 0), 0);
                case "translatey" -> tx.translate(0, length(arg(args, 0), r.height, 0));
                case "scale" -> {
                    double sx = number(arg(args, 0), 1);
                    double sy = args.size() > 1 ? number(arg(args, 1), sx) : sx;
                    tx.scale(sx, sy);
                }
                case "scalex" -> tx.scale(number(arg(args, 0), 1), 1);
                case "scaley" -> tx.scale(1, number(arg(args, 0), 1));
                case "rotate" -> tx.rotate(angle(arg(args, 0)));
                case "matrix" -> {
                    if (args.size() >= 6) {
                        tx.concatenate(new AffineTransform(
                                number(arg(args, 0), 1), number(arg(args, 1), 0),
                                number(arg(args, 2), 0), number(arg(args, 3), 1),
                                length(arg(args, 4), r.width, 0), length(arg(args, 5), r.height, 0)));
                    }
                }
                default -> { }
            }
        }
    }

    private void appendTranslateProperty(AffineTransform tx, String raw, Rectangle r) {
        List<String> args = splitArgs(raw);
        if (args.isEmpty() || "none".equalsIgnoreCase(args.get(0))) return;
        tx.translate(length(arg(args, 0), r.width, 0), length(arg(args, 1), r.height, 0));
    }

    private void appendScaleProperty(AffineTransform tx, String raw) {
        List<String> args = splitArgs(raw);
        if (args.isEmpty() || "none".equalsIgnoreCase(args.get(0))) return;
        double sx = number(arg(args, 0), 1);
        double sy = args.size() > 1 ? number(arg(args, 1), sx) : sx;
        tx.scale(sx, sy);
    }

    private void appendRotateProperty(AffineTransform tx, String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return;
        tx.rotate(angle(value));
    }

    private double originX(String raw, Rectangle r) {
        List<String> args = splitArgs(raw == null || raw.isBlank() ? "50% 50%" : raw);
        return r.x + origin(arg(args, 0), r.width, 0.5);
    }

    private double originY(String raw, Rectangle r) {
        List<String> args = splitArgs(raw == null || raw.isBlank() ? "50% 50%" : raw);
        return r.y + origin(arg(args, 1), r.height, 0.5);
    }

    private double origin(String raw, double ref, double fallbackRatio) {
        String v = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (v.isBlank()) return ref * fallbackRatio;
        return switch (v) {
            case "left", "top" -> 0;
            case "center" -> ref * 0.5;
            case "right", "bottom" -> ref;
            default -> length(v, ref, ref * fallbackRatio);
        };
    }

    private List<FunctionCall> functions(String raw) {
        ArrayList<FunctionCall> out = new ArrayList<>();
        int i = 0;
        while (i < raw.length()) {
            while (i < raw.length() && Character.isWhitespace(raw.charAt(i))) i++;
            int nameStart = i;
            while (i < raw.length() && (Character.isLetter(raw.charAt(i)) || raw.charAt(i) == '-')) i++;
            if (i <= nameStart || i >= raw.length() || raw.charAt(i) != '(') break;
            String name = raw.substring(nameStart, i);
            int open = i++;
            int depth = 1;
            while (i < raw.length() && depth > 0) {
                char ch = raw.charAt(i++);
                if (ch == '(') depth++;
                else if (ch == ')') depth--;
            }
            if (depth == 0) out.add(new FunctionCall(name, raw.substring(open + 1, i - 1)));
        }
        return out;
    }

    private List<String> splitArgs(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) return List.of();
        ArrayList<String> out = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '(') depth++;
            if (ch == ')') depth--;
            if (depth == 0 && (ch == ',' || Character.isWhitespace(ch))) {
                if (!token.isEmpty()) {
                    out.add(token.toString().trim());
                    token.setLength(0);
                }
                continue;
            }
            token.append(ch);
        }
        if (!token.isEmpty()) out.add(token.toString().trim());
        return out;
    }

    private String arg(List<String> args, int index) {
        return index >= 0 && index < args.size() ? args.get(index) : "";
    }

    private double length(String raw, double reference, double fallback) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) return fallback;
        try {
            if (value.endsWith("%")) return Double.parseDouble(value.substring(0, value.length() - 1).trim()) * reference / 100.0;
            if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
            return Double.parseDouble(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private double number(String raw, double fallback) {
        try { return raw == null || raw.isBlank() ? fallback : Double.parseDouble(raw.trim()); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private double angle(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("turn")) return Double.parseDouble(value.substring(0, value.length() - 4).trim()) * Math.PI * 2.0;
            if (value.endsWith("rad")) return Double.parseDouble(value.substring(0, value.length() - 3).trim());
            if (value.endsWith("deg")) return Math.toRadians(Double.parseDouble(value.substring(0, value.length() - 3).trim()));
            return Math.toRadians(Double.parseDouble(value));
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private record FunctionCall(String name, String args) { }
}
