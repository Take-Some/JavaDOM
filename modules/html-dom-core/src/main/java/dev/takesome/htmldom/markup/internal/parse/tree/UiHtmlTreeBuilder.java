package dev.takesome.htmldom.markup.internal.parse.tree;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.markup.UiMarkupDocument;
import dev.takesome.htmldom.markup.internal.parse.UiHtmlParseSession;
import dev.takesome.htmldom.markup.internal.parse.recovery.UiHtmlCloseResolution;
import dev.takesome.htmldom.markup.internal.parse.recovery.UiHtmlRecoveryPolicy;
import dev.takesome.htmldom.markup.internal.parse.scanner.UiHtmlToken;
import dev.takesome.htmldom.markup.internal.parse.scanner.UiHtmlTokenType;
import dev.takesome.htmldom.markup.internal.parse.text.UiHtmlTextDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Token -> UiMarkupDocument tree builder. No CSS, renderer, action or Lua logic. */
public final class UiHtmlTreeBuilder {
    private final UiHtmlParseSession session;
    private final UiHtmlRecoveryPolicy recovery;
    private final UiHtmlStartTagValidator validator;
    private final UiHtmlParseNode document = new UiHtmlParseNode("#document", Map.of());
    private final ArrayList<UiHtmlParseNode> stack = new ArrayList<>();
    private UiHtmlParseNode pendingRawTextNode;
    private String pendingRawTextTag = "";
    private boolean eofAccepted;

    public UiHtmlTreeBuilder(UiHtmlParseSession session) {
        this.session = session;
        this.recovery = new UiHtmlRecoveryPolicy(session.syntax());
        this.validator = new UiHtmlStartTagValidator(session);
        this.stack.add(document);
    }

    public void accept(UiHtmlToken token) {
        if (token == null) {
            return;
        }
        if (token.hasDiagnostic()) {
            session.warn(token.diagnosticCode(), token.diagnosticMessage(), token.offset(), Math.max(1, token.length()));
        }
        if (token.type() == UiHtmlTokenType.TEXT) {
            appendText(token.text());
        } else if (token.type() == UiHtmlTokenType.START_TAG) {
            startTag(token);
        } else if (token.type() == UiHtmlTokenType.END_TAG) {
            closeTag(token);
        } else if (token.type() == UiHtmlTokenType.RAW_TEXT) {
            appendRawText(token);
        } else if (token.type() == UiHtmlTokenType.ERROR) {
            appendText(token.text());
        } else if (token.type() == UiHtmlTokenType.EOF) {
            acceptEof(token);
        }
    }

    public boolean hasPendingRawTextTag() {
        return pendingRawTextNode != null && !pendingRawTextTag.isBlank();
    }

    public String consumePendingRawTextTag() {
        String tag = pendingRawTextTag;
        pendingRawTextTag = "";
        return tag;
    }

    public UiMarkupDocument finish() {
        UiHtmlParseNode root = toDocumentRootNode();
        UiMarkupDocument document = new UiMarkupDocument(
                toDomDocument(root),
                legacyRootMode(root),
                session.diagnostics().snapshot(),
                session.sourcePath()
        );
        return session.documentOrThrow(document);
    }

    private void startTag(UiHtmlToken token) {
        validator.validate(token);
        popOptionalElements(token.name());

        UiHtmlParseNode node = new UiHtmlParseNode(token.name(), token.attributes());
        current().children.add(node);

        if (session.syntax().requiresRawTextRead(token.name()) && !token.selfClosing()) {
            pendingRawTextNode = node;
            pendingRawTextTag = token.name();
            return;
        }
        if (!token.selfClosing() && !session.syntax().isVoidTag(token.name())) {
            stack.add(node);
        }
    }

    private void closeTag(UiHtmlToken token) {
        if (token.name().isBlank()) {
            session.warn("html.invalid-closing-tag", "Invalid closing tag; ignored", token.offset(), Math.max(1, token.length()));
            return;
        }
        UiHtmlCloseResolution close = recovery.resolveClosing(openTagNames(), token.name());
        if (close.ignored()) {
            session.warn("html.unexpected-closing-tag", "Unexpected closing tag </" + token.name() + ">; ignored", token.offset(), Math.max(1, token.length()));
            return;
        }
        if (close.mismatched()) {
            session.warn(
                    "html.mismatched-closing-tag",
                    "Mismatched closing tag </" + token.name() + "> while <" + close.topTag() + "> was open; auto-closing nested elements",
                    token.offset(),
                    Math.max(1, token.length())
            );
        }
        while (stack.size() > close.matchIndex()) {
            stack.remove(stack.size() - 1);
        }
    }

    private void appendRawText(UiHtmlToken token) {
        UiHtmlParseNode target = pendingRawTextNode == null ? current() : pendingRawTextNode;
        target.text.append(token.text());
        pendingRawTextNode = null;
        pendingRawTextTag = "";
    }

    private void acceptEof(UiHtmlToken token) {
        if (eofAccepted) {
            return;
        }
        eofAccepted = true;
        for (int i = stack.size() - 1; i >= 1; i--) {
            session.warn("html.auto-closed-tag-at-eof", "Unclosed tag <" + stack.get(i).tag + ">; auto-closed at EOF", token.offset(), 1);
        }
    }

    private void popOptionalElements(String nextTag) {
        int count = recovery.optionalAutoCloseCount(openTagNames(), nextTag);
        for (int i = 0; i < count && stack.size() > 1; i++) {
            stack.remove(stack.size() - 1);
        }
    }

    private UiHtmlParseNode toDocumentRootNode() {
        if (document.children.size() == 1 && UiHtmlTextDecoder.collapseWhitespace(document.text.toString()).isBlank()) {
            return document.children.get(0);
        }
        if (hasDocumentScaffold(document.children)) {
            return new UiHtmlParseNode("html", Map.of(), document.children, document.text);
        }
        return new UiHtmlParseNode("body", Map.of(), document.children, document.text);
    }

    private boolean hasDocumentScaffold(List<UiHtmlParseNode> children) {
        if (children == null || children.isEmpty()) {
            return false;
        }
        for (UiHtmlParseNode child : children) {
            if ("head".equals(child.tag) || "body".equals(child.tag) || "html".equals(child.tag)) {
                return true;
            }
        }
        return false;
    }

    private UiDomDocument toDomDocument(UiHtmlParseNode source) {
        UiDomDocument dom = new UiDomDocument();
        dom.setRoot(toDomDocumentRoot(dom, source));
        dom.drainMutations();
        dom.root().clearDirty();
        return dom;
    }

    private UiDomElement toDomDocumentRoot(UiDomDocument dom, UiHtmlParseNode source) {
        if ("html".equals(source.tag)) return toDom(dom, source);

        UiDomElement html = dom.createElement("html");
        UiHtmlParseNode head = headNode(source);
        UiHtmlParseNode body = bodyNode(source);

        if (head != null) html.appendChild(toDom(dom, head));
        else html.appendChild(dom.createElement("head"));

        if (body != null) {
            html.appendChild(toDom(dom, body));
        } else if (!"head".equals(source.tag)) {
            UiDomElement bodyElement = dom.createElement("body");
            bodyElement.appendChild(toDom(dom, source));
            html.appendChild(bodyElement);
        } else {
            html.appendChild(dom.createElement("body"));
        }
        return html;
    }

    private UiHtmlParseNode headNode(UiHtmlParseNode source) {
        if (source == null) return null;
        if ("head".equals(source.tag)) return source;
        return firstChild(source, "head");
    }

    private UiHtmlParseNode bodyNode(UiHtmlParseNode source) {
        if (source == null) return null;
        if ("body".equals(source.tag)) return source;
        return firstChild(source, "body");
    }

    private UiHtmlParseNode firstChild(UiHtmlParseNode source, String tag) {
        if (source == null || tag == null || tag.isBlank()) return null;
        for (UiHtmlParseNode child : source.children) {
            if (tag.equals(child.tag)) return child;
        }
        return null;
    }

    private UiDomElement toDom(UiDomDocument dom, UiHtmlParseNode source) {
        UiDomElement element = dom.createElement(source.tag);
        source.attributes.forEach(element::setAttribute);
        String text = UiHtmlTextDecoder.textFor(source.tag, source.text.toString(), session.syntax());
        if (text != null && !text.isBlank()) element.appendChild(dom.createText(text));
        for (UiHtmlParseNode child : source.children) element.appendChild(toDom(dom, child));
        return element;
    }

    private UiMarkupDocument.LegacyRootMode legacyRootMode(UiHtmlParseNode root) {
        if (root == null) return UiMarkupDocument.LegacyRootMode.DOCUMENT;
        return switch (root.tag) {
            case "html" -> UiMarkupDocument.LegacyRootMode.DOCUMENT;
            case "head" -> UiMarkupDocument.LegacyRootMode.HEAD;
            case "body" -> UiMarkupDocument.LegacyRootMode.BODY;
            default -> UiMarkupDocument.LegacyRootMode.BODY_FIRST_ELEMENT;
        };
    }

    private void appendText(String text) {
        if (text != null && !text.isEmpty()) {
            current().text.append(text);
        }
    }

    private UiHtmlParseNode current() {
        return stack.get(stack.size() - 1);
    }

    private List<String> openTagNames() {
        ArrayList<String> names = new ArrayList<>(stack.size());
        for (UiHtmlParseNode node : stack) {
            names.add(node.tag);
        }
        return names;
    }
}
