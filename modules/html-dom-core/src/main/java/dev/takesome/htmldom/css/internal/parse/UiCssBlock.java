package dev.takesome.htmldom.css.internal.parse;

/** One parsed CSS block: selector/at-rule head and declaration/body payload. */
public record UiCssBlock(String head, String body, int openOffset, int closeOffset, int nextIndex) {
    public UiCssBlock {
        head = head == null ? "" : head.trim();
        body = body == null ? "" : body;
        openOffset = Math.max(0, openOffset);
        closeOffset = Math.max(openOffset, closeOffset);
        nextIndex = Math.max(closeOffset + 1, nextIndex);
    }
}
