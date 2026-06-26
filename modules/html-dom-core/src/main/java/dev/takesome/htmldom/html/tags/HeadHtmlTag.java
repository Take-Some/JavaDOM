package dev.takesome.htmldom.html.tags;

import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;
import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;

public final class HeadHtmlTag extends UiHtmlBaseTagSpec {
    public HeadHtmlTag() {
        super("head", Set.of(), "panel", UiHtmlCommonAttributes.common("profile", "data-*"));
    }
}
