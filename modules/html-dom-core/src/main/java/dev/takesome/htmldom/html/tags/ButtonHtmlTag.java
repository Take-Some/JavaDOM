package dev.takesome.htmldom.html.tags;



import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;

import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class ButtonHtmlTag extends UiHtmlBaseTagSpec {

    public ButtonHtmlTag() {

        super("button", Set.of(), "button", UiHtmlCommonAttributes.interactiveControl("disabled", "value", "type"));

    }

}
