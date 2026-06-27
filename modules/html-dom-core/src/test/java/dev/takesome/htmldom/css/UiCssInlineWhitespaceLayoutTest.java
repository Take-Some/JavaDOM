package dev.takesome.htmldom.css;

import dev.takesome.htmldom.markup.UiMarkupDocument;
import dev.takesome.htmldom.markup.UiMarkupParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiCssInlineWhitespaceLayoutTest {
    @Test
    void collapsedWhitespaceBetweenInlineFragmentsIsPartOfInlineRunText() {
        UiMarkupDocument markup = new UiMarkupParser().parse(
                "<html><body><section class=\"copy\">Hello <span>world</span></section></body></html>",
                "inline-whitespace.html"
        );
        UiStylesheet stylesheet = UiCssUserAgentStylesheet.stylesheet().plus(new UiCssParser().parse("""
                .copy {
                    width: 300px;
                    height: fit-content;
                    font-size: 16px;
                }
                span { display: inline; }
                """));
        new UiCssCascade().apply(markup.dom(), stylesheet);
        var layout = new UiCssLayoutEngine().layout(markup.dom(), 640, 480);
        var section = markup.dom().querySelector("section.copy").orElseThrow();
        var runs = layout.inlineBoxes(section);

        assertFalse(runs.isEmpty(), "inline formatter must produce paintable inline runs");
        assertEquals("Hello world", layout.lineBoxes(section).get(0).text());
        assertTrue(runs.size() >= 2, "separate text fragments should remain separate inline runs");
        assertEquals("Hello", runs.get(0).text());
        assertEquals("world", runs.get(1).text());
        assertTrue(runs.get(1).x() > runs.get(0).x() + runs.get(0).width(), "collapsed whitespace must be represented as layout advance between runs");
    }

    @Test
    void inlineRunTextHookCanOverridePaintTextWithoutChangingLineText() {
        UiMarkupDocument markup = new UiMarkupParser().parse(
                "<html><body><section class=\"copy\">Hello</section></body></html>",
                "inline-text-hook.html"
        );
        UiStylesheet stylesheet = UiCssUserAgentStylesheet.stylesheet().plus(new UiCssParser().parse("""
                .copy {
                    width: 300px;
                    height: fit-content;
                    font-size: 16px;
                }
                """));
        new UiCssCascade().apply(markup.dom(), stylesheet);

        UiCssInlineRunTextHook hook = event -> event.text().isBlank()
                ? UiCssInlineRunText.text(event.text())
                : new UiCssInlineRunText(event.text().toUpperCase(), event.text());
        var layout = new UiCssLayoutEngine(UiCssPropertyRegistry.loadBuiltins(), UiIntrinsicTextMeasurer.heuristic(), hook).layout(markup.dom(), 640, 480);
        var section = markup.dom().querySelector("section.copy").orElseThrow();

        assertTrue(layout.inlineBoxes(section).stream().anyMatch(run -> "HELLO".equals(run.text())));
        assertEquals("Hello", layout.lineBoxes(section).get(0).text());
    }

    @Test
    void inlinePaddingCreatesAtomicInlineBoxWithBoxMetrics() {
        UiMarkupDocument markup = new UiMarkupParser().parse(
                "<html><body><section class=\"copy\"><span class=\"icon\">x</span>Profile</section></body></html>",
                "inline-padding.html"
        );
        UiStylesheet stylesheet = UiCssUserAgentStylesheet.stylesheet().plus(new UiCssParser().parse("""
                .copy {
                    width: 300px;
                    height: fit-content;
                    font-size: 16px;
                }
                .icon {
                    padding: 5px;
                    font-size: 10px;
                }
                """));
        new UiCssCascade().apply(markup.dom(), stylesheet);
        var layout = new UiCssLayoutEngine().layout(markup.dom(), 640, 480);
        var section = markup.dom().querySelector("section.copy").orElseThrow();
        var runs = layout.inlineBoxes(section);

        assertTrue(runs.size() >= 2, "icon and text should become separate inline runs");
        var padded = runs.stream().filter(UiCssInlineBox::replaced).findFirst().orElseThrow();
        assertTrue(padded.width() >= 11f, "horizontal padding must contribute to inline box width");
        assertTrue(padded.height() >= 20f, "vertical padding must contribute to inline box height");
    }

}
