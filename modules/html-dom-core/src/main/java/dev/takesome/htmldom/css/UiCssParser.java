package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.internal.parse.UiCssDeclarationParser;
import dev.takesome.htmldom.css.internal.parse.UiCssStylesheetParser;

import java.util.List;
import java.util.Map;

/** Public facade for parsing HtmlDom CSS stylesheets and declaration blocks. */
public final class UiCssParser {
    private final UiCssStylesheetParser stylesheetParser;
    private final UiCssDeclarationParser declarationParser;

    public UiCssParser() {
        this(new UiCssStylesheetParser(), new UiCssDeclarationParser());
    }

    public UiCssParser(
            UiCssStylesheetParser stylesheetParser,
            UiCssDeclarationParser declarationParser
    ) {
        this.stylesheetParser = stylesheetParser == null ? new UiCssStylesheetParser() : stylesheetParser;
        this.declarationParser = declarationParser == null ? new UiCssDeclarationParser() : declarationParser;
    }

    public UiStylesheet parse(String source) {
        return stylesheetParser.parse(source);
    }

    public List<UiCssDeclaration> declarations(String block) {
        return declarationParser.declarations(block);
    }

    public Map<String, String> declarationMap(String block) {
        return declarationParser.declarationMap(block);
    }
}
