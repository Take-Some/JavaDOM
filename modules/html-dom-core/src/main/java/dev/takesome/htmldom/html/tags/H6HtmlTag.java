package dev.takesome.htmldom.html.tags;

import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;
import java.util.Set;

public final class H6HtmlTag extends UiHtmlBaseTagSpec {
    public H6HtmlTag() {
        super("h6", Set.of(), "text", Set.of("id", "class", "style", "title", "action", "command", "data-*", "data-action", "data-target-id", "bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style"));
    }
}
