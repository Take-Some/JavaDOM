package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssKeyframe;
import dev.takesome.htmldom.css.UiCssKeyframesRule;
import dev.takesome.htmldom.css.animation.UiCssAnimationDescriptor;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomAnimationFrameExecutorTest {
    @Test
    void computesAnimationFramesAcrossWorkerPoolWithoutMutatingDom() {
        UiDomDocument document = UiDomDocument.parse(markup(12));
        UiCssAnimationDescriptor descriptor = new UiCssAnimationDescriptor(
                "fade",
                1_000L,
                0L,
                "linear",
                -1.0,
                "normal",
                "none",
                "running"
        );
        ArrayList<HtmlDomAnimationFrameExecutor.FrameRequest> requests = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            UiDomElement element = document.getElementById("node-" + i).orElseThrow();
            requests.add(new HtmlDomAnimationFrameExecutor.FrameRequest(element, List.of(descriptor)));
        }
        HtmlDomAnimationFrameExecutor executor = new HtmlDomAnimationFrameExecutor(4);

        List<HtmlDomAnimationFrameExecutor.FrameResult> frames = executor.computeFrames(requests, keyframes(), 500L);

        assertTrue(executor.parallelEnabled());
        assertEquals(4, executor.maxWorkerThreads());
        assertEquals(4, executor.lastRequestedWorkers());
        assertTrue(executor.liveWorkerThreads() <= 4);
        HtmlDomAnimationFrameExecutor.Stats parallelStats = executor.stats();
        assertEquals(12, parallelStats.animationTargets());
        assertEquals(4, parallelStats.requestedWorkers());
        assertEquals(12, parallelStats.parallelFrames());
        assertEquals(0, parallelStats.workerFallbacks());
        assertEquals(12, frames.size());
        for (HtmlDomAnimationFrameExecutor.FrameResult result : frames) {
            assertTrue(result.frame().containsKey("opacity"));
            assertTrue(result.element().animatedComputedStyle().isEmpty(), "workers must compute frame maps only; DOM overlays stay on EDT");
        }

        List<HtmlDomAnimationFrameExecutor.FrameResult> single = executor.computeFrames(List.of(requests.get(0)), keyframes(), 500L);

        assertEquals(1, single.size());
        assertEquals(1, executor.lastRequestedWorkers(), "single target animation should stay serial instead of reserving the full worker limit");
        HtmlDomAnimationFrameExecutor.Stats serialStats = executor.stats();
        assertEquals(1, serialStats.animationTargets());
        assertEquals(1, serialStats.requestedWorkers());
        assertEquals(1, serialStats.serialFrames());

        executor.close();
        assertFalse(executor.parallelEnabled());
    }

    @Test
    @SuppressWarnings("deprecation")
    void configExposesAnimationWorkerLimitTuningAndLegacyBridge() {
        HtmlDomConfig config = HtmlDomConfig.defaults().withAnimationWorkerLimit(8);

        assertEquals(8, config.animationWorkerLimit());
        int fallbackWorkers = config.withAnimationWorkerLimit(-1).animationWorkerLimit();
        assertTrue(fallbackWorkers >= 1 && fallbackWorkers <= 32);

        HtmlDomConfig legacy = HtmlDomConfig.defaults().withAnimationWorkerThreads(3);
        assertEquals(3, legacy.animationWorkerThreads());
        assertEquals(3, legacy.animationWorkerLimit());
    }

    private Map<String, UiCssKeyframesRule> keyframes() {
        UiCssKeyframesRule rule = new UiCssKeyframesRule("fade", List.of(
                new UiCssKeyframe(0.0, Map.of("opacity", "0")),
                new UiCssKeyframe(1.0, Map.of("opacity", "1"))
        ));
        return Map.of("fade", rule);
    }

    private String markup(int count) {
        StringBuilder out = new StringBuilder("<html><body>");
        for (int i = 0; i < count; i++) out.append("<div id='node-").append(i).append("'></div>");
        return out.append("</body></html>").toString();
    }
}
