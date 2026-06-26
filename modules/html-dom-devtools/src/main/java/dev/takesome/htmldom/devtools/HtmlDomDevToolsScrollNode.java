package dev.takesome.htmldom.devtools;

public record HtmlDomDevToolsScrollNode(
        int nodeId,
        String selector,
        float viewportWidth,
        float viewportHeight,
        float contentWidth,
        float contentHeight,
        float scrollX,
        float scrollY,
        boolean scrollableX,
        boolean scrollableY,
        boolean scrollXEnabled,
        boolean scrollYEnabled,
        boolean clipX,
        boolean clipY
) { }
