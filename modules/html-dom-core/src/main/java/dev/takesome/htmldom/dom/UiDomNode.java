package dev.takesome.htmldom.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base node for the HtmlDom DOM.
 *
 * <p>This is a retained engine-owned tree, not {@code org.w3c.dom}. It is small,
 * deterministic and designed to feed our style/layout/render pipeline.</p>
 */
public abstract class UiDomNode {
    private final UiDomDocument ownerDocument;
    private final int nodeId;
    private final ArrayList<UiDomNode> children = new ArrayList<>();
    private UiDomElement parent;
    private long version;
    private boolean dirty = true;

    UiDomNode(UiDomDocument ownerDocument, int nodeId) {
        this.ownerDocument = Objects.requireNonNull(ownerDocument, "ownerDocument");
        this.nodeId = nodeId;
    }

    public final UiDomDocument ownerDocument() {
        return ownerDocument;
    }

    public final int nodeId() {
        return nodeId;
    }

    public final UiDomElement parent() {
        return parent;
    }

    public final boolean hasParent() {
        return parent != null;
    }

    public final boolean dirty() {
        return dirty;
    }

    public final long version() {
        return version;
    }

    public final List<UiDomNode> children() {
        return Collections.unmodifiableList(children);
    }

    public final int childCount() {
        return children.size();
    }

    public final UiDomNode child(int index) {
        return children.get(index);
    }

    public final void appendChild(UiDomNode child) {
        insertChild(children.size(), child);
    }

    public final void insertChild(int index, UiDomNode child) {
        ensureCanHaveChildren();
        Objects.requireNonNull(child, "child");
        if (child.ownerDocument() != ownerDocument) {
            throw new IllegalArgumentException("Cannot insert node from a different UiDomDocument");
        }
        if (!(this instanceof UiDomElement elementParent)) {
            throw new IllegalStateException("Only element nodes can be parents: " + nodeName());
        }
        if (child == this || child.isAncestorOf(this)) {
            throw new IllegalArgumentException("Cannot create a cycle in the UI DOM");
        }
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        int safeIndex = Math.max(0, Math.min(index, children.size()));
        children.add(safeIndex, child);
        child.parent = elementParent;
        markDirty(UiDomMutationType.CHILD_ADDED, "", "", child.nodeName());
    }

    public final UiDomNode removeChild(UiDomNode child) {
        Objects.requireNonNull(child, "child");
        int index = children.indexOf(child);
        if (index < 0) throw new IllegalArgumentException("Node is not a child: " + child.nodeName());
        UiDomNode removed = children.remove(index);
        removed.parent = null;
        markDirty(UiDomMutationType.CHILD_REMOVED, "", removed.nodeName(), "");
        return removed;
    }

    public final void clearChildren() {
        if (children.isEmpty()) return;
        for (UiDomNode child : children) child.parent = null;
        children.clear();
        markDirty(UiDomMutationType.CHILDREN_CLEARED, "", "", "");
    }

    public String textContent() {
        StringBuilder text = new StringBuilder();
        collectText(this, text);
        return text.toString();
    }

    public final void clearDirty() {
        dirty = false;
        for (UiDomNode child : children) child.clearDirty();
    }

    public abstract UiDomNodeType nodeType();

    public abstract String nodeName();

    protected boolean canHaveChildren() {
        return true;
    }

    protected final void markDirty(UiDomMutationType type, String key, String oldValue, String newValue) {
        dirty = true;
        version = ownerDocument.nextVersion(type, this, key, oldValue, newValue);
        if (parent != null) parent.markAncestorDirty();
    }

    void markAncestorDirty() {
        dirty = true;
        if (parent != null) parent.markAncestorDirty();
    }

    private void ensureCanHaveChildren() {
        if (!canHaveChildren()) {
            throw new IllegalStateException(nodeType() + " node cannot have children: " + nodeName());
        }
    }

    private boolean isAncestorOf(UiDomNode candidate) {
        UiDomNode cursor = candidate;
        while (cursor != null) {
            if (cursor == this) return true;
            cursor = cursor.parent;
        }
        return false;
    }

    private static void collectText(UiDomNode node, StringBuilder out) {
        if (node instanceof UiDomText text) {
            out.append(text.text());
            return;
        }
        for (UiDomNode child : node.children) collectText(child, out);
    }
}
