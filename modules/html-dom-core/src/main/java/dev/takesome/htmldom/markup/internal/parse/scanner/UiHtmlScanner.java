package dev.takesome.htmldom.markup.internal.parse.scanner;

import dev.takesome.htmldom.markup.internal.parse.UiHtmlParseSession;

/** Character-level scanner. Produces tokens; does not build markup nodes. */
public final class UiHtmlScanner {
    private final UiHtmlParseSession session;
    private final UiHtmlSourceCursor cursor;
    private final UiHtmlNameCanonicalizer names;
    private final UiHtmlAttributeScanner attributes;

    public UiHtmlScanner(UiHtmlParseSession session) {
        this.session = session;
        this.cursor = new UiHtmlSourceCursor(session.source());
        this.names = session.names();
        this.attributes = new UiHtmlAttributeScanner(names);
    }

    public UiHtmlToken nextToken() {
        if (cursor.eof()) {
            return UiHtmlToken.eof(cursor.offset());
        }
        if (cursor.peek() != '<') {
            return readText();
        }
        if (cursor.startsWith("<!--")) {
            return skipComment();
        }
        if (cursor.startsWith("<!")) {
            skipDeclaration();
            return nextToken();
        }
        if (cursor.startsWith("<?")) {
            skipProcessingInstruction();
            return nextToken();
        }
        if (cursor.startsWith("</")) {
            return readEndTag();
        }
        return readStartTag();
    }

    public UiHtmlToken readRawText(String tagName) {
        String name = names.canonical(tagName);
        int contentStart = cursor.offset();
        String closing = "</" + name;
        int close = cursor.indexOfIgnoreCase(closing, contentStart);
        if (close < 0) {
            String raw = cursor.slice(contentStart, cursor.length());
            cursor.moveToEnd();
            return UiHtmlToken.rawTextError(
                    name,
                    raw,
                    "html.auto-closed-tag-at-eof",
                    "Unclosed raw-text tag <" + name + ">; auto-closed at EOF",
                    contentStart,
                    cursor.length() - contentStart
            );
        }

        int closeEnd = UiHtmlTagSyntax.findTagEnd(cursor.source(), close + closing.length());
        if (closeEnd < 0) {
            String raw = cursor.slice(contentStart, close);
            cursor.moveToEnd();
            return UiHtmlToken.rawTextError(
                    name,
                    raw,
                    "html.malformed-closing-tag",
                    "Malformed raw-text closing tag </" + name + ">; auto-closed at EOF",
                    close,
                    Math.max(1, cursor.length() - close)
            );
        }

        String raw = cursor.slice(contentStart, close);
        cursor.moveTo(closeEnd + 1);
        return UiHtmlToken.rawText(name, raw, contentStart, closeEnd + 1 - contentStart);
    }

    private UiHtmlToken readText() {
        int start = cursor.offset();
        int lt = cursor.indexOf("<", start);
        int end = lt < 0 ? cursor.length() : lt;
        String text = cursor.slice(start, end);
        cursor.moveTo(end);
        return UiHtmlToken.text(text, start, end - start);
    }

    private UiHtmlToken skipComment() {
        int start = cursor.offset();
        int end = cursor.indexOf("-->", start + 4);
        if (end < 0) {
            cursor.moveToEnd();
            return UiHtmlToken.error(
                    "html.unclosed-comment",
                    "Unclosed HTML comment; ignored until EOF",
                    "",
                    start,
                    cursor.length() - start
            );
        }
        cursor.moveTo(end + 3);
        return nextToken();
    }

    private void skipDeclaration() {
        int start = cursor.offset();
        int end = cursor.indexOf(">", start + 2);
        cursor.moveTo(end < 0 ? cursor.length() : end + 1);
    }

    private void skipProcessingInstruction() {
        int start = cursor.offset();
        int end = cursor.indexOf("?>", start + 2);
        cursor.moveTo(end < 0 ? cursor.length() : end + 2);
    }

    private UiHtmlToken readEndTag() {
        int start = cursor.offset();
        cursor.moveTo(start + 2);
        int end = UiHtmlTagSyntax.findTagEnd(cursor.source(), cursor.offset());
        if (end < 0) {
            String text = cursor.slice(start, cursor.length());
            cursor.moveToEnd();
            return UiHtmlToken.error(
                    "html.malformed-closing-tag",
                    "Malformed closing tag; treated as text",
                    text,
                    start,
                    cursor.length() - start
            );
        }
        String rawName = cursor.slice(cursor.offset(), end);
        String name = UiHtmlTagSyntax.closingName(rawName, names);
        cursor.moveTo(end + 1);
        return UiHtmlToken.endTag(name, start, end + 1 - start);
    }

    private UiHtmlToken readStartTag() {
        int start = cursor.offset();
        cursor.moveTo(start + 1);
        int end = UiHtmlTagSyntax.findTagEnd(cursor.source(), cursor.offset());
        if (end < 0) {
            String text = cursor.slice(start, cursor.length());
            cursor.moveToEnd();
            return UiHtmlToken.error(
                    "html.malformed-start-tag",
                    "Malformed start tag; treated as text",
                    text,
                    start,
                    cursor.length() - start
            );
        }

        String raw = cursor.slice(cursor.offset(), end);
        UiHtmlStartTagData parsed = attributes.parseStartTag(raw);
        cursor.moveTo(end + 1);
        if (parsed == null) {
            return UiHtmlToken.error(
                    "html.invalid-start-tag",
                    "Invalid start tag; treated as text",
                    cursor.slice(start, end + 1),
                    start,
                    end + 1 - start
            );
        }
        return UiHtmlToken.startTag(parsed.name(), parsed.attributes(), parsed.selfClosing(), start, end + 1 - start);
    }
}
