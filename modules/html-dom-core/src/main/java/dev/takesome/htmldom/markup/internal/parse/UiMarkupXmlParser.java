package dev.takesome.htmldom.markup.internal.parse;

import dev.takesome.htmldom.html.UiHtmlTagRegistry;
import dev.takesome.htmldom.markup.UiMarkupDocument;
import dev.takesome.htmldom.markup.UiMarkupParseOptions;
import dev.takesome.htmldom.markup.internal.parse.diagnostics.UiHtmlDiagnosticFactory;
import dev.takesome.htmldom.markup.internal.parse.scanner.UiHtmlScanner;
import dev.takesome.htmldom.markup.internal.parse.scanner.UiHtmlToken;
import dev.takesome.htmldom.markup.internal.parse.scanner.UiHtmlTokenType;
import dev.takesome.htmldom.markup.internal.parse.syntax.UiHtmlSyntaxProfile;
import dev.takesome.htmldom.markup.internal.parse.tree.UiHtmlTreeBuilder;

import java.util.Objects;

/**
 * Tolerant HTML-like parser facade for HtmlDom Markup.
 *
 * <p>The facade only orchestrates scanner -> tree builder -> document.
 * Tokenization, recovery and diagnostic creation live in dedicated classes.</p>
 */
public final class UiMarkupXmlParser {
    private final UiHtmlSyntaxProfile syntaxProfile;
    private final UiHtmlDiagnosticFactory diagnosticFactory;
    private final UiHtmlTagRegistry tagRegistry;

    public UiMarkupXmlParser() {
        this(
                UiHtmlSyntaxProfile.helixDefault(),
                new UiHtmlDiagnosticFactory(),
                UiHtmlTagRegistry.loadBuiltins()
        );
    }

    public UiMarkupXmlParser(
            UiHtmlSyntaxProfile syntaxProfile,
            UiHtmlDiagnosticFactory diagnosticFactory,
            UiHtmlTagRegistry tagRegistry
    ) {
        this.syntaxProfile = Objects.requireNonNull(syntaxProfile, "syntaxProfile");
        this.diagnosticFactory = Objects.requireNonNull(diagnosticFactory, "diagnosticFactory");
        this.tagRegistry = Objects.requireNonNull(tagRegistry, "tagRegistry");
    }

    public UiMarkupDocument parse(String source) {
        return parse(source, UiMarkupParseOptions.runtime());
    }

    public UiMarkupDocument parse(String source, UiMarkupParseOptions options) {
        return parse(source, options, "");
    }

    public UiMarkupDocument parse(String source, UiMarkupParseOptions options, String sourcePath) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("markup source must not be blank");
        }

        UiHtmlParseSession session = new UiHtmlParseSession(
                source,
                sourcePath,
                options == null ? UiMarkupParseOptions.runtime() : options,
                syntaxProfile,
                diagnosticFactory,
                tagRegistry
        );
        UiHtmlScanner scanner = new UiHtmlScanner(session);
        UiHtmlTreeBuilder builder = new UiHtmlTreeBuilder(session);

        while (true) {
            UiHtmlToken token = scanner.nextToken();
            builder.accept(token);

            if (builder.hasPendingRawTextTag()) {
                builder.accept(scanner.readRawText(builder.consumePendingRawTextTag()));
            }
            if (token.type() == UiHtmlTokenType.EOF) {
                break;
            }
        }

        return builder.finish();
    }
}
