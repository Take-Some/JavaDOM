package dev.takesome.htmldom.html.tags;



import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;

import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class BodyHtmlTag extends UiHtmlBaseTagSpec {

    public BodyHtmlTag() {

        super("body", Set.of(), "panel", UiHtmlCommonAttributes.root());

    }

}
