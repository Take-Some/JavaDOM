package dev.takesome.htmldom.markup;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiHtmlStylesheetLinkParsingTest {
    @Test
    void linkStylesheetIsVoidAndDoesNotSwallowBody() {
        UiMarkupDocument document = new UiMarkupParser().parse(
                "<html><head><link rel=\"stylesheet\" href=\"app.css\"><title>Demo</title></head><body><section id=\"ok\">OK</section></body></html>",
                "link-test.html"
        );
        UiDomDocument dom = document.dom();
        UiDomElement link = dom.getElementsByTagName("link").get(0);

        assertTrue(dom.body().isPresent(), "body must remain a sibling of head");
        assertTrue(dom.getElementById("ok").isPresent(), "body content must not be parsed as link children");
        assertEquals("head", link.parent().tagName());
        assertEquals(0, link.childCount(), "link is a void metadata element");
        assertFalse(document.hasDiagnostics(), "valid link/head/body document should not produce parser diagnostics");
    }
}
