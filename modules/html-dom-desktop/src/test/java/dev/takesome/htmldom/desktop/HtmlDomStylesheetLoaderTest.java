package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiStylesheet;
import dev.takesome.htmldom.markup.UiMarkupDocument;
import dev.takesome.htmldom.markup.UiMarkupParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomStylesheetLoaderTest {
    @Test
    void loadsLinkedStylesheetFromDocumentHead() {
        UiMarkupDocument document = new UiMarkupParser().parse(
                "<html><head><link rel=\"stylesheet\" href=\"showcase.ui.css\"></head><body><section class=\"page\"></section></body></html>",
                "html-dom/bundled/test.ui.html"
        );

        UiStylesheet stylesheet = new HtmlDomStylesheetLoader(getClass().getClassLoader()).load(
                document.dom(),
                "",
                "html-dom/bundled/test.ui.html",
                "html-dom/bundled/"
        );

        assertFalse(stylesheet.rules().isEmpty(), "linked stylesheet must produce CSS rules");
        assertTrue(stylesheet.rules().stream().anyMatch(rule -> ".page".equals(rule.selectorText())), "linked showcase stylesheet must contain .page rule");
    }

    @Test
    void skipsDisabledOrNonScreenStylesheetLinks() {
        UiMarkupDocument document = new UiMarkupParser().parse(
                "<html><head>"
                        + "<link rel=\"stylesheet\" href=\"showcase.ui.css\" disabled>"
                        + "<link rel=\"stylesheet\" href=\"showcase.ui.css\" media=\"print\">"
                        + "</head><body><section class=\"page\"></section></body></html>",
                "html-dom/bundled/test.ui.html"
        );

        UiStylesheet stylesheet = new HtmlDomStylesheetLoader(getClass().getClassLoader()).load(
                document.dom(),
                "",
                "html-dom/bundled/test.ui.html",
                "html-dom/bundled/"
        );

        assertTrue(stylesheet.rules().isEmpty(), "disabled and print-only links must not be applied to the screen renderer");
    }
}
