package dev.takesome.htmldom.dom;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.emptyIfNull;
/** Text node used by the HtmlDom DOM. */
public final class UiDomText extends UiDomNode {
    private String text;

    UiDomText(UiDomDocument ownerDocument, int nodeId, String text) {
        super(ownerDocument, nodeId);
        this.text = emptyIfNull(text);
    }

    @Override
    public UiDomNodeType nodeType() {
        return UiDomNodeType.TEXT;
    }

    @Override
    public String nodeName() {
        return "#text";
    }

    @Override
    public String textContent() {
        return text;
    }

    public String text() {
        return text;
    }

    public void setText(String nextText) {
        String normalized = emptyIfNull(nextText);
        if (text.equals(normalized)) return;
        String old = text;
        text = normalized;
        markDirty(UiDomMutationType.TEXT_CHANGED, "text", old, normalized);
    }

    @Override
    protected boolean canHaveChildren() {
        return false;
    }
}
