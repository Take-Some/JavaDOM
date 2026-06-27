package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.markup.UiMarkupParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

final class HtmlDomFocusControllerFormTest {
    @Test
    void formOwnerFindsNearestFormGroup() {
        UiDomDocument document = document("""
                <html><body>
                    <form id="profile">
                        <input id="name">
                    </form>
                </body></html>
                """);
        HtmlDomFocusController focus = new HtmlDomFocusController();

        UiDomElement name = element(document, "name");

        assertEquals("profile", focus.formOwner(name).id());
    }

    @Test
    void formControlsUseDocumentOrderAndSkipDisabledControls() {
        UiDomDocument document = document("""
                <html><body>
                    <form id="profile">
                        <input id="name">
                        <input id="disabled" disabled>
                        <textarea id="bio"></textarea>
                        <select id="slot"><option>One</option></select>
                        <button id="ok">OK</button>
                    </form>
                    <input id="outside">
                </body></html>
                """);
        HtmlDomFocusController focus = new HtmlDomFocusController();

        List<String> ids = focus.formControls(element(document, "profile")).stream().map(UiDomElement::id).toList();

        assertEquals(List.of("name", "bio", "slot", "ok"), ids);
        assertFalse(focus.focusable(element(document, "disabled")));
    }

    @Test
    void arrowNavigationCyclesInsideCurrentFormOnly() {
        UiDomDocument document = document("""
                <html><body>
                    <form id="profile">
                        <input id="name">
                        <textarea id="bio"></textarea>
                        <button id="ok">OK</button>
                    </form>
                    <input id="outside">
                </body></html>
                """);
        HtmlDomFocusController focus = new HtmlDomFocusController();

        focus.setFocusedElement(element(document, "name"));
        assertEquals("bio", focus.focusNextInForm(true).id());

        focus.setFocusedElement(element(document, "ok"));
        assertEquals("name", focus.focusNextInForm(true).id());

        focus.setFocusedElement(element(document, "name"));
        assertEquals("ok", focus.focusNextInForm(false).id());
    }

    @Test
    void formNavigationIsInactiveOutsideForms() {
        UiDomDocument document = document("""
                <html><body>
                    <input id="outside">
                </body></html>
                """);
        HtmlDomFocusController focus = new HtmlDomFocusController();

        focus.setFocusedElement(element(document, "outside"));

        assertNull(focus.focusNextInForm(true));
    }

    private UiDomDocument document(String markup) {
        return new UiMarkupParser().parse(markup, "form-navigation-test.html").dom();
    }

    private UiDomElement element(UiDomDocument document, String id) {
        return document.getElementById(id).orElseThrow();
    }
}
