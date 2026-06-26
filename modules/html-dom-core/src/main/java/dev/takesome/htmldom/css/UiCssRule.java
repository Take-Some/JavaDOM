package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomSelector;

import java.util.Collections;
import java.util.List;

/** Parsed CSS selector rule. */
public final class UiCssRule {
    private final String selectorText;
    private final UiDomSelector selector;
    private final UiCssSpecificity specificity;
    private final List<UiCssDeclaration> declarations;
    private final int order;

    public UiCssRule(String selectorText, List<UiCssDeclaration> declarations, int order) {
        if (selectorText == null || selectorText.isBlank()) throw new IllegalArgumentException("selector must not be blank");
        this.selectorText = selectorText.trim();
        this.selector = UiDomSelector.parse(this.selectorText);
        this.specificity = UiCssSpecificity.of(this.selectorText);
        this.declarations = Collections.unmodifiableList(declarations == null ? List.of() : List.copyOf(declarations));
        this.order = order;
    }

    public String selectorText() {
        return selectorText;
    }

    public UiCssSpecificity specificity() {
        return specificity;
    }

    public List<UiCssDeclaration> declarations() {
        return declarations;
    }

    public int order() {
        return order;
    }

    public String pseudoElement() {
        return selector.pseudoElement();
    }

    public boolean hasPseudoElement() {
        return selector.hasPseudoElement();
    }

    public boolean matches(UiDomElement element) {
        return selector.matches(element);
    }
}
