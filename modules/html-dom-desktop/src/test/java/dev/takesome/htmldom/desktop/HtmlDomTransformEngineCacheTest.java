package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomTransformEngineCacheTest {
    @Test
    void sameTransformStyleReusesCachedPlan() {
        HtmlDomTransformEngine engine = new HtmlDomTransformEngine();
        UiDomElement element = element();
        element.setInternalComputedStyle("transform", "translateX(10px) scale(2) rotate(45deg)");

        AffineTransform first = engine.transform(element, rect());
        HtmlDomTransformEngine.Stats afterFirst = engine.stats();
        AffineTransform second = engine.transform(element, rect());
        HtmlDomTransformEngine.Stats afterSecond = engine.stats();

        assertFalse(first.isIdentity());
        assertEquals(first, second);
        assertEquals(1, afterFirst.cachedPlans());
        assertEquals(1, afterFirst.cacheMisses());
        assertEquals(0, afterFirst.cacheHits());
        assertEquals(1, afterFirst.parsedPlans());
        assertEquals(1, afterSecond.cacheHits());
        assertEquals(1, afterSecond.cacheMisses());
        assertEquals(1, afterSecond.parsedPlans());
    }

    @Test
    void changedTransformValueInvalidatesCachedPlanForNode() {
        HtmlDomTransformEngine engine = new HtmlDomTransformEngine();
        UiDomElement element = element();
        element.setInternalComputedStyle("transform", "translateX(10px)");

        engine.transform(element, rect());
        element.setInternalComputedStyle("transform", "translateX(20px)");
        AffineTransform changed = engine.transform(element, rect());

        HtmlDomTransformEngine.Stats stats = engine.stats();
        assertFalse(changed.isIdentity());
        assertEquals(1, stats.cachedPlans());
        assertEquals(0, stats.cacheHits());
        assertEquals(2, stats.cacheMisses());
        assertEquals(2, stats.parsedPlans());
    }

    @Test
    void noneTransformUsesCheapIdentityPlanWithoutParsing() {
        HtmlDomTransformEngine engine = new HtmlDomTransformEngine();
        UiDomElement element = element();

        boolean appliedFirst = apply(engine, element);
        boolean appliedSecond = apply(engine, element);

        HtmlDomTransformEngine.Stats stats = engine.stats();
        assertFalse(appliedFirst);
        assertFalse(appliedSecond);
        assertEquals(1, stats.cachedPlans());
        assertEquals(1, stats.cacheMisses());
        assertEquals(1, stats.cacheHits());
        assertEquals(0, stats.parsedPlans());
    }

    @Test
    void cachedPlanSupportsIndividualAndFunctionTransforms() {
        HtmlDomTransformEngine engine = new HtmlDomTransformEngine();
        UiDomElement element = element();
        element.setInternalComputedStyle("transform-origin", "0 0");
        element.setInternalComputedStyle("translate", "10px 20px");
        element.setInternalComputedStyle("scale", "2 3");
        element.setInternalComputedStyle("rotate", "0deg");
        element.setInternalComputedStyle("transform", "translate(5px, 6px) translateX(7px) translateY(8px) scaleX(2) scaleY(3) rotate(0deg)");

        AffineTransform transform = engine.transform(element, rect());
        engine.transform(element, rect());

        HtmlDomTransformEngine.Stats stats = engine.stats();
        assertFalse(transform.isIdentity());
        assertEquals(1, stats.cachedPlans());
        assertEquals(1, stats.cacheMisses());
        assertEquals(1, stats.cacheHits());
        assertEquals(1, stats.parsedPlans());
    }

    private boolean apply(HtmlDomTransformEngine engine, UiDomElement element) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            return engine.apply(g, element, rect());
        } finally {
            g.dispose();
        }
    }

    private UiDomElement element() {
        UiDomDocument document = UiDomDocument.parse("<html><body><div id='box'>Box</div></body></html>");
        return document.getElementById("box").orElseThrow();
    }

    private Rectangle rect() {
        return new Rectangle(0, 0, 100, 50);
    }
}
