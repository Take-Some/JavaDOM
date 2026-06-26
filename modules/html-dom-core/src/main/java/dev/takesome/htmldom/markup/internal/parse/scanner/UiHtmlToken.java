package dev.takesome.htmldom.markup.internal.parse.scanner;

import java.util.Map;

/** Scanner token with source offsets. It does not own DOM/tree state. */
public record UiHtmlToken(
        UiHtmlTokenType type,
        String name,
        String text,
        Map<String, String> attributes,
        boolean selfClosing,
        String diagnosticCode,
        String diagnosticMessage,
        int offset,
        int length
) {
    public UiHtmlToken {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        name = name == null ? "" : name;
        text = text == null ? "" : text;
        diagnosticCode = diagnosticCode == null ? "" : diagnosticCode;
        diagnosticMessage = diagnosticMessage == null ? "" : diagnosticMessage;
        offset = Math.max(0, offset);
        length = Math.max(0, length);
    }

    public static UiHtmlToken text(String value, int offset, int length) {
        return new UiHtmlToken(UiHtmlTokenType.TEXT, "", value, Map.of(), false, "", "", offset, length);
    }

    public static UiHtmlToken startTag(String name, Map<String, String> attributes, boolean selfClosing, int offset, int length) {
        return new UiHtmlToken(UiHtmlTokenType.START_TAG, name, "", attributes, selfClosing, "", "", offset, length);
    }

    public static UiHtmlToken endTag(String name, int offset, int length) {
        return new UiHtmlToken(UiHtmlTokenType.END_TAG, name, "", Map.of(), false, "", "", offset, length);
    }

    public static UiHtmlToken rawText(String tagName, String value, int offset, int length) {
        return new UiHtmlToken(UiHtmlTokenType.RAW_TEXT, tagName, value, Map.of(), false, "", "", offset, length);
    }

    public static UiHtmlToken rawTextError(String tagName, String value, String code, String message, int offset, int length) {
        return new UiHtmlToken(UiHtmlTokenType.RAW_TEXT, tagName, value, Map.of(), false, code, message, offset, length);
    }

    public static UiHtmlToken error(String code, String message, String text, int offset, int length) {
        return new UiHtmlToken(UiHtmlTokenType.ERROR, "", text, Map.of(), false, code, message, offset, length);
    }

    public static UiHtmlToken eof(int offset) {
        return new UiHtmlToken(UiHtmlTokenType.EOF, "", "", Map.of(), false, "", "", offset, 0);
    }

    public boolean hasDiagnostic() {
        return !diagnosticCode.isBlank();
    }
}
