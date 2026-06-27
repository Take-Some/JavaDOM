package dev.takesome.htmldom.css;

/** Extension point for overriding paint/line text of inline layout runs. */
@FunctionalInterface
public interface UiCssInlineRunTextHook {
    UiCssInlineRunText resolve(UiCssInlineRunTextEvent event);

    static UiCssInlineRunTextHook identity() {
        return event -> event == null ? UiCssInlineRunText.text("") : UiCssInlineRunText.text(event.text());
    }
}
