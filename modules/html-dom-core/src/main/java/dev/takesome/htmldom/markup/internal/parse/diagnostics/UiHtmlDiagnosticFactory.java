package dev.takesome.htmldom.markup.internal.parse.diagnostics;

import dev.takesome.htmldom.html.UiHtmlDiagnosticSink;

/** Centralized diagnostic message factory for HtmlDom HTML parser recovery. */
public final class UiHtmlDiagnosticFactory {
    public void warn(
            UiHtmlDiagnosticSink sink,
            UiHtmlSourceMap sourceMap,
            String code,
            String message,
            int offset,
            int length
    ) {
        UiHtmlSourceMap.UiHtmlSourcePosition position = sourceMap.positionAt(offset);
        sink.warn(code, message, Math.max(0, offset), Math.max(1, length), position.line(), position.column());
    }

    public void unknownTagFallback(UiHtmlDiagnosticSink sink, UiHtmlSourceMap sourceMap, String tagName, String fallback, int offset, int length) {
        warn(sink, sourceMap, "html.unknown-tag-fallback", "Unknown HTML tag <" + tagName + ">; using <" + fallback + "> runtime fallback", offset, length);
    }

    public void unsupportedAttribute(UiHtmlDiagnosticSink sink, UiHtmlSourceMap sourceMap, String tagName, String attribute, int offset, int length) {
        warn(sink, sourceMap, "html.unsupported-attribute", "Unsupported attribute `" + attribute + "` on <" + tagName + ">; kept for compatibility", offset, length);
    }

    public void unexpectedClosingTag(UiHtmlDiagnosticSink sink, UiHtmlSourceMap sourceMap, String tagName, int offset, int length) {
        warn(sink, sourceMap, "html.unexpected-closing-tag", "Unexpected closing tag </" + tagName + ">; ignored", offset, length);
    }

    public void mismatchedClosingTag(UiHtmlDiagnosticSink sink, UiHtmlSourceMap sourceMap, String closingTag, String openTag, int offset, int length) {
        warn(sink, sourceMap, "html.mismatched-closing-tag", "Mismatched closing tag </" + closingTag + "> while <" + openTag + "> was open; auto-closing nested elements", offset, length);
    }

    public void autoClosedTagAtEof(UiHtmlDiagnosticSink sink, UiHtmlSourceMap sourceMap, String tagName, int offset) {
        warn(sink, sourceMap, "html.auto-closed-tag-at-eof", "Unclosed tag <" + tagName + ">; auto-closed at EOF", offset, 1);
    }
}
