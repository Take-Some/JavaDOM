package dev.takesome.htmldom.html.tags;



import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;

import dev.takesome.htmldom.html.UiHtmlCommonAttributes;

import java.util.Set;



public final class ImageHtmlTag extends UiHtmlBaseTagSpec {

    public ImageHtmlTag() {

        super("img", Set.of(), "image", UiHtmlCommonAttributes.media());

    }

}
