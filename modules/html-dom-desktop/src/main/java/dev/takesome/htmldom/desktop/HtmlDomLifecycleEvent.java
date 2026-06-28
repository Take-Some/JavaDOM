package dev.takesome.htmldom.desktop;

/** Immutable lifecycle callback payload for host diagnostics and embedding hooks. */
public record HtmlDomLifecycleEvent(
        long pass,
        HtmlDomLifecyclePhase phase,
        String reason,
        boolean entering,
        int depth,
        String threadName
) {
}
