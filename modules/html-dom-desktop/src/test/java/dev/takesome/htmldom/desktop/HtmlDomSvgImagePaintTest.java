package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomSvgImagePaintTest {
    @Test
    void loadsSvgViaSharedImageLoaderFromClasspathBase() {
        HtmlDomImageLoader loader = new HtmlDomImageLoader(getClass().getClassLoader(), "html-dom/svg/document.ui.html");

        BufferedImage image = loader.load("icon.svg");

        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    void paintsSvgAsCssBackgroundImage() {
        HtmlDomPaintEngine paint = new HtmlDomPaintEngine(getClass().getClassLoader(), "html-dom/svg/document.ui.html");
        UiDomElement element = element("div");
        element.setComputedStyle("background-image", "url(\"icon.svg\")");
        element.setComputedStyle("background-size", "fill");
        element.setComputedStyle("background-position", "center center");
        BufferedImage canvas = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            paint.paintBackground(g, element, new Rectangle(0, 0, 32, 32));
        } finally {
            g.dispose();
        }

        assertTrue(((canvas.getRGB(16, 16) >>> 24) & 0xff) > 0, "SVG background should paint non-transparent pixels");
    }

    @Test
    void paintsSvgFromImgSrc() {
        HtmlDomPaintEngine paint = new HtmlDomPaintEngine(getClass().getClassLoader(), "html-dom/svg/document.ui.html");
        UiDomElement element = element("img");
        element.setAttribute("src", "icon.svg");
        element.setComputedStyle("object-fit", "fill");
        BufferedImage canvas = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        boolean painted;
        try {
            painted = paint.paintImage(g, element, new Rectangle(0, 0, 32, 32));
        } finally {
            g.dispose();
        }

        assertTrue(painted);
        assertTrue(((canvas.getRGB(16, 16) >>> 24) & 0xff) > 0, "SVG img should paint non-transparent pixels");
    }

    private UiDomElement element(String tagName) {
        UiDomDocument document = new UiDomDocument();
        UiDomElement element = document.createElement(tagName);
        document.setRoot(element);
        return element;
    }
}
