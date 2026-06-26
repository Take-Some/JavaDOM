package dev.takesome.htmldom.logging;

import org.fusesource.jansi.AnsiConsole;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Global HtmlDom logger factory backed by JANSI-capable console output. */
public final class HtmlDomLog {
    private static final Map<String, HtmlDomLogger> LOGGERS = new ConcurrentHashMap<>();
    private static volatile boolean installed;

    private HtmlDomLog() {
    }

    public static HtmlDomLogger logger(Class<?> type) {
        return logger(type == null ? "dev.takesome.htmldom" : type.getName());
    }

    public static HtmlDomLogger logger(String name) {
        installJansi();
        String loggerName = name == null || name.isBlank() ? "dev.takesome.htmldom" : name.trim();
        return LOGGERS.computeIfAbsent(loggerName, HtmlDomLogger::new);
    }

    public static synchronized void installJansi() {
        if (installed) return;
        installed = true;
        if (Boolean.getBoolean("htmldom.jansi.disabled")) return;
        try {
            AnsiConsole.systemInstall();
        } catch (Throwable ignored) {
            // JANSI is a console enhancement. Logging must keep working without it.
        }
    }
}
