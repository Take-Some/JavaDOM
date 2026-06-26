package dev.takesome.htmldom.devtools;

public record HtmlDomDevToolsHitNode(
        int nodeId,
        String selector,
        float x,
        float y,
        float width,
        float height,
        boolean scrollAdjusted,
        boolean clipped
) { }
