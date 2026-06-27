package dev.takesome.htmldom.desktop;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CSS value classification and formatting helpers for the painted DevTools inspector. */
final class HtmlDomDevToolsCssValue {
    private static final Pattern METRIC_PATTERN = Pattern.compile("^([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))([a-z%]*)$", Pattern.CASE_INSENSITIVE);

    private HtmlDomDevToolsCssValue() {
    }

    static boolean colorProperty(String property) {
        String p = normalize(property);
        return p.equals("color")
                || p.equals("text-color")
                || p.equals("background-color")
                || p.equals("border-color")
                || p.equals("border-top-color")
                || p.equals("border-right-color")
                || p.equals("border-bottom-color")
                || p.equals("border-left-color")
                || p.equals("outline-color")
                || p.equals("box-shadow-color")
                || p.equals("shadow-color")
                || p.equals("icon-color")
                || p.equals("inner-color")
                || p.equals("check-color")
                || p.endsWith("-color");
    }

    static Color parseColor(String raw) {
        String value = normalize(raw);
        if (value.isBlank() || value.startsWith("var(")) return null;
        if ("transparent".equals(value)) return new Color(0, 0, 0, 0);
        if (value.startsWith("#")) return parseHex(value);
        if (value.startsWith("rgb")) return parseRgb(value);
        Color named = HtmlDomNamedColors.lookup(value);
        return named == null ? null : named;
    }

    static boolean colorValue(String property, String value) {
        return colorProperty(property) || parseColor(value) != null;
    }

    static String formatColor(Color color) {
        if (color == null) return "transparent";
        if (color.getAlpha() >= 255) {
            return String.format(Locale.ROOT, "#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }
        String alpha = trimNumber(color.getAlpha() / 255.0);
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%s)", color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    static Optional<Metric> metric(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || value.contains(" ") || value.startsWith("var(")) return Optional.empty();
        Matcher matcher = METRIC_PATTERN.matcher(value);
        if (!matcher.matches()) return Optional.empty();
        try {
            double number = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
            return Optional.of(new Metric(number, unit));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    static boolean metricValue(String raw) {
        return metric(raw).isPresent();
    }

    static String nudgeMetric(String raw, int direction, boolean largeStep, boolean fineStep) {
        Metric metric = metric(raw).orElse(null);
        if (metric == null) return raw == null ? "" : raw;
        double step = stepFor(metric.unit());
        if (largeStep) step *= 10.0;
        if (fineStep) step /= 10.0;
        double next = metric.value() + Math.signum(direction) * step;
        if (metric.unit().isBlank() && Math.abs(next) < 0.000_001) next = 0.0;
        return trimNumber(next) + metric.unit();
    }

    static List<String> keywordOptions(String property) {
        String p = normalize(property);
        if (p.equals("display")) return List.of("none", "block", "inline", "inline-block", "flex", "inline-flex");
        if (p.equals("position")) return List.of("static", "relative", "absolute", "fixed", "sticky");
        if (p.equals("overflow") || p.equals("overflow-x") || p.equals("overflow-y")) return List.of("visible", "hidden", "clip", "auto", "scroll");
        if (p.equals("text-align") || p.equals("align")) return List.of("left", "center", "right", "start", "end", "justify");
        if (p.equals("vertical-align")) return List.of("baseline", "top", "middle", "bottom", "text-top", "text-bottom");
        if (p.equals("white-space")) return List.of("normal", "nowrap", "pre", "pre-wrap", "pre-line");
        if (p.equals("visibility")) return List.of("visible", "hidden", "collapse");
        if (p.equals("pointer-events")) return List.of("auto", "none");
        if (p.equals("box-sizing")) return List.of("content-box", "border-box");
        if (p.equals("cursor")) return List.of("default", "pointer", "text", "move", "not-allowed", "grab", "grabbing");
        if (p.equals("flex-direction")) return List.of("row", "row-reverse", "column", "column-reverse");
        if (p.equals("flex-wrap")) return List.of("nowrap", "wrap", "wrap-reverse");
        if (p.equals("justify-content")) return List.of("flex-start", "center", "flex-end", "space-between", "space-around", "space-evenly");
        if (p.equals("align-items") || p.equals("align-content") || p.equals("align-self")) return List.of("stretch", "flex-start", "center", "flex-end", "baseline");
        if (p.equals("object-fit")) return List.of("fill", "contain", "cover", "none", "scale-down");
        if (p.equals("background-size")) return List.of("auto", "cover", "contain", "fill");
        if (p.equals("text-transform")) return List.of("none", "uppercase", "lowercase", "capitalize");
        if (p.equals("font-style")) return List.of("normal", "italic", "oblique");
        if (p.equals("font-weight")) return List.of("normal", "bold", "lighter", "bolder", "100", "200", "300", "400", "500", "600", "700", "800", "900");
        if (p.equals("border-style") || p.equals("outline-style") || p.endsWith("-border-style") || p.endsWith("-style") && p.contains("border")) {
            return List.of("none", "hidden", "solid", "dotted", "dashed", "double", "groove", "ridge", "inset", "outset");
        }
        return List.of();
    }

    static boolean keywordProperty(String property) {
        return !keywordOptions(property).isEmpty();
    }

    private static Color parseHex(String value) {
        try {
            String hex = value.substring(1).trim();
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            } else if (hex.length() == 4) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2) + hex.charAt(3) + hex.charAt(3);
            }
            if (hex.length() == 6) return new Color(Integer.parseInt(hex, 16));
            if (hex.length() == 8) {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                int a = Integer.parseInt(hex.substring(6, 8), 16);
                return new Color(r, g, b, a);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static Color parseRgb(String value) {
        try {
            int start = value.indexOf('(');
            int end = value.lastIndexOf(')');
            if (start < 0 || end <= start) return null;
            String inside = value.substring(start + 1, end).trim().replace('/', ',');
            String[] parts = inside.split(",");
            if (parts.length < 3) return null;
            int r = colorChannel(parts[0]);
            int g = colorChannel(parts[1]);
            int b = colorChannel(parts[2]);
            int a = parts.length > 3 ? alpha(parts[3]) : 255;
            return new Color(r, g, b, a);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int colorChannel(String raw) {
        String value = normalize(raw);
        if (value.endsWith("%")) return clamp255(Math.round(Float.parseFloat(value.substring(0, value.length() - 1)) * 2.55f));
        return clamp255(Math.round(Float.parseFloat(value)));
    }

    private static int alpha(String raw) {
        String value = normalize(raw);
        if (value.endsWith("%")) return clamp255(Math.round(Float.parseFloat(value.substring(0, value.length() - 1)) * 2.55f));
        float parsed = Float.parseFloat(value);
        if (parsed <= 1f) return clamp255(Math.round(parsed * 255f));
        return clamp255(Math.round(parsed));
    }

    private static double stepFor(String unit) {
        return switch (unit == null ? "" : unit) {
            case "s" -> 0.1;
            case "ms" -> 50.0;
            case "deg" -> 1.0;
            case "turn" -> 0.01;
            case "em", "rem" -> 0.1;
            default -> 1.0;
        };
    }

    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000_001) return Long.toString(Math.round(value));
        String out = String.format(Locale.ROOT, "%.4f", value);
        while (out.contains(".") && out.endsWith("0")) out = out.substring(0, out.length() - 1);
        if (out.endsWith(".")) out = out.substring(0, out.length() - 1);
        return out;
    }

    record Metric(double value, String unit) { }
}
