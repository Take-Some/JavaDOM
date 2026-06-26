package dev.takesome.htmldom.dom;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.emptyIfNull;
/** Immutable mutation event emitted by the UI DOM. */
public record UiDomMutation(
        long version,
        UiDomMutationType type,
        int nodeId,
        String nodeName,
        String key,
        String oldValue,
        String newValue
) {
    public UiDomMutation {
        if (type == null) throw new IllegalArgumentException("mutation type must not be null");
        nodeName = emptyIfNull(nodeName);
        key = emptyIfNull(key);
        oldValue = emptyIfNull(oldValue);
        newValue = emptyIfNull(newValue);
    }
}
