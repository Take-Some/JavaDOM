package dev.takesome.htmldom.html.tags;

import dev.takesome.htmldom.html.UiHtmlBaseTagSpec;
import java.util.Set;

public final class TextareaHtmlTag extends UiHtmlBaseTagSpec {
    public TextareaHtmlTag() {
        super("textarea", Set.of(), "input", Set.of("id", "class", "style", "title", "action", "command", "data-*", "data-action", "data-target-id", "bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style", "type", "name", "value", "checked", "disabled", "placeholder", "readonly", "required", "min", "max", "step"));
    }
}
