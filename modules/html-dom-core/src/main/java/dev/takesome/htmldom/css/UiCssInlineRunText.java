package dev.takesome.htmldom.css;

/** Text selected for a committed inline layout run. */
public record UiCssInlineRunText(String paintText, String lineText) {
    public UiCssInlineRunText {
        paintText = paintText == null ? "" : paintText;
        lineText = lineText == null ? "" : lineText;
    }

    public static UiCssInlineRunText text(String text) {
        return new UiCssInlineRunText(text, text);
    }

    public static UiCssInlineRunText paintOnly(String text) {
        return new UiCssInlineRunText(text, "");
    }

    public static UiCssInlineRunText lineOnly(String text) {
        return new UiCssInlineRunText("", text);
    }
}
