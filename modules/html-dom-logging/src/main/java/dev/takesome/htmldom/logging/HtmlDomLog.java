package dev.takesome.htmldom.logging;

import org.fusesource.jansi.AnsiConsole;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Global HtmlDom logger factory backed by Log4j2 + JANSI. */
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
        if (installed) {
            return;
        }
        installed = true;
        if (Boolean.getBoolean("htmldom.jansi.disabled")) {
            return;
        }

        setDefaultProperty("log4j2.skipJansi", "false");
        setDefaultProperty("log4j.skipJansi", "false");
        setDefaultProperty("log4j2.enableJansi", "true");
        setDefaultProperty("log4j2.forceAnsi", "true");
        setDefaultProperty("jansi.mode", "force");
        setDefaultProperty("jansi.out.mode", "force");
        setDefaultProperty("jansi.err.mode", "force");
        setDefaultProperty("jansi.passthrough", "true");
        setDefaultProperty("jansi.strip", "false");

        if (Boolean.getBoolean("htmldom.debug") && System.getProperty("htmldom.log.level") == null) {
            System.setProperty("htmldom.log.level", "debug");
        }

        try {
            AnsiConsole.systemInstall();
        } catch (Throwable ignored) {
            // JANSI is a console enhancement. Logging must keep working without it.
        }
    }

    private static void setDefaultProperty(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
