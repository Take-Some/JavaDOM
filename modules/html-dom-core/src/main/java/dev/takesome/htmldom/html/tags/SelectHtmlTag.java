package dev.takesome.htmldom.html.tags;

import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;
import java.util.Set;

public final class SelectHtmlTag extends UiHtmlBaseTagSpec {
    public SelectHtmlTag() {
        super("select", Set.of(), "combo_box", Set.of("id", "class", "style", "title", "action", "command", "data-*", "data-action", "data-target-id", "bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style", "name", "value", "disabled", "required", "placeholder", "icon", "closed-icon", "open-icon", "icon-color"));
    }
}
