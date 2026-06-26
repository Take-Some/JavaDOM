package dev.takesome.htmldom.css.transition;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interpolates CSS transition values supported by the current engine UI runtime. */
public final class UiCssTransitionInterpolator {
    private static final Pattern NUMBER = Pattern.compile("^([-+]?(?:\\d+\\.?\\d*|\\.\\d+))([a-z%]*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RGB = Pattern.compile("^rgba?\\(([^)]*)\\)$", Pattern.CASE_INSENSITIVE);

    private UiCssTransitionInterpolator() {
    }

    public static String interpolate(String from, String to, double progress) {
        String start = clean(from);
        String end = clean(to);
        double t = clamp(progress);
        if (t >= 1.0 || start.equals(end)) return end;
        if (t <= 0.0) return start;

        NumericValue a = numeric(start);
        NumericValue b = numeric(end);
        if (a != null && b != null && a.unit.equals(b.unit)) return format(lerp(a.value, b.value, t), a.unit);

        ColorValue ca = color(start);
        ColorValue cb = color(end);
        if (ca != null && cb != null) {
            int r = (int) Math.round(lerp(ca.r, cb.r, t));
            int g = (int) Math.round(lerp(ca.g, cb.g, t));
            int bch = (int) Math.round(lerp(ca.b, cb.b, t));
            double alpha = lerp(ca.a, cb.a, t);
            if (alpha >= 0.999) return String.format(Locale.ROOT, "#%02x%02x%02x", clamp255(r), clamp255(g), clamp255(bch));
            return String.format(Locale.ROOT, "rgba(%d,%d,%d,%s)", clamp255(r), clamp255(g), clamp255(bch), trim(alpha));
        }

        TransformValue ta = transform(start);
        TransformValue tb = transform(end);
        if (ta != null && tb != null && ta.compatible(tb)) return transform(ta, tb, t);

        return t < 1.0 ? start : end;
    }

    public static boolean interpolable(String from, String to) {
        String start = clean(from);
        String end = clean(to);
        if (start.equals(end)) return true;
        NumericValue a = numeric(start);
        NumericValue b = numeric(end);
        if (a != null && b != null && a.unit.equals(b.unit)) return true;
        if (color(start) != null && color(end) != null) return true;
        TransformValue ta = transform(start);
        TransformValue tb = transform(end);
        return ta != null && tb != null && ta.compatible(tb);
    }

    private static NumericValue numeric(String value) {
        Matcher matcher = NUMBER.matcher(value.trim());
        if (!matcher.matches()) return null;
        try {
            return new NumericValue(Double.parseDouble(matcher.group(1)), matcher.group(2).toLowerCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static ColorValue color(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.startsWith("#")) return hexColor(v);
        Matcher matcher = RGB.matcher(v);
        if (!matcher.matches()) return namedColor(v);
        String[] parts = matcher.group(1).split("\\s*,\\s*");
        if (parts.length < 3) return null;
        try {
            int r = parseChannel(parts[0]);
            int g = parseChannel(parts[1]);
            int b = parseChannel(parts[2]);
            double a = parts.length > 3 ? Double.parseDouble(parts[3].trim()) : 1.0;
            return new ColorValue(r, g, b, clamp(a));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static ColorValue hexColor(String value) {
        try {
            if (value.length() == 4 || value.length() == 5) {
                int r = Integer.parseInt(value.substring(1, 2) + value.substring(1, 2), 16);
                int g = Integer.parseInt(value.substring(2, 3) + value.substring(2, 3), 16);
                int b = Integer.parseInt(value.substring(3, 4) + value.substring(3, 4), 16);
                double a = value.length() == 5
                        ? Integer.parseInt(value.substring(4, 5) + value.substring(4, 5), 16) / 255.0
                        : 1.0;
                return new ColorValue(r, g, b, a);
            }
            if (value.length() == 7 || value.length() == 9) {
                int r = Integer.parseInt(value.substring(1, 3), 16);
                int g = Integer.parseInt(value.substring(3, 5), 16);
                int b = Integer.parseInt(value.substring(5, 7), 16);
                double a = value.length() == 9 ? Integer.parseInt(value.substring(7, 9), 16) / 255.0 : 1.0;
                return new ColorValue(r, g, b, a);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static ColorValue namedColor(String value) {
        if ("black".equals(value)) return new ColorValue(0, 0, 0, 1.0);
        if ("white".equals(value)) return new ColorValue(255, 255, 255, 1.0);
        if ("red".equals(value)) return new ColorValue(255, 0, 0, 1.0);
        if ("green".equals(value)) return new ColorValue(0, 128, 0, 1.0);
        if ("blue".equals(value)) return new ColorValue(0, 0, 255, 1.0);
        if ("transparent".equals(value)) return new ColorValue(0, 0, 0, 0.0);
        return null;
    }

    private static int parseChannel(String raw) {
        String value = raw.trim();
        if (value.endsWith("%")) return clamp255((int) Math.round(Double.parseDouble(value.substring(0, value.length() - 1)) * 2.55));
        return clamp255((int) Math.round(Double.parseDouble(value)));
    }

    private static TransformValue transform(String raw) {
        String value = clean(raw).toLowerCase(Locale.ROOT);
        if (value.isBlank() || "none".equals(value)) {
            return new TransformValue(new NumericValue(0.0, ""), new NumericValue(0.0, ""), 1.0, 0.0);
        }

        boolean found = false;
        NumericValue x = new NumericValue(0.0, "");
        NumericValue y = new NumericValue(0.0, "");
        double scale = 1.0;
        double rotate = 0.0;

        String translate = functionArgs(value, "translate");
        if (translate != null) {
            String[] parts = translate.split("\\s*,\\s*", -1);
            NumericValue parsedX = parts.length > 0 ? numeric(parts[0]) : null;
            NumericValue parsedY = parts.length > 1 ? numeric(parts[1]) : null;
            if (parsedX == null || parsedY == null) return null;
            x = parsedX;
            y = parsedY;
            found = true;
        }

        String tx = functionArgs(value, "translatex");
        if (tx != null) {
            NumericValue parsed = numeric(tx);
            if (parsed == null) return null;
            x = parsed;
            found = true;
        }

        String ty = functionArgs(value, "translatey");
        if (ty != null) {
            NumericValue parsed = numeric(ty);
            if (parsed == null) return null;
            y = parsed;
            found = true;
        }

        String scaleArg = functionArgs(value, "scale");
        if (scaleArg != null) {
            int comma = scaleArg.indexOf(',');
            String first = comma >= 0 ? scaleArg.substring(0, comma).trim() : scaleArg.trim();
            try {
                scale = Double.parseDouble(first);
                found = true;
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        String rotateArg = functionArgs(value, "rotate");
        if (rotateArg != null) {
            try {
                rotate = angleDegrees(rotateArg);
                found = true;
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        return found ? new TransformValue(x, y, scale, rotate) : null;
    }

    private static double angleDegrees(String raw) {
        String value = clean(raw).toLowerCase(Locale.ROOT);
        if (value.endsWith("deg")) return Double.parseDouble(value.substring(0, value.length() - 3).trim());
        if (value.endsWith("turn")) return Double.parseDouble(value.substring(0, value.length() - 4).trim()) * 360.0;
        if (value.endsWith("rad")) return Math.toDegrees(Double.parseDouble(value.substring(0, value.length() - 3).trim()));
        return Double.parseDouble(value);
    }

    private static String functionArgs(String value, String name) {
        String token = name + "(";
        int start = value.indexOf(token);
        if (start < 0) return null;
        int open = value.indexOf('(', start);
        int close = value.indexOf(')', open + 1);
        if (open < 0 || close <= open) return null;
        return value.substring(open + 1, close).trim();
    }

    private static String transform(TransformValue a, TransformValue b, double t) {
        String x = format(lerp(a.x.value, b.x.value, t), a.x.unit);
        String y = format(lerp(a.y.value, b.y.value, t), a.y.unit);
        String scale = trim(lerp(a.scale, b.scale, t));
        String rotate = trim(lerp(a.rotate, b.rotate, t));
        return "translate(" + x + ", " + y + ") scale(" + scale + ") rotate(" + rotate + "deg)";
    }

    private static String format(double value, String unit) {
        return trim(value) + unit;
    }

    private static String trim(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) return Long.toString(Math.round(value));
        String out = String.format(Locale.ROOT, "%.4f", value);
        while (out.contains(".") && out.endsWith("0")) out = out.substring(0, out.length() - 1);
        if (out.endsWith(".")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }

    private static double clamp(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private static int clamp255(int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }

    private record NumericValue(double value, String unit) { }
    private record ColorValue(int r, int g, int b, double a) { }
    private record TransformValue(NumericValue x, NumericValue y, double scale, double rotate) {
        boolean compatible(TransformValue other) {
            return other != null && x.unit.equals(other.x.unit) && y.unit.equals(other.y.unit);
        }
    }
}
