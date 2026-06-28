package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomDirtyRegionEngineTest {
    private final HtmlDomDirtyRegionEngine dirty = new HtmlDomDirtyRegionEngine();
    private final HtmlDomTransformEngine transform = new HtmlDomTransformEngine();

    @Test
    void opacityInvalidatesOnlyElementRect() {
        UiDomElement element = element();

        Rectangle out = dirty.dirtyRect(element, rect(), Set.of("opacity"), transform);

        assertEquals(rect(), out);
    }

    @Test
    void translateInvalidatesCurrentVisualBoundsAndBaseRectForPreviousUnion() {
        UiDomElement element = element();
        element.setInternalComputedStyle("transform", "translateX(40px)");

        Rectangle out = dirty.dirtyRect(element, rect(), Set.of("transform"), transform);

        assertEquals(new Rectangle(10, 20, 140, 50), out);
    }

    @Test
    void scaleIncludesExpandedTransformedBounds() {
        UiDomElement element = element();
        element.setInternalComputedStyle("transform-origin", "0 0");
        element.setInternalComputedStyle("transform", "scale(2)");

        Rectangle out = dirty.dirtyRect(element, rect(), Set.of("transform"), transform);

        assertEquals(new Rectangle(10, 20, 200, 100), out);
    }

    @Test
    void rotateIncludesExpandedRotatedBounds() {
        UiDomElement element = element();
        element.setInternalComputedStyle("transform", "rotate(45deg)");

        Rectangle out = dirty.dirtyRect(element, rect(), Set.of("transform"), transform);

        assertTrue(out.width > rect().width, "rotated dirty bounds should be wider than the base rect");
        assertTrue(out.height > rect().height, "rotated dirty bounds should be taller than the base rect");
    }

    @Test
    void boxShadowExpandsByOffsetBlurAndSpread() {
        UiDomElement element = element();
        element.setInternalComputedStyle("box-shadow", "10px 5px 20px 3px rgba(0,0,0,0.5)");

        Rectangle out = dirty.dirtyRect(element, rect(), Set.of("box-shadow"), transform);

        assertEquals(new Rectangle(-23, -13, 166, 116), out);
    }

    @Test
    void borderAndOutlineExpandPaintDirtyRegion() {
        UiDomElement element = element();
        element.setInternalComputedStyle("border-width", "4px");
        element.setInternalComputedStyle("outline-width", "7px");

        Rectangle out = dirty.dirtyRect(element, rect(), Set.of("outline-color"), transform);

        assertEquals(new Rectangle(3, 13, 114, 64), out);
    }

    private UiDomElement element() {
        UiDomDocument document = UiDomDocument.parse("<html><body><div id='box'>Box</div></body></html>");
        return document.getElementById("box").orElseThrow();
    }

    private Rectangle rect() {
        return new Rectangle(10, 20, 100, 50);
    }
}
