package dev.takesome.htmldom.html.tags;



import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;

import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class InputHtmlTag extends UiHtmlBaseTagSpec {

    public InputHtmlTag() {

        super("input", Set.of(), "input", UiHtmlCommonAttributes.inputControl());

    }

}
