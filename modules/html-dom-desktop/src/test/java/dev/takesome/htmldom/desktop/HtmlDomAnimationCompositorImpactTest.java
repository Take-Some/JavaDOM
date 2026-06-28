package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssStyleImpact;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.Timer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomAnimationCompositorImpactTest {
    @Test
    void opacityAnimationIsCompositorOnlyAndDoesNotTriggerLayoutOrCascade() throws Exception {
        HtmlDomSwingPanel panel = panel("""
                #box { width: 80px; height: 40px; animation-name: fade; animation-duration: 2000ms; animation-timing-function: linear; animation-iteration-count: infinite; }
                @keyframes fade {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                """);

        stopAnimationTimer(panel);
        panel.ensureLayout();
        stopAnimationTimer(panel);
        clearAnimatedOverlay(panel);
        resetRuntimeThrottle(panel);
        Pass pass = runtimePass(panel);

        assertEquals(UiCssStyleImpact.COMPOSITE, pass.impact());
        assertEquals(UiCssStyleImpact.RuntimeCategory.COMPOSITOR_ONLY, pass.impact().category());
        assertFalse(pass.cascadeUpdated());
        assertFalse(pass.layoutUpdated());
        assertTrue(pass.needsRepaint());
        assertFalse(pass.requiresFullRepaint());
    }

    @Test
    void transformAnimationIsCompositorOnlyAndDoesNotTriggerLayoutOrCascade() throws Exception {
        HtmlDomSwingPanel panel = panel("""
                #box { width: 80px; height: 40px; animation-name: move; animation-duration: 2000ms; animation-timing-function: linear; animation-iteration-count: infinite; }
                @keyframes move {
                    from { transform: translateX(0px); }
                    to { transform: translateX(120px); }
                }
                """);

        stopAnimationTimer(panel);
        panel.ensureLayout();
        stopAnimationTimer(panel);
        clearAnimatedOverlay(panel);
        resetRuntimeThrottle(panel);
        Pass pass = runtimePass(panel);

        assertEquals(UiCssStyleImpact.COMPOSITE, pass.impact());
        assertEquals(UiCssStyleImpact.RuntimeCategory.COMPOSITOR_ONLY, pass.impact().category());
        assertFalse(pass.cascadeUpdated());
        assertFalse(pass.layoutUpdated());
        assertTrue(pass.needsRepaint());
        assertFalse(pass.requiresFullRepaint());
    }

    @Test
    void colorAnimationIsPaintOnlyAndDoesNotTriggerLayout() throws Exception {
        HtmlDomSwingPanel panel = panel("""
                #box { width: 80px; height: 40px; animation-name: tint; animation-duration: 2000ms; animation-timing-function: linear; animation-iteration-count: infinite; }
                @keyframes tint {
                    from { background-color: #000000; }
                    to { background-color: #ffffff; }
                }
                """);

        stopAnimationTimer(panel);
        panel.ensureLayout();
        stopAnimationTimer(panel);
        clearAnimatedOverlay(panel);
        resetRuntimeThrottle(panel);
        Pass pass = runtimePass(panel);

        assertEquals(UiCssStyleImpact.PAINT, pass.impact());
        assertEquals(UiCssStyleImpact.RuntimeCategory.PAINT_ONLY, pass.impact().category());
        assertFalse(pass.cascadeUpdated());
        assertFalse(pass.layoutUpdated());
        assertTrue(pass.needsRepaint());
        assertFalse(pass.requiresFullRepaint());
    }

    @Test
    void widthAnimationIsLayoutAffectingAndTriggersLayout() throws Exception {
        HtmlDomSwingPanel panel = panel("""
                #box { width: 40px; height: 40px; animation-name: grow; animation-duration: 2000ms; animation-timing-function: linear; animation-iteration-count: infinite; }
                @keyframes grow {
                    from { width: 40px; }
                    to { width: 160px; }
                }
                """);

        stopAnimationTimer(panel);
        panel.ensureLayout();
        stopAnimationTimer(panel);
        clearAnimatedOverlay(panel);
        resetRuntimeThrottle(panel);
        Pass pass = runtimePass(panel);

        assertEquals(UiCssStyleImpact.LAYOUT, pass.impact());
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, pass.impact().category());
        assertFalse(pass.cascadeUpdated());
        assertTrue(pass.layoutUpdated());
        assertTrue(pass.needsRepaint());
        assertTrue(pass.requiresFullRepaint());
    }

    @Test
    void runtimePropertyCategoriesAreExplicit() {
        assertEquals(UiCssStyleImpact.RuntimeCategory.COMPOSITOR_ONLY, UiCssStyleImpact.categoryOf("opacity"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.COMPOSITOR_ONLY, UiCssStyleImpact.categoryOf("transform"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.PAINT_ONLY, UiCssStyleImpact.categoryOf("color"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.PAINT_ONLY, UiCssStyleImpact.categoryOf("background-color"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.PAINT_ONLY, UiCssStyleImpact.categoryOf("border-color"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.PAINT_ONLY, UiCssStyleImpact.categoryOf("box-shadow"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("width"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("height"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("margin"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("padding"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("top"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("left"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("right"));
        assertEquals(UiCssStyleImpact.RuntimeCategory.LAYOUT_AFFECTING, UiCssStyleImpact.categoryOf("bottom"));
    }

    private HtmlDomSwingPanel panel(String css) {
        HtmlDomSwingPanel panel = new HtmlDomSwingPanel("""
                <html><body><div id=\"box\">Box</div></body></html>
                """, css, "animation-compositor-impact-test.html", "",
                HtmlDomConfig.defaults()
                        .withAllowDevTools(HtmlDomConfig.DevToolsAvailability.DISABLED)
                        .withAnimationFrameIntervalMs(250));
        panel.setSize(400, 240);
        return panel;
    }

    private void stopAnimationTimer(HtmlDomSwingPanel panel) throws Exception {
        Field field = HtmlDomSwingPanel.class.getDeclaredField("transitionTimer");
        field.setAccessible(true);
        ((Timer) field.get(panel)).stop();
    }

    private void clearAnimatedOverlay(HtmlDomSwingPanel panel) {
        panel.document().getElementById("box").orElseThrow().clearAnimatedComputedStyle();
    }

    private void resetRuntimeThrottle(HtmlDomSwingPanel panel) throws Exception {
        Field field = HtmlDomSwingPanel.class.getDeclaredField("lastRuntimeEffectsMs");
        field.setAccessible(true);
        field.setLong(panel, 0L);
    }

    private Pass runtimePass(HtmlDomSwingPanel panel) throws Exception {
        Method method = null;
        for (Method candidate : HtmlDomSwingPanel.class.getDeclaredMethods()) {
            if (candidate.getName().equals("updateStyleLayoutAndEffects") && candidate.getParameterCount() == 3) {
                method = candidate;
                break;
            }
        }
        if (method == null) throw new IllegalStateException("updateStyleLayoutAndEffects method not found");
        method.setAccessible(true);
        Object result = method.invoke(panel, "animation-frame", null, true);
        UiDomElement element = panel.document().getElementById("box").orElseThrow();
        assertFalse(element.animatedComputedStyle().isEmpty(), "runtime pass should apply an animated overlay");
        return new Pass(
                (UiCssStyleImpact) invoke(result, "impact"),
                (boolean) invoke(result, "cascadeUpdated"),
                (boolean) invoke(result, "layoutUpdated"),
                (boolean) invoke(result, "needsRepaint"),
                (boolean) invoke(result, "requiresFullRepaint")
        );
    }

    private Object invoke(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private record Pass(UiCssStyleImpact impact, boolean cascadeUpdated, boolean layoutUpdated, boolean needsRepaint, boolean requiresFullRepaint) { }
}
