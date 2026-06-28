package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomTransitionFadeRuntimeTest {
    @Test
    void opacityTransitionUsesCssEaseByDefaultAndFinishesCleanly() {
        UiDomDocument document = UiDomDocument.parse("<html><body><div id='fade'></div></body></html>");
        UiDomElement element = document.getElementById("fade").orElseThrow();
        element.setInternalComputedStyle("opacity", "0");
        element.setInternalComputedStyle("transition", "opacity 200ms");
        HtmlDomTransitionController controller = new HtmlDomTransitionController();

        controller.apply(document, 1_000L, true);
        element.setInternalComputedStyle("opacity", "1");
        HtmlDomTransitionController.TickResult start = controller.apply(document, 1_000L, true);
        HtmlDomTransitionController.TickResult quarter = controller.apply(document, 1_050L, false);

        assertTrue(start.changedElements().contains(element));
        assertTrue(quarter.changedElements().contains(element));

        float easedQuarter = Float.parseFloat(element.style("opacity"));
        assertTrue(easedQuarter > 0.25f, "default transition timing should be CSS ease, not linear");
        assertTrue(easedQuarter < 0.75f, "fade should remain gradual early in the transition");

        controller.apply(document, 1_200L, false);

        assertEquals("1", element.style("opacity"));
        assertTrue(element.animatedComputedStyle().isEmpty(), "finished transition should leave no animated overlay");
    }

    @Test
    void opacityTransitionAcceptsCubicBezierWithCommasAndSpaces() {
        UiDomDocument document = UiDomDocument.parse("<html><body><div id='fade'></div></body></html>");
        UiDomElement element = document.getElementById("fade").orElseThrow();
        element.setInternalComputedStyle("opacity", "0");
        element.setInternalComputedStyle("transition", "opacity 200ms cubic-bezier(0.4, 0.0, 0.2, 1)");
        HtmlDomTransitionController controller = new HtmlDomTransitionController();

        controller.apply(document, 2_000L, true);
        element.setInternalComputedStyle("opacity", "1");
        controller.apply(document, 2_000L, true);
        HtmlDomTransitionController.TickResult middleResult = controller.apply(document, 2_100L, false);

        assertTrue(middleResult.changedElements().contains(element));

        float middle = Float.parseFloat(element.style("opacity"));
        assertTrue(middle > 0.5f, "custom cubic-bezier should be parsed as one timing function, not split as broken transition tokens");
        assertTrue(middle < 1f);
    }
    @Test
    void transitionControllerKeepsOnlyActiveStatesOnCompositorTicks() throws Exception {
        StringBuilder markup = new StringBuilder("<html><body>");
        for (int i = 0; i < 120; i++) markup.append("<div id='node-").append(i).append("'></div>");
        markup.append("</body></html>");
        UiDomDocument document = UiDomDocument.parse(markup.toString());
        UiDomElement animated = document.getElementById("node-77").orElseThrow();
        animated.setInternalComputedStyle("opacity", "0");
        animated.setInternalComputedStyle("transition", "opacity 300ms ease-out");
        HtmlDomTransitionController controller = new HtmlDomTransitionController();

        controller.apply(document, 3_000L, true);
        assertEquals(0, activeStateCount(controller));

        animated.setInternalComputedStyle("opacity", "1");
        HtmlDomTransitionController.TickResult start = controller.apply(document, 3_000L, true);

        assertTrue(start.changedElements().contains(animated));
        assertEquals(1, activeStateCount(controller), "only the element with a running transition should stay in the active compositor set");

        HtmlDomTransitionController.TickResult middle = controller.apply(document, 3_150L, false);

        assertTrue(middle.changedElements().contains(animated));
        assertEquals(1, activeStateCount(controller));

        controller.apply(document, 3_300L, false);

        assertEquals(0, activeStateCount(controller), "finished transitions must leave the active compositor set");
    }

    private int activeStateCount(HtmlDomTransitionController controller) throws Exception {
        Field field = HtmlDomTransitionController.class.getDeclaredField("activeStates");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(controller)).size();
    }

}
