package dev.takesome.htmldom.dom;

/** Mutations tracked by the retained UI document tree. */
public enum UiDomMutationType {
    ROOT_REPLACED,
    CHILD_ADDED,
    CHILD_REMOVED,
    CHILDREN_CLEARED,
    ATTRIBUTE_CHANGED,
    ATTRIBUTE_REMOVED,
    TEXT_CHANGED,
    STYLE_CHANGED
}
