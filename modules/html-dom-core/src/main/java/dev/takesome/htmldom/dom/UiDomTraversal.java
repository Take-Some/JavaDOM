package dev.takesome.htmldom.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** DOM traversal helpers. */
public final class UiDomTraversal {
    private UiDomTraversal() {
    }

    public static List<UiDomNode> depthFirst(UiDomNode root) {
        Objects.requireNonNull(root, "root");
        ArrayList<UiDomNode> out = new ArrayList<>();
        walk(root, out);
        return List.copyOf(out);
    }

    public static List<UiDomElement> depthFirstElements(UiDomElement root) {
        Objects.requireNonNull(root, "root");
        ArrayList<UiDomElement> out = new ArrayList<>();
        walkElements(root, out);
        return List.copyOf(out);
    }

    private static void walk(UiDomNode node, ArrayList<UiDomNode> out) {
        out.add(node);
        for (UiDomNode child : node.children()) walk(child, out);
    }

    private static void walkElements(UiDomElement element, ArrayList<UiDomElement> out) {
        out.add(element);
        for (UiDomNode child : element.children()) {
            if (child instanceof UiDomElement childElement) walkElements(childElement, out);
        }
    }
}
