package dev.takesome.htmldom.desktop;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HtmlDomPaintEngineColorTest {
    @Test
    void resolvesCssNamedColorsIncludingAliasesAndCssColorFourNames() {
        HtmlDomPaintEngine paint = new HtmlDomPaintEngine();

        assertEquals(new Color(0x8A2BE2), paint.color("blueviolet", null));
        assertEquals(new Color(0x663399), paint.color("rebeccapurple", null));
        assertEquals(new Color(0x808080), paint.color("gray", null));
        assertEquals(new Color(0x808080), paint.color("grey", null));
        assertEquals(new Color(0x00FFFF), paint.color("aqua", null));
        assertEquals(new Color(0x00FFFF), paint.color("cyan", null));
        assertEquals(new Color(0xFF00FF), paint.color("fuchsia", null));
        assertEquals(new Color(0xFF00FF), paint.color("magenta", null));
    }

    @Test
    void parsesBorderStyleFromShorthand() {
        HtmlDomPaintEngine paint = new HtmlDomPaintEngine();

        assertEquals("dotted", paint.borderStyle("", "1px dotted blueviolet"));
        assertEquals("dashed", paint.borderStyle("", "2px dashed rebeccapurple"));
        assertEquals("solid", paint.borderStyle("", "1px blueviolet"));
        assertEquals("none", paint.borderStyle("none", "1px dotted blueviolet"));
    }
}
