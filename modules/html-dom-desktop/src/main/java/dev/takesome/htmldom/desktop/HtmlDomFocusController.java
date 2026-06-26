package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;

import java.util.ArrayList;
import java.util.List;

/** Runtime focus model and focus pseudo-class projection. */
public final class HtmlDomFocusController {
    private UiDomElement focusedElement;

    public UiDomElement focusedElement() {
        return focusedElement;
    }

    public boolean focusable(UiDomElement element) {
        if (element == null || skip(element)) return false;
        String tabindex = element.attribute("tabindex", "").trim();
        if ("-1".equals(tabindex)) return false;
        if (!tabindex.isBlank()) return true;
        String tag = element.tagName();
        return tag.equals("button") || tag.equals("input") || tag.equals("select") || tag.equals("textarea") || (tag.equals("a") && !element.attribute("href", "").isBlank());
    }

    public UiDomElement focusNext(UiDomDocument document, boolean forward) {
        List<UiDomElement> focusables = focusableElements(document);
        if (focusables.isEmpty()) return null;
        int current = focusedElement == null ? -1 : focusables.indexOf(focusedElement);
        int next = forward ? current + 1 : current - 1;
        if (next < 0) next = focusables.size() - 1;
        if (next >= focusables.size()) next = 0;
        return focusables.get(next);
    }

    public List<UiDomElement> focusableElements(UiDomDocument document) {
        ArrayList<UiDomElement> out = new ArrayList<>();
        if (document == null) return out;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            if (focusable(element)) out.add(element);
        }
        return out;
    }

    public void setFocusedElement(UiDomElement element) {
        if (element == null || !focusable(element)) return;
        focusedElement = element;
        updatePseudoState(element);
    }

    public void clear(UiDomDocument document) {
        focusedElement = null;
        if (document != null) clearPseudoState(document);
    }

    public void clearPseudoState(UiDomDocument document) {
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            element.setPseudoClass("focus", false);
            element.setPseudoClass("focus-visible", false);
            element.setPseudoClass("focus-within", false);
        }
    }

    private void updatePseudoState(UiDomElement focused) {
        UiDomElement root = focused;
        while (root.parent() != null) root = root.parent();
        for (UiDomElement element : UiDomTraversal.depthFirstElements(root)) {
            element.setPseudoClass("focus", false);
            element.setPseudoClass("focus-visible", false);
            element.setPseudoClass("focus-within", false);
        }
        focused.setPseudoClass("focus", true);
        focused.setPseudoClass("focus-visible", true);
        UiDomElement current = focused;
        while (current != null) {
            current.setPseudoClass("focus-within", true);
            current = current.parent();
        }
    }

    private boolean skip(UiDomElement element) {
        String tag = element.tagName();
        if (tag.equals("head") || tag.equals("title") || tag.equals("style") || tag.equals("option")) return true;
        return "none".equals(element.style("display", ""));
    }
}
