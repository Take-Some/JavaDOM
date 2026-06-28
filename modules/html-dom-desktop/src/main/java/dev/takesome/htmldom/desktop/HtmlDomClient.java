package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.desktop.resource.HtmlDomResourceBundle;

/**
 * CEF-inspired host integration boundary. A host application may provide one
 * client instance per HtmlDom surface to customize document resources and
 * observe lifecycle events without depending on internal Swing/runtime classes.
 */
public interface HtmlDomClient {
    HtmlDomClient DEFAULT = new HtmlDomClient() { };

    default HtmlDomResourceBundle configureResources(String sourcePath, String baseResourcePath, HtmlDomResourceBundle defaults) {
        return defaults;
    }

    default void onAfterCreated(HtmlDomSurface surface) {
    }

    default void onBeforeClose(HtmlDomSurface surface) {
    }

    default void onLifecycleEvent(HtmlDomLifecycleEvent event) {
    }
}
