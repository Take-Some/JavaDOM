package dev.takesome.htmldom.html.tags;

import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;
import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class HtmlHtmlTag extends UiHtmlBaseTagSpec {
    public HtmlHtmlTag() {
        super("html", Set.of(), "panel", UiHtmlCommonAttributes.root("lang", "dir"));
    }
}
