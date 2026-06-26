package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;

public final class HtmlDomPseudoStateController {
    private UiDomElement hoveredElement;
    private UiDomElement activeElement;

    public UiDomElement hoveredElement() { return hoveredElement; }
    public UiDomElement activeElement() { return activeElement; }

    public boolean setHoveredElement(UiDomElement root, UiDomElement element) {
        if (hoveredElement == element) return false;
        clearPseudo(root, "hover");
        hoveredElement = element;
        addPseudoChain(element, "hover");
        return true;
    }

    public boolean setActiveElement(UiDomElement root, UiDomElement element) {
        if (activeElement == element) return false;
        clearPseudo(root, "active");
        activeElement = element;
        if (element != null) element.setPseudoClass("active", true);
        return true;
    }

    public boolean clearActiveElement(UiDomElement root) {
        if (activeElement == null) return false;
        clearPseudo(root, "active");
        activeElement = null;
        return true;
    }

    private void addPseudoChain(UiDomElement element, String token) {
        UiDomElement current = element;
        while (current != null) {
            current.setPseudoClass(token, true);
            current = current.parent();
        }
    }

    private void clearPseudo(UiDomElement root, String token) {
        if (root == null) return;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(root)) element.setPseudoClass(token, false);
    }
}
