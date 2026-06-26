package dev.takesome.htmldom.css.units;

import dev.takesome.htmldom.support.logging.HtmlDomLog;
import dev.takesome.htmldom.support.logging.HtmlDomLog.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class UiCssUnitDiagnostics {
    private static final Logger LOG = HtmlDomLog.logger(UiCssUnitDiagnostics.class);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final Set<String> DEBUGGED = ConcurrentHashMap.newKeySet();

    private UiCssUnitDiagnostics() {
    }

    static void warnOnce(String key, String message, Object... args) {
        if (WARNED.add(key == null ? "unknown" : key)) LOG.warn(message, args);
    }

    static void debugOnce(String key, String message, Object... args) {
        if (DEBUGGED.add(key == null ? "unknown" : key)) LOG.debug(message, args);
    }
}
