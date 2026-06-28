package dev.takesome.htmldom.css.animation;

import dev.takesome.htmldom.css.UiCssKeyframe;
import dev.takesome.htmldom.css.UiCssKeyframesRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiCssAnimationTimelineCompiledCacheTest {
    @Test
    void timelineCompilesKeyframesOnceAndReusesCompiledTracks() {
        UiCssAnimationTimeline timeline = new UiCssAnimationTimeline();
        UiCssKeyframesRule rule = rule();
        UiCssAnimationDescriptor animation = animation("pulse");

        Map<String, String> first = timeline.frame(List.of(animation), Map.of("pulse", rule), 500L);
        Map<String, String> second = timeline.frame(List.of(animation), Map.of("pulse", rule), 750L);

        assertEquals("0.5", first.get("opacity"));
        assertEquals("0.75", second.get("opacity"));
        assertEquals("#808080", first.get("background-color"));
        assertTrue(first.get("transform").contains("translate(50px, 0)"));

        UiCssAnimationTimeline.Stats stats = timeline.stats();
        assertEquals(1, stats.cachedRules());
        assertEquals(1, stats.compiledSegments());
        assertEquals(3, stats.typedInterpolators());
        assertEquals(0, stats.discreteInterpolators());
        assertEquals(1, stats.cacheMisses());
        assertEquals(1, stats.cacheHits());
        assertEquals(2, stats.sampledFrames());
    }

    @Test
    void replacingKeyframesRuleRecompilesCacheForSameAnimationName() {
        UiCssAnimationTimeline timeline = new UiCssAnimationTimeline();
        UiCssAnimationDescriptor animation = animation("pulse");

        timeline.frame(List.of(animation), Map.of("pulse", rule()), 500L);
        timeline.frame(List.of(animation), Map.of("pulse", alternateRule()), 500L);

        UiCssAnimationTimeline.Stats stats = timeline.stats();
        assertEquals(1, stats.cachedRules());
        assertEquals(2, stats.cacheMisses());
        assertEquals(0, stats.cacheHits());
    }

    @Test
    void compiledKeyframesPreserveDiscreteFallbackForNonTypedValues() {
        UiCssKeyframesRule rule = new UiCssKeyframesRule("swap", List.of(
                new UiCssKeyframe(0.0, Map.of("visibility", "hidden")),
                new UiCssKeyframe(1.0, Map.of("visibility", "visible"))
        ));
        UiCssCompiledKeyframes compiled = new UiCssCompiledKeyframes(rule);

        assertEquals("hidden", compiled.sample(0.5).get("visibility"));
        assertEquals("visible", compiled.sample(1.0).get("visibility"));
        assertEquals(0, compiled.typedInterpolatorCount());
        assertEquals(1, compiled.discreteInterpolatorCount());
    }

    private UiCssKeyframesRule rule() {
        return new UiCssKeyframesRule("pulse", List.of(
                new UiCssKeyframe(0.0, Map.of(
                        "opacity", "0",
                        "background-color", "#000000",
                        "transform", "translateX(0px)"
                )),
                new UiCssKeyframe(1.0, Map.of(
                        "opacity", "1",
                        "background-color", "#ffffff",
                        "transform", "translateX(100px)"
                ))
        ));
    }

    private UiCssKeyframesRule alternateRule() {
        return new UiCssKeyframesRule("pulse", List.of(
                new UiCssKeyframe(0.0, Map.of("opacity", "1")),
                new UiCssKeyframe(1.0, Map.of("opacity", "0"))
        ));
    }

    private UiCssAnimationDescriptor animation(String name) {
        return new UiCssAnimationDescriptor(name, 1_000L, 0L, "linear", -1.0, "normal", "none", "running");
    }
}
