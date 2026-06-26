package dev.takesome.htmldom.css;

/** Browser-style paint stage marker used by HtmlDom paint/devtools pipeline. */
public enum UiCssPaintPhase {
    BACKGROUND,
    BORDER,
    CONTENT,
    OUTLINE
}
