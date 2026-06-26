package dev.takesome.htmldom.markup;



import java.util.Objects;



/** Immutable parser options for HtmlDom Markup. */

public final class UiMarkupParseOptions {

    private final UiMarkupParseMode mode;



    private UiMarkupParseOptions(UiMarkupParseMode mode) {

        this.mode = Objects.requireNonNull(mode, "mode");

    }



    public static UiMarkupParseOptions runtime() {

        return new UiMarkupParseOptions(UiMarkupParseMode.RUNTIME);

    }



    public static UiMarkupParseOptions editor() {

        return new UiMarkupParseOptions(UiMarkupParseMode.EDITOR);

    }



    public static UiMarkupParseOptions strict() {

        return new UiMarkupParseOptions(UiMarkupParseMode.STRICT);

    }



    public static UiMarkupParseOptions of(UiMarkupParseMode mode) {

        return new UiMarkupParseOptions(mode == null ? UiMarkupParseMode.RUNTIME : mode);

    }



    public UiMarkupParseMode mode() {

        return mode;

    }



    public boolean isStrict() {

        return mode == UiMarkupParseMode.STRICT;

    }



    public boolean isEditor() {

        return mode == UiMarkupParseMode.EDITOR;

    }

}
