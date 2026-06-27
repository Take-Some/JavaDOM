package dev.takesome.htmldom.dom;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiDomComputedStyleOverlayTest {
    @Test
    void animatedStyleOverridesBaseWithoutChangingDocumentVersion() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement element = document.createElement("div");
        document.setRoot(element);

        element.setComputedStyle("opacity", "1");
        long baseVersion = document.version();

        assertTrue(element.setAnimatedComputedStyle("opacity", "0.5"));

        assertEquals(baseVersion, document.version());
        assertEquals("1", element.baseComputedStyle().get("opacity"));
        assertEquals("0.5", element.computedStyle().get("opacity"));
        assertEquals("0.5", element.style("opacity"));
    }

    @Test
    void removingAnimatedStyleRestoresBaseValue() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement element = document.createElement("div");
        document.setRoot(element);

        element.setComputedStyle("background-color", "#000000");
        element.setAnimatedComputedStyle("background-color", "#ffffff");

        assertEquals("#ffffff", element.style("background-color"));
        assertTrue(element.removeAnimatedComputedStyle("background-color"));
        assertEquals("#000000", element.style("background-color"));
        assertFalse(element.removeAnimatedComputedStyle("background-color"));
    }

    @Test
    void computedStyleSnapshotMergesBaseAndAnimatedValues() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement element = document.createElement("div");
        document.setRoot(element);

        element.setComputedStyle("width", "100px");
        element.setComputedStyle("height", "40px");
        element.setAnimatedComputedStyle("opacity", "0.25");
        element.setAnimatedComputedStyle("width", "120px");

        Map<String, String> snapshot = element.computedStyle();

        assertEquals("120px", snapshot.get("width"));
        assertEquals("40px", snapshot.get("height"));
        assertEquals("0.25", snapshot.get("opacity"));
    }
}
