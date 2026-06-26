package dev.takesome.htmldom.devtools;

import java.util.List;

public record HtmlDomDevToolsSnapshot(
        long documentVersion,
        int nodeCount,
        int layoutBoxCount,
        int lineBoxCount,
        int inlineBoxCount,
        int paintNodeCount,
        int scrollContainerCount,
        List<HtmlDomDevToolsLayoutNode> layoutNodes,
        List<HtmlDomDevToolsPaintNode> paintNodes,
        List<HtmlDomDevToolsScrollNode> scrollNodes
) { }
