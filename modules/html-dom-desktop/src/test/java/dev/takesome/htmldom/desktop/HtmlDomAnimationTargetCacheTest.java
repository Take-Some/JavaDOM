package dev.takesome.htmldom.desktop;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HtmlDomAnimationTargetCacheTest {
    @Test
    void keyframesWithoutActiveAnimationDoNotPopulateTargetCache() throws ReflectiveOperationException {
        HtmlDomSwingPanel panel = panel("""
                <html><body>
                    <div id=\"alpha\">Alpha</div>
                    <div id=\"beta\">Beta</div>
                </body></html>
                """, """
                @keyframes fade {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                """);

        panel.ensureLayout();

        assertEquals(0, animationTargets(panel).size());
    }

    @Test
    void activeAnimationPopulatesTargetCacheOnlyForAnimatedElements() throws ReflectiveOperationException {
        HtmlDomSwingPanel panel = panel("""
                <html><body>
                    <div id=\"alpha\">Alpha</div>
                    <div id=\"beta\">Beta</div>
                </body></html>
                """, """
                #alpha { animation-name: fade; animation-duration: 10s; }
                @keyframes fade {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                """);

        panel.ensureLayout();

        assertEquals(1, animationTargets(panel).size());
    }

    private HtmlDomSwingPanel panel(String markup, String css) {
        HtmlDomSwingPanel panel = new HtmlDomSwingPanel(markup, css, "animation-target-cache-test.html", "",
                HtmlDomConfig.defaults().withAllowDevTools(HtmlDomConfig.DevToolsAvailability.DISABLED));
        panel.setSize(320, 200);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, ?> animationTargets(HtmlDomSwingPanel panel) throws ReflectiveOperationException {
        Field field = HtmlDomSwingPanel.class.getDeclaredField("animationTargets");
        field.setAccessible(true);
        return (Map<Integer, ?>) field.get(panel);
    }
}
