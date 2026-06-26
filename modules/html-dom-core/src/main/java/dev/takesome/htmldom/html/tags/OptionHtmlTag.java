package dev.takesome.htmldom.html.tags;

import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;
import java.util.Set;

public final class OptionHtmlTag extends UiHtmlBaseTagSpec {
    public OptionHtmlTag() {
        super("option", Set.of(), "text", Set.of("id", "class", "style", "title", "action", "command", "data-*", "data-action", "data-target-id", "bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style", "value", "disabled", "selected"));
    }
}
