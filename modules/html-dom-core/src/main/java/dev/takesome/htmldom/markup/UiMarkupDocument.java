package dev.takesome.htmldom.markup;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;
import dev.takesome.htmldom.dom.UiDomText;
import dev.takesome.htmldom.html.UiHtmlDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Parsed HtmlDom markup document. DOM is the canonical source of truth. */
public final class UiMarkupDocument {
    private final UiDomDocument dom;
    private final LegacyRootMode legacyRootMode;
    private final List<UiHtmlDiagnostic> diagnostics;
    private final String sourcePath;
    private volatile UiMarkupNode legacyRoot;
    private volatile long legacyRootVersion = Long.MIN_VALUE;

    public UiMarkupDocument(UiMarkupNode root) {
        this(root, List.of());
    }

    public UiMarkupDocument(UiMarkupNode root, List<UiHtmlDiagnostic> diagnostics) {
        this(root, diagnostics, "");
    }

    public UiMarkupDocument(UiMarkupNode root, List<UiHtmlDiagnostic> diagnostics, String sourcePath) {
        this(toDom(root), legacyMode(root), diagnostics, sourcePath);
    }

    public UiMarkupDocument(UiMarkupNode root, UiDomDocument dom, List<UiHtmlDiagnostic> diagnostics, String sourcePath) {
        this(dom == null ? toDom(root) : dom, legacyMode(root), diagnostics, sourcePath);
        this.legacyRoot = root;
        this.legacyRootVersion = this.dom.version();
    }

    public UiMarkupDocument(UiDomDocument dom, List<UiHtmlDiagnostic> diagnostics, String sourcePath) {
        this(dom, legacyMode(dom), diagnostics, sourcePath);
    }

    public UiMarkupDocument(UiDomDocument dom, LegacyRootMode legacyRootMode, List<UiHtmlDiagnostic> diagnostics, String sourcePath) {
        this.dom = Objects.requireNonNull(dom, "dom");
        this.legacyRootMode = legacyRootMode == null ? LegacyRootMode.DOCUMENT : legacyRootMode;
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        this.sourcePath = sourcePath == null ? "" : sourcePath.trim().replace('\\', '/');
    }

    public UiMarkupNode root() {
        long version = dom.version();
        UiMarkupNode cached = legacyRoot;
        if (cached != null && legacyRootVersion == version) return cached;
        UiMarkupNode rebuilt = toMarkup(legacyRootElement());
        legacyRoot = rebuilt;
        legacyRootVersion = version;
        return rebuilt;
    }

    public UiDomDocument dom() {
        return dom;
    }

    public UiMarkupNode documentElement() {
        return root();
    }

    public Optional<UiMarkupNode> head() {
        UiMarkupNode root = root();
        if ("head".equals(root.tag())) return Optional.of(root);
        return firstChild(root, "head");
    }

    public Optional<UiMarkupNode> body() {
        UiMarkupNode root = root();
        if ("body".equals(root.tag())) return Optional.of(root);
        return firstChild(root, "body");
    }

    public UiMarkupNode renderRoot() {
        return body().orElse(root());
    }

    public List<UiHtmlDiagnostic> diagnostics() {
        return diagnostics;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    private UiDomElement legacyRootElement() {
        return switch (legacyRootMode) {
            case HEAD -> dom.head().orElse(dom.root());
            case BODY -> dom.body().orElse(dom.root());
            case BODY_FIRST_ELEMENT -> firstElementChild(dom.body().orElse(dom.renderRoot())).orElse(dom.renderRoot());
            case DOCUMENT -> dom.documentElement();
        };
    }

    private static UiMarkupNode toMarkup(UiDomElement element) {
        ArrayList<UiMarkupNode> children = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (UiDomNode child : element.children()) {
            if (child instanceof UiDomElement childElement) children.add(toMarkup(childElement));
            else if (child instanceof UiDomText domText) text.append(domText.text());
        }
        return new UiMarkupNode(element.tagName(), element.attributes(), children, text.toString());
    }

    private static UiDomDocument toDom(UiMarkupNode root) {
        Objects.requireNonNull(root, "root");
        UiDomDocument out = new UiDomDocument();
        out.setRoot(toDomDocumentRoot(out, root));
        out.drainMutations();
        out.root().clearDirty();
        return out;
    }

    private static UiDomElement toDomDocumentRoot(UiDomDocument dom, UiMarkupNode root) {
        if ("html".equals(root.tag())) return toDomElement(dom, root);
        UiDomElement html = dom.createElement("html");
        UiMarkupNode head = "head".equals(root.tag()) ? root : firstChild(root, "head").orElse(null);
        UiMarkupNode body = "body".equals(root.tag()) ? root : firstChild(root, "body").orElse(null);
        if (head != null) html.appendChild(toDomElement(dom, head));
        else html.appendChild(dom.createElement("head"));
        if (body != null) html.appendChild(toDomElement(dom, body));
        else if (!"head".equals(root.tag())) {
            UiDomElement bodyElement = dom.createElement("body");
            bodyElement.appendChild(toDomElement(dom, root));
            html.appendChild(bodyElement);
        } else {
            html.appendChild(dom.createElement("body"));
        }
        return html;
    }

    private static UiDomElement toDomElement(UiDomDocument dom, UiMarkupNode source) {
        UiDomElement element = dom.createElement(source.tag());
        source.attributes().forEach(element::setAttribute);
        if (source.text() != null && !source.text().isBlank()) element.appendChild(dom.createText(source.text()));
        for (UiMarkupNode child : source.children()) element.appendChild(toDomElement(dom, child));
        return element;
    }

    private static LegacyRootMode legacyMode(UiMarkupNode root) {
        if (root == null) return LegacyRootMode.DOCUMENT;
        return switch (root.tag()) {
            case "html" -> LegacyRootMode.DOCUMENT;
            case "head" -> LegacyRootMode.HEAD;
            case "body" -> LegacyRootMode.BODY;
            default -> LegacyRootMode.BODY_FIRST_ELEMENT;
        };
    }

    private static LegacyRootMode legacyMode(UiDomDocument dom) {
        if (dom == null || dom.rootOptional().isEmpty()) return LegacyRootMode.DOCUMENT;
        String tag = dom.documentElement().tagName();
        return switch (tag) {
            case "head" -> LegacyRootMode.HEAD;
            case "body" -> LegacyRootMode.BODY;
            case "html" -> LegacyRootMode.DOCUMENT;
            default -> LegacyRootMode.BODY_FIRST_ELEMENT;
        };
    }

    private static Optional<UiMarkupNode> firstChild(UiMarkupNode parent, String tag) {
        if (parent == null || tag == null || tag.isBlank()) return Optional.empty();
        for (UiMarkupNode child : parent.children()) {
            if (tag.equals(child.tag())) return Optional.of(child);
        }
        return Optional.empty();
    }

    private static Optional<UiDomElement> firstElementChild(UiDomElement parent) {
        if (parent == null) return Optional.empty();
        for (UiDomNode child : parent.children()) {
            if (child instanceof UiDomElement element) return Optional.of(element);
        }
        return Optional.empty();
    }

    public enum LegacyRootMode {
        DOCUMENT,
        HEAD,
        BODY,
        BODY_FIRST_ELEMENT
    }
}
