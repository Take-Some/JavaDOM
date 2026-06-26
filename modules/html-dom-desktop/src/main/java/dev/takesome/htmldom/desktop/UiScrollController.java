package dev.takesome.htmldom.desktop;

/** @deprecated Use {@link HtmlDomScrollController}. */
@Deprecated(forRemoval = false)
public final class UiScrollController {
    private final HtmlDomScrollController delegate = new HtmlDomScrollController();

    public UiScrollState state() { return delegate.state(); }
    public void sync(dev.takesome.htmldom.css.UiCssLayoutResult layout) { delegate.sync(layout); }
    public dev.takesome.htmldom.css.UiCssScrollBox resolve(dev.takesome.htmldom.dom.UiDomElement element, dev.takesome.htmldom.css.UiCssScrollBox box) { return delegate.resolve(element, box); }
    public boolean scroll(dev.takesome.htmldom.dom.UiDomElement element, dev.takesome.htmldom.css.UiCssScrollBox box, float dx, float dy) { return delegate.scroll(element, box, dx, dy); }
    public boolean set(dev.takesome.htmldom.dom.UiDomElement element, dev.takesome.htmldom.css.UiCssScrollBox box, float x, float y) { return delegate.set(element, box, x, y); }
    public java.util.Map<Integer, dev.takesome.htmldom.css.UiCssScrollBox> resolvedScrollBoxes(dev.takesome.htmldom.css.UiCssLayoutResult layout) { return delegate.resolvedScrollBoxes(layout); }
}
