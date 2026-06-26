package dev.takesome.htmldom.html.tags;



import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;

import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class StyleHtmlTag extends UiHtmlBaseTagSpec {

    public StyleHtmlTag() {

        super("style", Set.of(), "style", UiHtmlCommonAttributes.styleRaw("display"));

    }

}
