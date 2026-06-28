package dev.takesome.htmldom.desktop.resource;

import java.util.List;

public final class HtmlDomResourceNotFoundException extends RuntimeException {
    private final HtmlDomResourceKind kind;
    private final String request;
    private final List<String> searched;

    public HtmlDomResourceNotFoundException(HtmlDomResourceKind kind, String request, List<String> searched) {
        super("HtmlDom resource unresolved kind='" + kind + "' request='" + request + "' searched=" + searched);
        this.kind = kind;
        this.request = request;
        this.searched = searched == null ? List.of() : List.copyOf(searched);
    }

    public HtmlDomResourceKind kind() { return kind; }
    public String request() { return request; }
    public List<String> searched() { return searched; }
}
