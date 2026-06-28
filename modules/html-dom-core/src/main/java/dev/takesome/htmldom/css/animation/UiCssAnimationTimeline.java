package dev.takesome.htmldom.css.animation;

import dev.takesome.htmldom.css.UiCssKeyframesRule;
import dev.takesome.htmldom.css.transition.UiCssTransitionTiming;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiCssAnimationTimeline {
    private final Map<String, CachedKeyframes> compiledKeyframes = new HashMap<>();
    private long cacheHits;
    private long cacheMisses;
    private long sampledFrames;

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
            UiCssCompiledKeyframes compiled = compiled(animation.name(), rule);
            Map<String, String> sample = compiled.sample(eased);
            if (!sample.isEmpty()) out.putAll(sample);
            sampledFrames++;
        }
        return out;
    }

    public Stats stats() {
        int typed = 0;
        int discrete = 0;
        int segments = 0;
        for (CachedKeyframes cached : compiledKeyframes.values()) {
            typed += cached.compiled.typedInterpolatorCount();
            discrete += cached.compiled.discreteInterpolatorCount();
            segments += cached.compiled.segmentCount();
        }
        return new Stats(compiledKeyframes.size(), segments, typed, discrete, cacheHits, cacheMisses, sampledFrames);
    }

    public void clearCache() {
        compiledKeyframes.clear();
        cacheHits = 0L;
        cacheMisses = 0L;
        sampledFrames = 0L;
    }

    private UiCssCompiledKeyframes compiled(String animationName, UiCssKeyframesRule rule) {
        String key = animationName == null ? "" : animationName;
        CachedKeyframes cached = compiledKeyframes.get(key);
        if (cached != null && cached.rule == rule) {
            cacheHits++;
            return cached.compiled;
        }
        UiCssCompiledKeyframes compiled = new UiCssCompiledKeyframes(rule);
        compiledKeyframes.put(key, new CachedKeyframes(rule, compiled));
        cacheMisses++;
        return compiled;
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

    public record Stats(int cachedRules, int compiledSegments, int typedInterpolators, int discreteInterpolators, long cacheHits, long cacheMisses, long sampledFrames) { }
    private record CachedKeyframes(UiCssKeyframesRule rule, UiCssCompiledKeyframes compiled) { }
    private record AnimationProgress(double fraction, long cycle) { }
}
