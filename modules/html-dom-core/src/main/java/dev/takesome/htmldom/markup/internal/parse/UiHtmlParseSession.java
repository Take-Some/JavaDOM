package dev.takesome.htmldom.markup.internal.parse;

import dev.takesome.htmldom.html.UiHtmlDiagnosticSink;
import dev.takesome.htmldom.html.UiHtmlTagRegistry;
import dev.takesome.htmldom.markup.UiMarkupDocument;
import dev.takesome.htmldom.markup.UiMarkupParseOptions;
import dev.takesome.htmldom.markup.internal.parse.diagnostics.UiHtmlDiagnosticFactory;
import dev.takesome.htmldom.markup.internal.parse.diagnostics.UiHtmlSourceMap;
import dev.takesome.htmldom.markup.internal.parse.scanner.UiHtmlNameCanonicalizer;
import dev.takesome.htmldom.markup.internal.parse.syntax.UiHtmlSyntaxProfile;

import java.util.Objects;

/** Parse-local services and immutable parser configuration. */
public final class UiHtmlParseSession {
    private final String source;
    private final String sourcePath;
    private final UiMarkupParseOptions options;
    private final UiHtmlSyntaxProfile syntaxProfile;
    private final UiHtmlDiagnosticFactory diagnosticFactory;
    private final UiHtmlTagRegistry tagRegistry;
    private final UiHtmlDiagnosticSink diagnostics = new UiHtmlDiagnosticSink();
    private final UiHtmlSourceMap sourceMap;
    private final UiHtmlNameCanonicalizer names = new UiHtmlNameCanonicalizer();

    public UiHtmlParseSession(
            String source,
            UiMarkupParseOptions options,
            UiHtmlSyntaxProfile syntaxProfile,
            UiHtmlDiagnosticFactory diagnosticFactory,
            UiHtmlTagRegistry tagRegistry
    ) {
        this(source, "", options, syntaxProfile, diagnosticFactory, tagRegistry);
    }

    public UiHtmlParseSession(
            String source,
            String sourcePath,
            UiMarkupParseOptions options,
            UiHtmlSyntaxProfile syntaxProfile,
            UiHtmlDiagnosticFactory diagnosticFactory,
            UiHtmlTagRegistry tagRegistry
    ) {
        this.source = Objects.requireNonNullElse(source, "");
        this.sourcePath = sourcePath == null ? "" : sourcePath.trim().replace('\\', '/');
        this.options = options == null ? UiMarkupParseOptions.runtime() : options;
        this.syntaxProfile = Objects.requireNonNull(syntaxProfile, "syntaxProfile");
        this.diagnosticFactory = Objects.requireNonNull(diagnosticFactory, "diagnosticFactory");
        this.tagRegistry = Objects.requireNonNull(tagRegistry, "tagRegistry");
        this.sourceMap = new UiHtmlSourceMap(this.source);
    }

    public String source() {
        return source;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public UiMarkupParseOptions options() {
        return options;
    }

    public UiHtmlSyntaxProfile syntax() {
        return syntaxProfile;
    }

    public UiHtmlTagRegistry tags() {
        return tagRegistry;
    }

    public UiHtmlNameCanonicalizer names() {
        return names;
    }

    public void warn(String code, String message, int offset, int length) {
        diagnosticFactory.warn(diagnostics, sourceMap, code, message, offset, length);
    }

    public UiMarkupDocument documentOrThrow(UiMarkupDocument document) {
        if (options.isStrict() && document.hasDiagnostics()) {
            throw strictException(document);
        }
        return document;
    }

    public UiHtmlDiagnosticSink diagnostics() {
        return diagnostics;
    }

    private IllegalArgumentException strictException(UiMarkupDocument document) {
        var first = document.diagnostics().get(0);
        return new IllegalArgumentException(
                "Invalid HtmlDom markup in strict mode: "
                        + first.code()
                        + " at "
                        + first.jumpTarget()
                        + " - "
                        + first.message()
        );
    }
}
