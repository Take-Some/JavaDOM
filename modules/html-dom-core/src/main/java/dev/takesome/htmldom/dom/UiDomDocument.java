package dev.takesome.htmldom.dom;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Engine-owned retained UI document tree. */
public final class UiDomDocument {
    private final AtomicInteger nextNodeId = new AtomicInteger(1);
    private final ArrayDeque<UiDomMutation> mutations = new ArrayDeque<>();
    private UiDomElement root;
    private long version;

    public static UiDomDocument parse(String markupSource) {
        return new UiDomParser().parse(markupSource);
    }

    public UiDomElement createElement(String tagName) {
        return new UiDomElement(this, nextNodeId.getAndIncrement(), tagName);
    }

    public UiDomText createText(String text) {
        return new UiDomText(this, nextNodeId.getAndIncrement(), text);
    }

    public UiDomElement root() {
        if (root == null) throw new IllegalStateException("UI DOM has no root element");
        return root;
    }

    public Optional<UiDomElement> rootOptional() {
        return Optional.ofNullable(root);
    }

    public UiDomElement documentElement() {
        return root();
    }

    public Optional<UiDomElement> head() {
        if (root == null) return Optional.empty();
        if ("head".equals(root.tagName())) return Optional.of(root);
        return firstChild(root, "head");
    }

    public Optional<UiDomElement> body() {
        if (root == null) return Optional.empty();
        if ("body".equals(root.tagName())) return Optional.of(root);
        return firstChild(root, "body");
    }

    public UiDomElement renderRoot() {
        return body().orElse(root());
    }

    public void setRoot(UiDomElement nextRoot) {
        Objects.requireNonNull(nextRoot, "nextRoot");
        if (nextRoot.ownerDocument() != this) throw new IllegalArgumentException("Root belongs to another UiDomDocument");
        if (nextRoot.hasParent()) nextRoot.parent().removeChild(nextRoot);
        this.root = nextRoot;
        nextVersion(UiDomMutationType.ROOT_REPLACED, nextRoot, "", "", nextRoot.nodeName());
    }

    public long version() {
        return version;
    }

    public Optional<UiDomElement> getElementById(String id) {
        if (id == null || id.isBlank() || root == null) return Optional.empty();
        return UiDomTraversal.depthFirstElements(root).stream().filter(element -> id.equals(element.id())).findFirst();
    }

    public List<UiDomElement> getElementsByTagName(String tagName) {
        if (tagName == null || tagName.isBlank() || root == null) return List.of();
        String tag = tagName.trim().toLowerCase(java.util.Locale.ROOT);
        return UiDomTraversal.depthFirstElements(root).stream().filter(element -> tag.equals(element.tagName())).toList();
    }

    public List<UiDomElement> getElementsByClassName(String className) {
        if (className == null || className.isBlank() || root == null) return List.of();
        String clazz = className.trim().toLowerCase(java.util.Locale.ROOT);
        return UiDomTraversal.depthFirstElements(root).stream().filter(element -> element.classList().contains(clazz)).toList();
    }

    public Optional<UiDomElement> querySelector(String selector) {
        List<UiDomElement> matches = querySelectorAll(selector);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public List<UiDomElement> querySelectorAll(String selector) {
        if (root == null) return List.of();
        UiDomSelector parsed = UiDomSelector.parse(selector);
        return UiDomTraversal.depthFirstElements(root).stream().filter(parsed::matches).toList();
    }

    public List<UiDomMutation> mutations() {
        return Collections.unmodifiableList(new ArrayList<>(mutations));
    }

    public List<UiDomMutation> drainMutations() {
        ArrayList<UiDomMutation> out = new ArrayList<>(mutations);
        mutations.clear();
        return out;
    }


    private Optional<UiDomElement> firstChild(UiDomElement parent, String tag) {
        if (parent == null || tag == null || tag.isBlank()) return Optional.empty();
        String normalized = tag.trim().toLowerCase(java.util.Locale.ROOT);
        for (UiDomNode child : parent.children()) {
            if (child instanceof UiDomElement element && normalized.equals(element.tagName())) return Optional.of(element);
        }
        return Optional.empty();
    }

    long nextVersion(UiDomMutationType type, UiDomNode node, String key, String oldValue, String newValue) {
        version++;
        mutations.addLast(new UiDomMutation(version, type, node.nodeId(), node.nodeName(), key, oldValue, newValue));
        while (mutations.size() > 512) mutations.removeFirst();
        return version;
    }
}
