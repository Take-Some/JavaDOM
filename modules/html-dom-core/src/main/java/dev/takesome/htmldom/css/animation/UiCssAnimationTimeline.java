package dev.takesome.htmldom.css.animation;

import dev.takesome.htmldom.css.UiCssKeyframesRule;
import dev.takesome.htmldom.css.transition.UiCssTransitionTiming;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiCssAnimationTimeline {
    public Map<String, String> frame(List<UiCssAnimationDescriptor> animations, Map<String, UiCssKeyframesRule> keyframes, long nowMs) {
        if (animations == null || animations.isEmpty() || keyframes == null || keyframes.isEmpty()) return Map.of();
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (UiCssAnimationDescriptor animation : animations) {
            if (animation == null || !animation.active()) continue;
            UiCssKeyframesRule rule = keyframes.get(animation.name());
            if (rule == null || rule.empty()) continue;
            AnimationProgress progress = progress(animation, nowMs);
            if (progress == null) continue;
            double eased = UiCssTransitionTiming.apply(animation.timingFunction(), directed(animation, progress));
            out.putAll(rule.sample(eased));
        }
        return out;
    }

    private AnimationProgress progress(UiCssAnimationDescriptor a, long nowMs) {
        if ("paused".equalsIgnoreCase(a.playState())) nowMs = Math.max(0L, nowMs);
        long local = nowMs - a.delayMs();
        if (local < 0L) {
            return fillsBackwards(a) ? new AnimationProgress(0.0, 0L) : null;
        }
        if (a.durationMs() <= 0L) return fillsForwards(a) ? new AnimationProgress(1.0, 0L) : null;

        double rawCycle = local / (double) a.durationMs();
        long cycle = Math.max(0L, (long) Math.floor(rawCycle));
        if (!a.infinite() && rawCycle >= a.iterationCount()) {
            long finalCycle = Math.max(0L, (long) Math.ceil(a.iterationCount()) - 1L);
            return fillsForwards(a) ? new AnimationProgress(1.0, finalCycle) : null;
        }
        return new AnimationProgress(rawCycle - Math.floor(rawCycle), cycle);
    }

    private double directed(UiCssAnimationDescriptor a, AnimationProgress progress) {
        String direction = a.direction();
        if ("reverse".equals(direction)) return 1.0 - progress.fraction();
        if ("alternate".equals(direction) || "alternate-reverse".equals(direction)) {
            boolean reverse = (progress.cycle() % 2L) == 1L;
            if ("alternate-reverse".equals(direction)) reverse = !reverse;
            return reverse ? 1.0 - progress.fraction() : progress.fraction();
        }
        return progress.fraction();
    }

    private boolean fillsBackwards(UiCssAnimationDescriptor a) { return "backwards".equals(a.fillMode()) || "both".equals(a.fillMode()); }
    private boolean fillsForwards(UiCssAnimationDescriptor a) { return "forwards".equals(a.fillMode()) || "both".equals(a.fillMode()); }

    private record AnimationProgress(double fraction, long cycle) { }
}
