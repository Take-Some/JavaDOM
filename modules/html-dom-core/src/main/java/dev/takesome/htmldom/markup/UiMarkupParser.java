package dev.takesome.htmldom.markup;



import dev.takesome.htmldom.markup.internal.parse.UiMarkupXmlParser;



/** Public facade for parsing HtmlDom Markup documents. */

public final class UiMarkupParser {

    private final UiMarkupXmlParser parser = new UiMarkupXmlParser();



    public UiMarkupDocument parse(String source) {

        return parser.parse(source, UiMarkupParseOptions.runtime());

    }



    public UiMarkupDocument parse(String source, UiMarkupParseMode mode) {

        return parser.parse(source, UiMarkupParseOptions.of(mode));

    }



    public UiMarkupDocument parse(String source, UiMarkupParseOptions options) {

        return parser.parse(source, options);

    }



    public UiMarkupDocument parse(String source, String sourcePath) {

        return parser.parse(source, UiMarkupParseOptions.runtime(), sourcePath);

    }



    public UiMarkupDocument parse(String source, UiMarkupParseOptions options, String sourcePath) {

        return parser.parse(source, options, sourcePath);

    }

}
