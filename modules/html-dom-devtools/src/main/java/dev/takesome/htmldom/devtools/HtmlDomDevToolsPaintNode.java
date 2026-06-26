package dev.takesome.htmldom.devtools;

import java.util.List;

public record HtmlDomDevToolsPaintNode(
        int nodeId,
        int parentNodeId,
        int depth,
        int order,
        String selector,
        float x,
        float y,
        float width,
        float height,
        int zIndex,
        boolean zIndexAuto,
        boolean positioned,
        boolean stackingContext,
        boolean scrollContainer,
        float opacity,
        List<String> phases
) { }
