package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Runtime focus model and focus pseudo-class projection. */
public final class HtmlDomFocusController {
    private UiDomElement focusedElement;

    public UiDomElement focusedElement() {
        return focusedElement;
    }

    public boolean focusable(UiDomElement element) {
        if (element == null || skip(element) || element.hasAttribute("disabled")) return false;
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
        if (document == null || document.rootOptional().isEmpty()) return out;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            if (focusable(element)) out.add(element);
        }
        return out;
    }

    /**
     * Returns the nearest owning {@code <form>} for an element, matching the
     * practical HtmlDom form model: forms are local input-navigation groups,
     * not browser/network submission boundaries.
     */
    public UiDomElement formOwner(UiDomElement element) {
        UiDomElement current = element;
        while (current != null) {
            if ("form".equals(current.tagName())) return current;
            current = current.parent();
        }
        return null;
    }

    public List<UiDomElement> formControls(UiDomElement form) {
        ArrayList<UiDomElement> out = new ArrayList<>();
        if (form == null || !"form".equals(form.tagName())) return out;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(form)) {
            if (element == form) continue;
            UiDomElement owner = formOwner(element);
            if (owner != form) continue;
            if (formNavigable(element)) out.add(element);
        }
        return out;
    }

    public UiDomElement focusNextInForm(boolean forward) {
        UiDomElement form = formOwner(focusedElement);
        if (form == null) return null;
        List<UiDomElement> controls = formControls(form);
        if (controls.size() < 2) return null;
        int current = controls.indexOf(focusedElement);
        if (current < 0) return forward ? controls.get(0) : controls.get(controls.size() - 1);
        int next = forward ? current + 1 : current - 1;
        if (next < 0) next = controls.size() - 1;
        if (next >= controls.size()) next = 0;
        UiDomElement candidate = controls.get(next);
        return candidate == focusedElement ? null : candidate;
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

    private boolean formNavigable(UiDomElement element) {
        if (!focusable(element)) return false;
        if (element.hasAttribute("tabindex")) return true;
        return standardFormControl(element.tagName());
    }

    private boolean standardFormControl(String tag) {
        String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("button") || normalized.equals("input") || normalized.equals("select") || normalized.equals("textarea");
    }

    private boolean skip(UiDomElement element) {
        String tag = element.tagName();
        if (tag.equals("head") || tag.equals("title") || tag.equals("style") || tag.equals("option")) return true;
        return "none".equals(element.style("display", ""));
    }
}
