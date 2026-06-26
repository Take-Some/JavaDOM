package dev.takesome.htmldom.css.transition;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CSS transition timing functions, including cubic-bezier() and steps(). */
public final class UiCssTransitionTiming {
    private static final Pattern CUBIC = Pattern.compile("^cubic-bezier\\(([^,]+),([^,]+),([^,]+),([^,]+)\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STEPS = Pattern.compile("^steps\\((\\d+)(?:\\s*,\\s*([^)]+))?\\)$", Pattern.CASE_INSENSITIVE);

    private UiCssTransitionTiming() {
    }

    public static double apply(String timingFunction, double rawProgress) {
        double x = clamp(rawProgress);
        String timing = timingFunction == null || timingFunction.isBlank() ? "ease" : timingFunction.trim().toLowerCase(Locale.ROOT);
        if ("linear".equals(timing)) return x;
        if ("ease".equals(timing)) return cubic(0.25, 0.10, 0.25, 1.00, x);
        if ("ease-in".equals(timing)) return cubic(0.42, 0.00, 1.00, 1.00, x);
        if ("ease-out".equals(timing)) return cubic(0.00, 0.00, 0.58, 1.00, x);
        if ("ease-in-out".equals(timing)) return cubic(0.42, 0.00, 0.58, 1.00, x);
        if ("step-start".equals(timing)) return x <= 0.0 ? 0.0 : 1.0;
        if ("step-end".equals(timing)) return x >= 1.0 ? 1.0 : 0.0;

        Matcher cubic = CUBIC.matcher(timing);
        if (cubic.matches()) {
            try {
                return cubic(
                        Double.parseDouble(cubic.group(1).trim()),
                        Double.parseDouble(cubic.group(2).trim()),
                        Double.parseDouble(cubic.group(3).trim()),
                        Double.parseDouble(cubic.group(4).trim()),
                        x
                );
            } catch (RuntimeException ignored) {
                return x;
            }
        }

        Matcher steps = STEPS.matcher(timing);
        if (steps.matches()) {
            try {
                int count = Math.max(1, Integer.parseInt(steps.group(1)));
                String mode = steps.group(2) == null ? "end" : steps.group(2).trim().toLowerCase(Locale.ROOT);
                if (mode.contains("start")) return Math.min(1.0, Math.ceil(x * count) / count);
                return Math.min(1.0, Math.floor(x * count) / count);
            } catch (RuntimeException ignored) {
                return x;
            }
        }
        return x;
    }

    private static double cubic(double x1, double y1, double x2, double y2, double x) {
        double t = solveT(x1, x2, x);
        double inv = 1.0 - t;
        return clamp(3.0 * inv * inv * t * y1 + 3.0 * inv * t * t * y2 + t * t * t);
    }

    private static double solveT(double x1, double x2, double x) {
        double low = 0.0;
        double high = 1.0;
        double t = x;
        for (int i = 0; i < 18; i++) {
            double value = sampleCurve(x1, x2, t);
            if (Math.abs(value - x) < 0.00001) return t;
            if (value < x) low = t;
            else high = t;
            t = (low + high) * 0.5;
        }
        return t;
    }

    private static double sampleCurve(double x1, double x2, double t) {
        double inv = 1.0 - t;
        return 3.0 * inv * inv * t * x1 + 3.0 * inv * t * t * x2 + t * t * t;
    }

    private static double clamp(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
