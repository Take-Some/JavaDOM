package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.bundled.HtmlOnlyAnimationDemo;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlOnlyAnimationDemoTest {
    @Test
    void htmlOnlyDemoParsesInlineStylesAndAnimationTargets() throws ReflectiveOperationException {
        HtmlDomSwingPanel panel = panel();

        panel.ensureLayout();

        assertFalse(animationTargets(panel).isEmpty());
        assertTrue(keyframeCount(panel) >= 4);
    }

    @Test
    void htmlOnlyDemoControlsDispatchToLuaDomBindings() {
        HtmlDomSwingPanel panel = panel();

        panel.activate(element(panel, "btn-ignite"));

        assertEquals("HOT", element(panel, "mode-label").textContent());
        assertEquals("1", element(panel, "event-count").textContent());
        assertEquals("Lua ignition sequence armed", element(panel, "primary-readout").textContent());
        assertTrue(element(panel, "core").classList().contains("hot"));
        assertTrue(element(panel, "stage").classList().contains("boosted"));
    }

    @Test
    void htmlOnlyDemoTabsStillUseNativeHtmlDomTabActionAndLuaObserver() {
        HtmlDomSwingPanel panel = panel();

        panel.activate(element(panel, "tab-lua"));

        assertTrue(element(panel, "tab-lua").classList().contains("active"));
        assertFalse(element(panel, "tab-diagnostics").classList().contains("active"));
        assertEquals("1", element(panel, "event-count").textContent());
        assertEquals("Lua observed tab button #tab-lua", element(panel, "lua-status").textContent());
    }

    private HtmlDomSwingPanel panel() {
        HtmlDomSwingPanel panel = new HtmlDomSwingPanel(
                HtmlOnlyAnimationDemo.html(),
                "",
                "html-only-animation-demo-test.html",
                "",
                HtmlDomConfig.defaults().withAllowDevTools(HtmlDomConfig.DevToolsAvailability.DISABLED)
        );
        panel.setSize(1180, 760);
        panel.executeLua(HtmlOnlyAnimationDemo.lua(), "html-only-animation-demo-test.lua");
        return panel;
    }

    private UiDomElement element(HtmlDomSwingPanel panel, String id) {
        return panel.document().getElementById(id).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, ?> animationTargets(HtmlDomSwingPanel panel) throws ReflectiveOperationException {
        Field field = HtmlDomSwingPanel.class.getDeclaredField("animationTargets");
        field.setAccessible(true);
        return (Map<Integer, ?>) field.get(panel);
    }

    private int keyframeCount(HtmlDomSwingPanel panel) throws ReflectiveOperationException {
        Field field = HtmlDomSwingPanel.class.getDeclaredField("stylesheet");
        field.setAccessible(true);
        Object stylesheet = field.get(panel);
        Method keyframes = stylesheet.getClass().getMethod("keyframes");
        Object value = keyframes.invoke(stylesheet);
        return value instanceof Map<?, ?> map ? map.size() : 0;
    }
}
