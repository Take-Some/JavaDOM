package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.dom.UiDomDocument;

/** Stable host-facing surface handle, similar in spirit to CEF browser/frame handles. */
public interface HtmlDomSurface {
    UiDomDocument document();
    UiCssLayoutResult layoutResult();
    HtmlDomConfig config();
    void ensureLayout();
    void repaintHost();
}
