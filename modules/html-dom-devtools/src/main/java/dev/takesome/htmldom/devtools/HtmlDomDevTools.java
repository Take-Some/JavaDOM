package dev.takesome.htmldom.devtools;

import dev.takesome.htmldom.css.UiCssBox;
import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.css.UiCssPaintNode;
import dev.takesome.htmldom.css.UiCssPaintTree;
import dev.takesome.htmldom.css.UiCssPaintTreeBuilder;
import dev.takesome.htmldom.css.UiCssScrollBox;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** DevTools snapshot factory for DOM/layout/line/inline/paint tree inspection. */
public final class HtmlDomDevTools {
    private HtmlDomDevTools() {
    }

    public static HtmlDomDevToolsSnapshot snapshot(UiDomDocument document, UiCssLayoutResult layout) {
        return snapshot(document, layout, new UiCssPaintTreeBuilder().build(document, layout));
    }

    public static HtmlDomDevToolsSnapshot snapshot(UiDomDocument document, UiCssLayoutResult layout, UiCssPaintTree paintTree) {
        return snapshot(document, layout, paintTree, Map.of());
    }

    public static HtmlDomDevToolsSnapshot snapshot(UiDomDocument document, UiCssLayoutResult layout, UiCssPaintTree paintTree, Map<Integer, UiCssScrollBox> scrollOverrides) {
        ArrayList<HtmlDomDevToolsLayoutNode> layoutNodes = new ArrayList<>();
        int lineBoxes = 0;
        int inlineBoxes = 0;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            UiCssBox box = layout.box(element).orElse(null);
            if (box == null) continue;
            int lines = layout.lineBoxes(element).size();
            int runs = layout.inlineBoxes(element).size();
            lineBoxes += lines;
            inlineBoxes += runs;
            layoutNodes.add(new HtmlDomDevToolsLayoutNode(
                    element.nodeId(), selector(element), box.x(), box.y(), box.width(), box.height(), lines, runs
            ));
        }
        ArrayList<HtmlDomDevToolsPaintNode> paintNodes = new ArrayList<>();
        for (UiCssPaintNode node : paintTree.order()) {
            paintNodes.add(new HtmlDomDevToolsPaintNode(
                    node.nodeId(), node.parentNodeId(), node.depth(), node.order(), node.selector(),
                    node.x(), node.y(), node.width(), node.height(), node.zIndex(), node.zIndexAuto(),
                    node.positioned(), node.stackingContext(), node.scrollContainer(), node.opacity(),
                    node.phases().stream().map(Enum::name).toList()
            ));
        }
        ArrayList<HtmlDomDevToolsScrollNode> scrollNodes = new ArrayList<>();
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            UiCssScrollBox scroll = scrollOverrides == null ? null : scrollOverrides.get(element.nodeId());
            if (scroll == null) scroll = layout.scrollBox(element).orElse(null);
            if (scroll == null) continue;
            scrollNodes.add(new HtmlDomDevToolsScrollNode(
                    element.nodeId(), selector(element), scroll.viewportWidth(), scroll.viewportHeight(),
                    scroll.contentWidth(), scroll.contentHeight(), scroll.scrollX(), scroll.scrollY(),
                    scroll.scrollableX(), scroll.scrollableY(), scroll.scrollXEnabled(), scroll.scrollYEnabled(),
                    scroll.clipX(), scroll.clipY()
            ));
        }
        return new HtmlDomDevToolsSnapshot(
                document.version(),
                UiDomTraversal.depthFirst(document.documentElement()).size(),
                layout.boxes().size(),
                lineBoxes,
                inlineBoxes,
                paintNodes.size(),
                scrollNodes.size(),
                List.copyOf(layoutNodes),
                List.copyOf(paintNodes),
                List.copyOf(scrollNodes)
        );
    }

    private static String selector(UiDomElement element) {
        String classes = String.join(" ", element.classList().values());
        return element.tagName() + (element.id().isBlank() ? "" : "#" + element.id()) + (classes.isBlank() ? "" : "." + classes.replace(' ', '.'));
    }
}
