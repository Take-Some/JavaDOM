package dev.takesome.htmldom.css.animation;

import dev.takesome.htmldom.css.UiCssKeyframe;
import dev.takesome.htmldom.css.UiCssKeyframesRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compiled keyframe segments with typed value interpolators for animation hot paths. */
public final class UiCssCompiledKeyframes {
    private static final Pattern NUMBER = Pattern.compile("^([-+]?(?:\\d+\\.?\\d*|\\.\\d+))([a-z%]*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RGB = Pattern.compile("^rgba?\\(([^)]*)\\)$", Pattern.CASE_INSENSITIVE);
    private static final double EPSILON = 0.000001;

    private final UiCssKeyframesRule rule;
    private final List<UiCssKeyframe> frames;
    private final List<Segment> segments;
    private final int typedInterpolatorCount;
    private final int discreteInterpolatorCount;

    public UiCssCompiledKeyframes(UiCssKeyframesRule rule) {
        this.rule = rule;
        this.frames = rule == null ? List.of() : rule.frames();
        ArrayList<Segment> nextSegments = new ArrayList<>();
        int typed = 0;
        int discrete = 0;
        for (int i = 0; i + 1 < frames.size(); i++) {
            Segment segment = compile(frames.get(i), frames.get(i + 1));
            nextSegments.add(segment);
            typed += segment.typedInterpolatorCount();
            discrete += segment.discreteInterpolatorCount();
        }
        this.segments = List.copyOf(nextSegments);
        this.typedInterpolatorCount = typed;
        this.discreteInterpolatorCount = discrete;
    }

    public String name() {
        return rule == null ? "" : rule.name();
    }

    public int segmentCount() {
        return segments.size();
    }

    public int typedInterpolatorCount() {
        return typedInterpolatorCount;
    }

    public int discreteInterpolatorCount() {
        return discreteInterpolatorCount;
    }

    public boolean empty() {
        return rule == null || rule.empty();
    }

    public Map<String, String> sample(double progress) {
        if (frames.isEmpty()) return Map.of();
        double t = clamp01(progress);
        UiCssKeyframe first = frames.get(0);
        UiCssKeyframe last = frames.get(frames.size() - 1);
        if (frames.size() == 1 || t <= first.offset()) return first.declarations();
        if (t >= last.offset()) return last.declarations();
        Segment segment = segmentFor(t);
        if (segment == null || segment.empty()) return Map.of();
        return segment.sample(t);
    }

    private Segment segmentFor(double progress) {
        for (Segment segment : segments) {
            if (progress >= segment.startOffset() - EPSILON && progress <= segment.endOffset() + EPSILON) return segment;
        }
        return segments.isEmpty() ? null : segments.get(segments.size() - 1);
    }

    private Segment compile(UiCssKeyframe prev, UiCssKeyframe next) {
        double start = prev.offset();
        double end = next.offset();
        if (Math.abs(end - start) < EPSILON) return new Segment(start, end, prev.declarations(), List.of());
        ArrayList<PropertySampler> samplers = new ArrayList<>();
        for (Map.Entry<String, String> entry : next.declarations().entrySet()) {
            String property = entry.getKey();
            String from = prev.declarations().get(property);
            String to = entry.getValue();
            samplers.add(PropertySampler.compile(property, from, to));
        }
        return new Segment(start, end, prev.declarations(), List.copyOf(samplers));
    }

    private static final class Segment {
        private final double startOffset;
        private final double endOffset;
        private final Map<String, String> baseDeclarations;
        private final List<PropertySampler> samplers;
        private final int typedInterpolatorCount;
        private final int discreteInterpolatorCount;

        private Segment(double startOffset, double endOffset, Map<String, String> baseDeclarations, List<PropertySampler> samplers) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.baseDeclarations = baseDeclarations == null ? Map.of() : Map.copyOf(baseDeclarations);
            this.samplers = samplers == null ? List.of() : List.copyOf(samplers);
            int typed = 0;
            int discrete = 0;
            for (PropertySampler sampler : this.samplers) {
                if (sampler.typed()) typed++;
                else discrete++;
            }
            this.typedInterpolatorCount = typed;
            this.discreteInterpolatorCount = discrete;
        }

        private double startOffset() { return startOffset; }
        private double endOffset() { return endOffset; }
        private boolean empty() { return baseDeclarations.isEmpty() && samplers.isEmpty(); }
        private int typedInterpolatorCount() { return typedInterpolatorCount; }
        private int discreteInterpolatorCount() { return discreteInterpolatorCount; }

        private Map<String, String> sample(double progress) {
            if (samplers.isEmpty()) return baseDeclarations;
            double local = clamp01((progress - startOffset) / (endOffset - startOffset));
            LinkedHashMap<String, String> out = new LinkedHashMap<>(baseDeclarations);
            for (PropertySampler sampler : samplers) out.put(sampler.property(), sampler.sample(local));
            return out;
        }
    }

    private sealed interface ValueSampler permits NumericSampler, ColorSampler, TransformSampler, DiscreteSampler {
        String sample(double progress);
        boolean typed();
    }

    private record PropertySampler(String property, ValueSampler valueSampler) {
        private static PropertySampler compile(String property, String from, String to) {
            return new PropertySampler(property, compileValue(property, from, to));
        }

        private String sample(double progress) {
            return valueSampler.sample(progress);
        }

        private boolean typed() {
            return valueSampler.typed();
        }

        private static ValueSampler compileValue(String property, String from, String to) {
            if (from != null) {
                NumericValue a = numeric(from);
                NumericValue b = numeric(to);
                if (a != null && b != null && a.unit.equals(b.unit)) return new NumericSampler(a, b);

                ColorValue ca = color(from);
                ColorValue cb = color(to);
                if (ca != null && cb != null) return new ColorSampler(ca, cb);

                TransformValue ta = transform(from);
                TransformValue tb = transform(to);
                if (ta != null && tb != null && ta.compatible(tb)) return new TransformSampler(ta, tb);
            }
            return new DiscreteSampler(from, to);
        }
    }

    private record NumericSampler(NumericValue from, NumericValue to) implements ValueSampler {
        @Override public String sample(double progress) {
            double t = clamp01(progress);
            if (t >= 1.0) return format(to.value, to.unit);
            if (t <= 0.0) return format(from.value, from.unit);
            return format(lerp(from.value, to.value, t), from.unit);
        }

        @Override public boolean typed() { return true; }
    }

    private record ColorSampler(ColorValue from, ColorValue to) implements ValueSampler {
        @Override public String sample(double progress) {
            double t = clamp01(progress);
            if (t >= 1.0) return to.format();
            if (t <= 0.0) return from.format();
            int r = (int) Math.round(lerp(from.r, to.r, t));
            int g = (int) Math.round(lerp(from.g, to.g, t));
            int b = (int) Math.round(lerp(from.b, to.b, t));
            double a = lerp(from.a, to.a, t);
            return new ColorValue(r, g, b, a).format();
        }

        @Override public boolean typed() { return true; }
    }

    private record TransformSampler(TransformValue from, TransformValue to) implements ValueSampler {
        @Override public String sample(double progress) {
            double t = clamp01(progress);
            if (t >= 1.0) return to.format();
            if (t <= 0.0) return from.format();
            NumericValue x = new NumericValue(lerp(from.x.value, to.x.value, t), from.x.unit);
            NumericValue y = new NumericValue(lerp(from.y.value, to.y.value, t), from.y.unit);
            double scale = lerp(from.scale, to.scale, t);
            double rotate = lerp(from.rotate, to.rotate, t);
            return new TransformValue(x, y, scale, rotate).format();
        }

        @Override public boolean typed() { return true; }
    }

    private record DiscreteSampler(String from, String to) implements ValueSampler {
        @Override public String sample(double progress) {
            return progress < 1.0 ? (from == null ? to : from) : to;
        }

        @Override public boolean typed() { return false; }
    }

    private static NumericValue numeric(String value) {
        String cleaned = clean(value);
        Matcher matcher = NUMBER.matcher(cleaned);
        if (!matcher.matches()) return null;
        try {
            return new NumericValue(Double.parseDouble(matcher.group(1)), matcher.group(2).toLowerCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static ColorValue color(String value) {
        String v = clean(value).toLowerCase(Locale.ROOT);
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
            return new ColorValue(r, g, b, clamp01(a));
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
                double a = value.length() == 5 ? Integer.parseInt(value.substring(4, 5) + value.substring(4, 5), 16) / 255.0 : 1.0;
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
        return switch (value) {
            case "black" -> new ColorValue(0, 0, 0, 1.0);
            case "white" -> new ColorValue(255, 255, 255, 1.0);
            case "red" -> new ColorValue(255, 0, 0, 1.0);
            case "green" -> new ColorValue(0, 128, 0, 1.0);
            case "blue" -> new ColorValue(0, 0, 255, 1.0);
            case "transparent" -> new ColorValue(0, 0, 0, 0.0);
            default -> null;
        };
    }

    private static int parseChannel(String raw) {
        String value = clean(raw);
        if (value.endsWith("%")) return clamp255((int) Math.round(Double.parseDouble(value.substring(0, value.length() - 1)) * 2.55));
        return clamp255((int) Math.round(Double.parseDouble(value)));
    }

    private static TransformValue transform(String raw) {
        String value = clean(raw).toLowerCase(Locale.ROOT);
        if (value.isBlank() || "none".equals(value)) return new TransformValue(new NumericValue(0.0, ""), new NumericValue(0.0, ""), 1.0, 0.0);

        boolean found = false;
        NumericValue x = new NumericValue(0.0, "");
        NumericValue y = new NumericValue(0.0, "");
        double scale = 1.0;
        double rotate = 0.0;

        String translate = functionArgs(value, "translate");
        if (translate != null) {
            String[] parts = translate.split("\\s*,\\s*", -1);
            NumericValue parsedX = parts.length > 0 ? numeric(parts[0]) : null;
            NumericValue parsedY = parts.length > 1 ? numeric(parts[1]) : new NumericValue(0.0, parsedX == null ? "" : parsedX.unit);
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
            String first = scaleArg.contains(",") ? scaleArg.substring(0, scaleArg.indexOf(',')).trim() : scaleArg.trim();
            try {
                scale = Double.parseDouble(first);
                found = true;
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        String scaleXArg = functionArgs(value, "scalex");
        if (scaleXArg != null) {
            try {
                scale = Double.parseDouble(scaleXArg.trim());
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

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private static int clamp255(int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
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

    private record NumericValue(double value, String unit) { }

    private record ColorValue(int r, int g, int b, double a) {
        private String format() {
            if (a >= 0.999) return String.format(Locale.ROOT, "#%02x%02x%02x", clamp255(r), clamp255(g), clamp255(b));
            return String.format(Locale.ROOT, "rgba(%d,%d,%d,%s)", clamp255(r), clamp255(g), clamp255(b), trim(clamp01(a)));
        }
    }

    private record TransformValue(NumericValue x, NumericValue y, double scale, double rotate) {
        private boolean compatible(TransformValue other) {
            return other != null && x.unit.equals(other.x.unit) && y.unit.equals(other.y.unit);
        }

        private String format() {
            return "translate(" + UiCssCompiledKeyframes.format(x.value, x.unit) + ", "
                    + UiCssCompiledKeyframes.format(y.value, y.unit) + ") scale(" + trim(scale)
                    + ") rotate(" + trim(rotate) + "deg)";
        }
    }
}
