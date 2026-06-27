package dev.takesome.htmldom.desktop;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomDevToolsCssValueTest {
    @Test
    void parsesAndFormatsAlphaColors() {
        Color color = HtmlDomDevToolsCssValue.parseColor("rgba(12, 34, 56, 0.5)");

        assertEquals(new Color(12, 34, 56, 128), color);
        assertEquals("rgba(12,34,56,0.502)", HtmlDomDevToolsCssValue.formatColor(color));
    }

    @Test
    void nudgesMetricValuesWithTheirUnit() {
        assertEquals("13px", HtmlDomDevToolsCssValue.nudgeMetric("12px", 1, false, false));
        assertEquals("2.5rem", HtmlDomDevToolsCssValue.nudgeMetric("2.4rem", 1, false, false));
        assertEquals("900ms", HtmlDomDevToolsCssValue.nudgeMetric("950ms", -1, false, false));
    }

    @Test
    void exposesKeywordOptionsForClosedValueProperties() {
        assertEquals(List.of("none", "block", "inline", "inline-block", "flex", "inline-flex"), HtmlDomDevToolsCssValue.keywordOptions("display"));
        assertTrue(HtmlDomDevToolsCssValue.keywordOptions("border-style").contains("dashed"));
    }
}
