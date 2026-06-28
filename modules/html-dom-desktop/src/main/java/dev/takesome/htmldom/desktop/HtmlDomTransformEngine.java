package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Applies CSS transform/translate/scale/rotate to Java2D painting and hit-test geometry. */
public final class HtmlDomTransformEngine {
    private final Map<Integer, CachedTransformPlan> planCache = new HashMap<>();
    private long cacheHits;
    private long cacheMisses;
    private long parsedPlans;

    public boolean apply(Graphics2D g, UiDomElement element, Rectangle r) {
        if (g == null || element == null || r == null) return false;
        AffineTransform tx = transform(element, r);
        if (tx == null || tx.isIdentity()) return false;
        g.transform(tx);
        return true;
    }

    public AffineTransform transform(UiDomElement element, Rectangle r) {
        if (element == null || r == null) return new AffineTransform();
        TransformPlan plan = plan(element);
        if (plan.identity()) return new AffineTransform();
        return plan.transform(r);
    }


    public Rectangle transformedBounds(UiDomElement element, Rectangle r) {
        if (element == null || r == null) return null;
        AffineTransform tx = transform(element, r);
        if (tx == null || tx.isIdentity()) return new Rectangle(r);
        Shape transformed = tx.createTransformedShape(r);
        return transformed.getBounds();
    }

    public Stats stats() {
        return new Stats(planCache.size(), cacheHits, cacheMisses, parsedPlans);
    }

    public void clearCache() {
        planCache.clear();
        cacheHits = 0L;
        cacheMisses = 0L;
        parsedPlans = 0L;
    }

    private TransformPlan plan(UiDomElement element) {
        int nodeId = element.nodeId();
        StyleKey key = StyleKey.from(element);
        CachedTransformPlan cached = planCache.get(nodeId);
        if (cached != null && cached.key.equals(key)) {
            cacheHits++;
            return cached.plan;
        }
        cacheMisses++;
        TransformPlan plan = key.none() ? TransformPlan.identityPlan() : parsePlan(key);
        if (!key.none()) parsedPlans++;
        planCache.put(nodeId, new CachedTransformPlan(key, plan));
        return plan;
    }

    private TransformPlan parsePlan(StyleKey key) {
        ArrayList<TransformOperation> operations = new ArrayList<>();
        appendTranslateProperty(operations, key.translate());
        appendRotateProperty(operations, key.rotate());
        appendScaleProperty(operations, key.scale());
        appendTransformFunctions(operations, key.transform());
        if (operations.isEmpty()) return TransformPlan.identityPlan();
        return new TransformPlan(List.copyOf(operations), TransformOrigin.parse(key.origin()));
    }

    private void appendTransformFunctions(List<TransformOperation> operations, String raw) {
        String value = normalize(raw);
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return;
        for (FunctionCall fn : functions(value)) {
            String name = fn.name().toLowerCase(Locale.ROOT);
            List<String> args = splitArgs(fn.args());
            switch (name) {
                case "translate" -> operations.add(new TranslateOperation(
                        LengthValue.parse(arg(args, 0), 0.0),
                        LengthValue.parse(arg(args, 1), 0.0)
                ));
                case "translatex" -> operations.add(new TranslateOperation(LengthValue.parse(arg(args, 0), 0.0), LengthValue.absolute(0.0)));
                case "translatey" -> operations.add(new TranslateOperation(LengthValue.absolute(0.0), LengthValue.parse(arg(args, 0), 0.0)));
                case "scale" -> {
                    double sx = number(arg(args, 0), 1.0);
                    double sy = args.size() > 1 ? number(arg(args, 1), sx) : sx;
                    operations.add(new ScaleOperation(sx, sy));
                }
                case "scalex" -> operations.add(new ScaleOperation(number(arg(args, 0), 1.0), 1.0));
                case "scaley" -> operations.add(new ScaleOperation(1.0, number(arg(args, 0), 1.0)));
                case "rotate" -> operations.add(new RotateOperation(angle(arg(args, 0))));
                case "matrix" -> {
                    if (args.size() >= 6) {
                        operations.add(new MatrixOperation(
                                number(arg(args, 0), 1.0), number(arg(args, 1), 0.0),
                                number(arg(args, 2), 0.0), number(arg(args, 3), 1.0),
                                LengthValue.parse(arg(args, 4), 0.0), LengthValue.parse(arg(args, 5), 0.0)));
                    }
                }
                default -> { }
            }
        }
    }

    private void appendTranslateProperty(List<TransformOperation> operations, String raw) {
        List<String> args = splitArgs(raw);
        if (args.isEmpty() || "none".equalsIgnoreCase(args.get(0))) return;
        operations.add(new TranslateOperation(LengthValue.parse(arg(args, 0), 0.0), LengthValue.parse(arg(args, 1), 0.0)));
    }

    private void appendScaleProperty(List<TransformOperation> operations, String raw) {
        List<String> args = splitArgs(raw);
        if (args.isEmpty() || "none".equalsIgnoreCase(args.get(0))) return;
        double sx = number(arg(args, 0), 1.0);
        double sy = args.size() > 1 ? number(arg(args, 1), sx) : sx;
        operations.add(new ScaleOperation(sx, sy));
    }

    private void appendRotateProperty(List<TransformOperation> operations, String raw) {
        String value = normalize(raw);
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return;
        operations.add(new RotateOperation(angle(value)));
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
        String value = normalize(raw);
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

    private static double number(String raw, double fallback) {
        try { return raw == null || raw.isBlank() ? fallback : Double.parseDouble(raw.trim()); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static double angle(String raw) {
        String value = normalize(raw).toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("turn")) return Double.parseDouble(value.substring(0, value.length() - 4).trim()) * Math.PI * 2.0;
            if (value.endsWith("rad")) return Double.parseDouble(value.substring(0, value.length() - 3).trim());
            if (value.endsWith("deg")) return Math.toRadians(Double.parseDouble(value.substring(0, value.length() - 3).trim()));
            return Math.toRadians(Double.parseDouble(value));
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record Stats(int cachedPlans, long cacheHits, long cacheMisses, long parsedPlans) { }

    private record CachedTransformPlan(StyleKey key, TransformPlan plan) { }

    private record StyleKey(String translate, String rotate, String scale, String transform, String origin) {
        private static StyleKey from(UiDomElement element) {
            return new StyleKey(
                    normalize(element.style("translate", "")),
                    normalize(element.style("rotate", "")),
                    normalize(element.style("scale", "")),
                    normalize(element.style("transform", "")),
                    normalize(element.style("transform-origin", ""))
            );
        }

        private boolean none() {
            return noneValue(translate) && noneValue(rotate) && noneValue(scale) && noneValue(transform);
        }

        private static boolean noneValue(String value) {
            String v = normalize(value);
            return v.isBlank() || "none".equalsIgnoreCase(v);
        }
    }

    private record TransformPlan(List<TransformOperation> operations, TransformOrigin origin) {
        private static TransformPlan identityPlan() {
            return new TransformPlan(List.of(), TransformOrigin.defaultOrigin());
        }

        private boolean identity() {
            return operations.isEmpty();
        }

        private AffineTransform transform(Rectangle r) {
            if (identity()) return new AffineTransform();
            AffineTransform ops = new AffineTransform();
            for (TransformOperation operation : operations) operation.apply(ops, r);
            if (ops.isIdentity()) return new AffineTransform();
            double ox = origin.x(r);
            double oy = origin.y(r);
            AffineTransform out = new AffineTransform();
            out.translate(ox, oy);
            out.concatenate(ops);
            out.translate(-ox, -oy);
            return out;
        }
    }

    private sealed interface TransformOperation permits TranslateOperation, ScaleOperation, RotateOperation, MatrixOperation {
        void apply(AffineTransform tx, Rectangle r);
    }

    private record TranslateOperation(LengthValue x, LengthValue y) implements TransformOperation {
        @Override public void apply(AffineTransform tx, Rectangle r) {
            tx.translate(x.resolve(r.width), y.resolve(r.height));
        }
    }

    private record ScaleOperation(double sx, double sy) implements TransformOperation {
        @Override public void apply(AffineTransform tx, Rectangle r) {
            tx.scale(sx, sy);
        }
    }

    private record RotateOperation(double radians) implements TransformOperation {
        @Override public void apply(AffineTransform tx, Rectangle r) {
            tx.rotate(radians);
        }
    }

    private record MatrixOperation(double m00, double m10, double m01, double m11, LengthValue txValue, LengthValue tyValue) implements TransformOperation {
        @Override public void apply(AffineTransform tx, Rectangle r) {
            tx.concatenate(new AffineTransform(m00, m10, m01, m11, txValue.resolve(r.width), tyValue.resolve(r.height)));
        }
    }

    private record TransformOrigin(LengthValue x, LengthValue y) {
        private static TransformOrigin defaultOrigin() {
            return new TransformOrigin(LengthValue.percent(50.0), LengthValue.percent(50.0));
        }

        private static TransformOrigin parse(String raw) {
            List<String> args = staticSplitArgs(raw == null || raw.isBlank() ? "50% 50%" : raw);
            return new TransformOrigin(originValue(argStatic(args, 0), true), originValue(argStatic(args, 1), false));
        }

        private double x(Rectangle r) { return r.x + x.resolve(r.width); }
        private double y(Rectangle r) { return r.y + y.resolve(r.height); }

        private static LengthValue originValue(String raw, boolean horizontal) {
            String v = normalize(raw).toLowerCase(Locale.ROOT);
            if (v.isBlank()) return LengthValue.percent(50.0);
            return switch (v) {
                case "left", "top" -> LengthValue.absolute(0.0);
                case "center" -> LengthValue.percent(50.0);
                case "right", "bottom" -> LengthValue.percent(100.0);
                default -> LengthValue.parse(v, 0.0);
            };
        }
    }

    private enum LengthUnit { PX, PERCENT }

    private record LengthValue(double value, LengthUnit unit) {
        private static LengthValue absolute(double value) { return new LengthValue(value, LengthUnit.PX); }
        private static LengthValue percent(double value) { return new LengthValue(value, LengthUnit.PERCENT); }

        private static LengthValue parse(String raw, double fallback) {
            String value = normalize(raw).toLowerCase(Locale.ROOT);
            if (value.isBlank()) return absolute(fallback);
            try {
                if (value.endsWith("%")) return percent(Double.parseDouble(value.substring(0, value.length() - 1).trim()));
                if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
                return absolute(Double.parseDouble(value));
            } catch (RuntimeException ignored) {
                return absolute(fallback);
            }
        }

        private double resolve(double reference) {
            return unit == LengthUnit.PERCENT ? value * reference / 100.0 : value;
        }
    }

    private record FunctionCall(String name, String args) { }

    private static List<String> staticSplitArgs(String raw) {
        String value = normalize(raw);
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

    private static String argStatic(List<String> args, int index) {
        return index >= 0 && index < args.size() ? args.get(index) : "";
    }
}
