package dev.takesome.htmldom.devtools;

public record HtmlDomDevToolsLayoutNode(
        int nodeId,
        String selector,
        float x,
        float y,
        float width,
        float height,
        int lineBoxes,
        int inlineBoxes
) { }
