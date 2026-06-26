package dev.takesome.htmldom.html.tags;

import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;
import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class MetaHtmlTag extends UiHtmlBaseTagSpec {
    public MetaHtmlTag() {
        super("meta", Set.of(), "panel", UiHtmlCommonAttributes.common(
                "charset", "name", "content", "http-equiv", "property", "scheme", "data-*"
        ));
    }
}
