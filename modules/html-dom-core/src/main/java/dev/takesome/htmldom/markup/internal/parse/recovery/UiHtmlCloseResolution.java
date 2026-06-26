package dev.takesome.htmldom.markup.internal.parse.recovery;

/** Result of resolving a closing tag against the open-element stack. */
public record UiHtmlCloseResolution(
        boolean ignored,
        boolean mismatched,
        int matchIndex,
        String topTag
) {
    public static UiHtmlCloseResolution unmatched() {
        return new UiHtmlCloseResolution(true, false, -1, "");
    }

    public static UiHtmlCloseResolution matched(int matchIndex, boolean mismatched, String topTag) {
        return new UiHtmlCloseResolution(false, mismatched, matchIndex, topTag == null ? "" : topTag);
    }
}
