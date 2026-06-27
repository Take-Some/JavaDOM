package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssStyleImpact;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;
import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Minimal CSS transition runtime for paint-only properties. */
public final class HtmlDomTransitionController {
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(HtmlDomTransitionController.class);
    private static final List<String> SUPPORTED = List.of("transform", "opacity", "color", "border-color", "background-color");
    private final Map<Integer, ElementState> states = new HashMap<>();
    private boolean active;
    private final java.util.ArrayList<TransitionEndEvent> finishedEvents = new java.util.ArrayList<>();

    public boolean active() {
        return active;
    }

    public List<TransitionEndEvent> drainFinishedEvents() {
        if (finishedEvents.isEmpty()) return List.of();
        List<TransitionEndEvent> out = List.copyOf(finishedEvents);
        finishedEvents.clear();
        return out;
    }

    public TickResult apply(UiDomDocument document) {
        finishedEvents.clear();
        active = false;
        if (document == null) return new TickResult(false, UiCssStyleImpact.NONE);
        long now = System.currentTimeMillis();
        UiCssStyleImpact impact = UiCssStyleImpact.NONE;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            impact = impact.merge(apply(element, now));
        }
        return new TickResult(active, impact);
    }

    private UiCssStyleImpact apply(UiDomElement element, long now) {
        UiCssStyleImpact impact = UiCssStyleImpact.NONE;
        ElementState state = states.computeIfAbsent(element.nodeId(), ignored -> new ElementState());
        if (!state.initialized) {
            for (String property : SUPPORTED) {
                String target = targetValue(element, property);
                state.targets.put(property, target);
                state.effective.put(property, target);
                element.removeAnimatedComputedStyle(property);
            }
            state.initialized = true;
            return UiCssStyleImpact.NONE;
        }
        for (String property : SUPPORTED) {
            String target = targetValue(element, property);
            String previousTarget = state.targets.getOrDefault(property, defaultValue(property));
            if (!same(previousTarget, target)) {
                TransitionSpec spec = transitionSpec(element, property);
                UiCssStyleImpact propertyImpact = UiCssStyleImpact.of(property);
                String start = state.effective.getOrDefault(property, previousTarget);
                state.targets.put(property, target);
                if (LOG.debugEnabled()) {
                    LOG.debug("HtmlDom transition property='{}' impact={} element='{}' durationMs={} delayMs={}", property, propertyImpact, elementDescription(element), spec.durationMs, spec.delayMs);
                }
                if (spec.durationMs > 0L) {
                    state.animations.put(property, new Animation(property, start, target, now + spec.delayMs, spec.durationMs, spec.timing));
                    impact = impact.merge(propertyImpact);
                } else {
                    state.animations.remove(property);
                    state.effective.put(property, target);
                    if (element.removeAnimatedComputedStyle(property)) impact = impact.merge(propertyImpact);
                }
            }
            Animation animation = state.animations.get(property);
            if (animation != null) {
                UiCssStyleImpact propertyImpact = UiCssStyleImpact.of(property);
                String value = animation.value(now);
                if (element.setAnimatedComputedStyle(property, value)) impact = impact.merge(propertyImpact);
                state.effective.put(property, value);
                if (animation.done(now)) {
                    if (element.removeAnimatedComputedStyle(property)) impact = impact.merge(propertyImpact);
                    state.effective.put(property, animation.end);
                    state.animations.remove(property);
                    finishedEvents.add(new TransitionEndEvent(element, property, animation.durationMs));
                } else {
                    active = true;
                }
            } else {
                state.effective.put(property, target);
            }
        }
        return impact;
    }

    private String targetValue(UiDomElement element, String property) {
        String raw = element.baseComputedStyle().getOrDefault(property, "").trim();
        if (raw.isBlank()) return defaultValue(property);
        return raw;
    }

    private String defaultValue(String property) {
        return switch (property) {
            case "opacity" -> "1";
            case "transform" -> "none";
            case "color" -> "#000000";
            case "border-color", "background-color" -> "rgba(0,0,0,0)";
            default -> "";
        };
    }

    private boolean colorProperty(String property) {
        return "color".equals(property) || "border-color".equals(property) || "background-color".equals(property);
    }

    private boolean same(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private TransitionSpec transitionSpec(UiDomElement element, String property) {
        TransitionSpec shorthand = transitionFromShorthand(element.style("transition", ""), property);
        if (shorthand.durationMs > 0L) return shorthand;
        long duration = timeFor(property, element.style("transition-property", ""), element.style("transition-duration", ""), 0L);
        long delay = timeFor(property, element.style("transition-property", ""), element.style("transition-delay", ""), 0L);
        String timing = timingFor(property, element.style("transition-property", ""), element.style("transition-timing-function", ""));
        return new TransitionSpec(duration, delay, timing);
    }

    private TransitionSpec transitionFromShorthand(String raw, String property) {
        if (raw == null || raw.isBlank()) return TransitionSpec.NONE;
        TransitionSpec fallbackAll = TransitionSpec.NONE;
        for (String item : splitComma(raw)) {
            String transitionProperty = "all";
            String timing = "linear";
            long duration = -1L;
            long delay = 0L;
            for (String token : item.trim().split("\\s+")) {
                if (token.isBlank()) continue;
                Long time = parseTimeNullable(token);
                if (time != null) {
                    if (duration < 0L) duration = time;
                    else delay = time;
                } else if (isTiming(token)) {
                    timing = token.toLowerCase(Locale.ROOT);
                } else {
                    transitionProperty = token.toLowerCase(Locale.ROOT);
                }
            }
            if (duration < 0L) duration = 0L;
            TransitionSpec spec = new TransitionSpec(duration, delay, timing);
            if (transitionProperty.equals(property)) return spec;
            if ("all".equals(transitionProperty)) fallbackAll = spec;
        }
        return fallbackAll;
    }

    private long timeFor(String property, String propertyList, String valueList, long fallback) {
        List<String> properties = splitComma(propertyList == null || propertyList.isBlank() ? "all" : propertyList);
        List<String> values = splitComma(valueList);
        if (values.isEmpty()) return fallback;
        int index = propertyIndex(properties, property);
        if (index < 0) return fallback;
        String raw = values.get(Math.min(index, values.size() - 1));
        Long parsed = parseTimeNullable(raw);
        return parsed == null ? fallback : parsed;
    }

    private String timingFor(String property, String propertyList, String valueList) {
        List<String> properties = splitComma(propertyList == null || propertyList.isBlank() ? "all" : propertyList);
        List<String> values = splitComma(valueList);
        int index = propertyIndex(properties, property);
        if (index < 0 || values.isEmpty()) return "linear";
        return values.get(Math.min(index, values.size() - 1)).trim().toLowerCase(Locale.ROOT);
    }

    private int propertyIndex(List<String> properties, String property) {
        int all = -1;
        for (int i = 0; i < properties.size(); i++) {
            String value = properties.get(i).trim().toLowerCase(Locale.ROOT);
            if (value.equals(property)) return i;
            if (value.equals("all")) all = i;
        }
        return all;
    }

    private List<String> splitComma(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return java.util.Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private Long parseTimeNullable(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("ms")) return Math.max(0L, Math.round(Double.parseDouble(value.substring(0, value.length() - 2).trim())));
            if (value.endsWith("s")) return Math.max(0L, Math.round(Double.parseDouble(value.substring(0, value.length() - 1).trim()) * 1000.0));
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private boolean isTiming(String value) {
        String v = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return v.equals("linear") || v.equals("ease") || v.equals("ease-in") || v.equals("ease-out") || v.equals("ease-in-out") || v.startsWith("cubic-bezier(");
    }

    public record TickResult(boolean active, UiCssStyleImpact impact) { }

    public record TransitionEndEvent(UiDomElement element, String propertyName, long elapsedMs) { }

    private String elementDescription(UiDomElement element) {
        if (element == null) return "";
        StringBuilder out = new StringBuilder(element.tagName());
        if (!element.id().isBlank()) out.append('#').append(element.id());
        for (String className : element.classList().values()) out.append('.').append(className);
        return out.toString();
    }

    private static final class ElementState {
        boolean initialized;
        final Map<String, String> targets = new HashMap<>();
        final Map<String, String> effective = new HashMap<>();
        final Map<String, Animation> animations = new HashMap<>();
    }

    private record TransitionSpec(long durationMs, long delayMs, String timing) {
        static final TransitionSpec NONE = new TransitionSpec(0L, 0L, "linear");
    }

    private static final class Animation {
        final String property;
        final String start;
        final String end;
        final long startedAt;
        final long durationMs;
        final String timing;

        Animation(String property, String start, String end, long startedAt, long durationMs, String timing) {
            this.property = property;
            this.start = start == null ? "" : start;
            this.end = end == null ? "" : end;
            this.startedAt = startedAt;
            this.durationMs = Math.max(1L, durationMs);
            this.timing = timing == null ? "linear" : timing;
        }

        boolean done(long now) {
            return now >= startedAt + durationMs;
        }

        String value(long now) {
            if (now < startedAt) return start;
            float t = Math.max(0f, Math.min(1f, (now - startedAt) / (float) durationMs));
            t = ease(t, timing);
            if ("opacity".equals(property)) return Float.toString(lerp(number(start, 1f), number(end, 1f), t));
            if (property.endsWith("color")) return ColorValue.lerp(ColorValue.parse(start), ColorValue.parse(end), t).css();
            TransformValue a = TransformValue.parse(start);
            TransformValue b = TransformValue.parse(end);
            return TransformValue.lerp(a, b, t).css();
        }

        private float ease(float t, String timing) {
            String mode = timing.toLowerCase(Locale.ROOT);
            if (mode.equals("ease-in")) return t * t * t;
            if (mode.equals("ease-out")) return 1f - (float) Math.pow(1f - t, 3);
            if (mode.equals("ease-in-out") || mode.equals("ease")) return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            return t;
        }

        private float number(String raw, float fallback) {
            try { return raw == null || raw.isBlank() || "none".equals(raw.trim()) ? fallback : Float.parseFloat(raw.trim()); }
            catch (RuntimeException ignored) { return fallback; }
        }
    }


    private record ColorValue(float r, float g, float b, float a) {
        static ColorValue parse(String raw) {
            String v = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (v.isBlank() || "transparent".equals(v) || "none".equals(v)) return new ColorValue(0f, 0f, 0f, 0f);
            if (v.startsWith("#")) {
                try {
                    String hex = v.substring(1);
                    if (hex.length() == 3) hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
                    if (hex.length() == 6) return new ColorValue(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), 255f);
                    if (hex.length() == 8) return new ColorValue(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), Integer.parseInt(hex.substring(6, 8), 16));
                } catch (RuntimeException ignored) { return new ColorValue(0f, 0f, 0f, 255f); }
            }
            if (v.startsWith("rgb")) {
                int a0 = v.indexOf('('), a1 = v.lastIndexOf(')');
                if (a0 >= 0 && a1 > a0) {
                    String[] parts = v.substring(a0 + 1, a1).split(",");
                    try {
                        float r = channel(parts, 0, 0f);
                        float g = channel(parts, 1, 0f);
                        float b = channel(parts, 2, 0f);
                        float a = parts.length > 3 ? alpha(parts[3].trim()) : 255f;
                        return new ColorValue(r, g, b, a);
                    } catch (RuntimeException ignored) { return new ColorValue(0f, 0f, 0f, 255f); }
                }
            }
            return switch (v) {
                case "white" -> new ColorValue(255f, 255f, 255f, 255f);
                case "black" -> new ColorValue(0f, 0f, 0f, 255f);
                case "red" -> new ColorValue(255f, 0f, 0f, 255f);
                case "green" -> new ColorValue(0f, 128f, 0f, 255f);
                case "blue" -> new ColorValue(0f, 0f, 255f, 255f);
                default -> new ColorValue(0f, 0f, 0f, 255f);
            };
        }

        static ColorValue lerp(ColorValue a, ColorValue b, float t) {
            return new ColorValue(HtmlDomTransitionController.lerp(a.r, b.r, t), HtmlDomTransitionController.lerp(a.g, b.g, t), HtmlDomTransitionController.lerp(a.b, b.b, t), HtmlDomTransitionController.lerp(a.a, b.a, t));
        }

        String css() {
            return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.4f)", clamp(r), clamp(g), clamp(b), Math.max(0f, Math.min(1f, a / 255f)));
        }

        private static float channel(String[] parts, int index, float fallback) {
            if (index >= parts.length) return fallback;
            String part = parts[index].trim();
            if (part.endsWith("%")) return Float.parseFloat(part.substring(0, part.length() - 1).trim()) * 2.55f;
            return Float.parseFloat(part);
        }

        private static float alpha(String raw) {
            if (raw.endsWith("%")) return Float.parseFloat(raw.substring(0, raw.length() - 1).trim()) * 2.55f;
            float value = Float.parseFloat(raw);
            return value <= 1f ? value * 255f : value;
        }

        private static int clamp(float value) {
            return Math.max(0, Math.min(255, Math.round(value)));
        }
    }

    private record TransformValue(float tx, float ty, float sx, float sy, float rotateDeg) {
        static TransformValue parse(String raw) {
            String v = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (v.isBlank() || "none".equals(v)) return new TransformValue(0f, 0f, 1f, 1f, 0f);
            TransformValue out = new TransformValue(0f, 0f, 1f, 1f, 0f);
            int i = 0;
            while (i < v.length()) {
                while (i < v.length() && Character.isWhitespace(v.charAt(i))) i++;
                int nameStart = i;
                while (i < v.length() && (Character.isLetter(v.charAt(i)) || v.charAt(i) == '-')) i++;
                if (i <= nameStart || i >= v.length() || v.charAt(i) != '(') break;
                String name = v.substring(nameStart, i);
                int open = i++;
                int depth = 1;
                while (i < v.length() && depth > 0) {
                    char ch = v.charAt(i++);
                    if (ch == '(') depth++;
                    else if (ch == ')') depth--;
                }
                if (depth == 0) out = out.with(name, v.substring(open + 1, i - 1));
            }
            return out;
        }

        private TransformValue with(String name, String argsRaw) {
            List<String> args = java.util.Arrays.stream(argsRaw.split("[,\\s]+"))
                    .map(String::trim).filter(s -> !s.isBlank()).toList();
            return switch (name) {
                case "translate" -> new TransformValue(length(arg(args, 0), tx), length(arg(args, 1), ty), sx, sy, rotateDeg);
                case "translatex" -> new TransformValue(length(arg(args, 0), tx), ty, sx, sy, rotateDeg);
                case "translatey" -> new TransformValue(tx, length(arg(args, 0), ty), sx, sy, rotateDeg);
                case "scale" -> {
                    float x = number(arg(args, 0), sx);
                    yield new TransformValue(tx, ty, x, args.size() > 1 ? number(arg(args, 1), sy) : x, rotateDeg);
                }
                case "scalex" -> new TransformValue(tx, ty, number(arg(args, 0), sx), sy, rotateDeg);
                case "scaley" -> new TransformValue(tx, ty, sx, number(arg(args, 0), sy), rotateDeg);
                case "rotate" -> new TransformValue(tx, ty, sx, sy, angle(arg(args, 0), rotateDeg));
                default -> this;
            };
        }

        static TransformValue lerp(TransformValue a, TransformValue b, float t) {
            return new TransformValue(lerp(a.tx, b.tx, t), lerp(a.ty, b.ty, t), lerp(a.sx, b.sx, t), lerp(a.sy, b.sy, t), lerp(a.rotateDeg, b.rotateDeg, t));
        }

        String css() {
            return String.format(Locale.ROOT, "translate(%.3fpx, %.3fpx) rotate(%.3fdeg) scale(%.5f, %.5f)", tx, ty, rotateDeg, sx, sy);
        }

        private String arg(List<String> args, int index) { return index >= 0 && index < args.size() ? args.get(index) : ""; }
        private float length(String raw, float fallback) { return number(clean(raw, "px", "%"), fallback); }
        private float angle(String raw, float fallback) { return raw != null && raw.trim().endsWith("turn") ? number(clean(raw, "turn"), fallback) * 360f : raw != null && raw.trim().endsWith("rad") ? (float) Math.toDegrees(number(clean(raw, "rad"), fallback)) : number(clean(raw, "deg"), fallback); }
        private float number(String raw, float fallback) { try { return raw == null || raw.isBlank() ? fallback : Float.parseFloat(raw.trim()); } catch (RuntimeException ignored) { return fallback; } }
        private String clean(String raw, String... suffixes) { String v = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT); for (String suffix : suffixes) if (v.endsWith(suffix)) return v.substring(0, v.length() - suffix.length()).trim(); return v; }
        private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
