package dev.takesome.htmldom.logging;

import org.fusesource.jansi.Ansi;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small dependency-light logger for HtmlDom modules. */
public final class HtmlDomLogger {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String LOG_LEVEL_PROPERTY = "htmldom.log.level";
    private static final String LEGACY_DEBUG_PROPERTY = "htmldom.debug";
    private final String name;

    HtmlDomLogger(String name) {
        this.name = name == null || name.isBlank() ? "dev.takesome.htmldom" : name;
    }

    public boolean traceEnabled() {
        return enabled(Level.TRACE);
    }

    public boolean debugEnabled() {
        return enabled(Level.DEBUG);
    }

    public void trace(String message, Object... args) {
        log(Level.TRACE, message, args);
    }

    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    public void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    public void error(String message, Throwable error, Object... args) {
        log(Level.ERROR, message, args);
        if (error != null) {
            printThrowable(error);
        }
    }

    private void log(Level level, String message, Object... args) {
        if (!enabled(level)) return;
        String line = "[" + TIME.format(LocalTime.now()) + "] [" + level.label + "] [" + Thread.currentThread().getName() + "] [" + shortName() + "] " + format(message, args);
        if (noColor()) {
            System.err.println(line);
            return;
        }
        try {
            System.err.println(Ansi.ansi().fg(level.color).a(line).reset());
        } catch (Throwable ignored) {
            System.err.println(line);
        }
    }

    private void printThrowable(Throwable error) {
        if (noColor()) {
            error.printStackTrace(System.err);
            return;
        }
        try {
            System.err.println(Ansi.ansi().fg(Ansi.Color.RED).a(error.getClass().getName()).a(": ").a(error.getMessage()).reset());
            for (StackTraceElement frame : error.getStackTrace()) {
                System.err.println(Ansi.ansi().fg(Ansi.Color.RED).a("    at ").a(frame.toString()).reset());
            }
            Throwable cause = error.getCause();
            if (cause != null && cause != error) {
                System.err.println(Ansi.ansi().fg(Ansi.Color.RED).a("Caused by: ").a(cause.getClass().getName()).a(": ").a(cause.getMessage()).reset());
                for (StackTraceElement frame : cause.getStackTrace()) {
                    System.err.println(Ansi.ansi().fg(Ansi.Color.RED).a("    at ").a(frame.toString()).reset());
                }
            }
        } catch (Throwable ignored) {
            error.printStackTrace(System.err);
        }
    }

    private boolean enabled(Level level) {
        return level.priority >= threshold().priority;
    }

    private Level threshold() {
        String configured = System.getProperty(LOG_LEVEL_PROPERTY, "");
        if (configured.isBlank() && Boolean.getBoolean(LEGACY_DEBUG_PROPERTY)) return Level.DEBUG;
        return Level.from(configured, Level.INFO);
    }

    private boolean noColor() {
        return Boolean.getBoolean("htmldom.log.noColor") || Boolean.getBoolean("htmldom.noColor");
    }

    private String shortName() {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private static String format(String message, Object... args) {
        String template = message == null ? "" : message;
        if (args == null || args.length == 0) return template;
        StringBuilder out = new StringBuilder(template.length() + args.length * 12);
        int argIndex = 0;
        for (int i = 0; i < template.length(); i++) {
            if (i + 1 < template.length() && template.charAt(i) == '{' && template.charAt(i + 1) == '}' && argIndex < args.length) {
                out.append(String.valueOf(args[argIndex++]));
                i++;
            } else {
                out.append(template.charAt(i));
            }
        }
        while (argIndex < args.length) out.append(' ').append(String.valueOf(args[argIndex++]));
        return out.toString();
    }

    private enum Level {
        TRACE("TRACE", Ansi.Color.MAGENTA, 10),
        DEBUG("DEBUG", Ansi.Color.CYAN, 20),
        INFO("INFO", Ansi.Color.GREEN, 30),
        WARN("WARN", Ansi.Color.YELLOW, 40),
        ERROR("ERROR", Ansi.Color.RED, 50);

        private final String label;
        private final Ansi.Color color;
        private final int priority;

        Level(String label, Ansi.Color color, int priority) {
            this.label = label.toUpperCase(Locale.ROOT);
            this.color = color;
            this.priority = priority;
        }

        private static Level from(String value, Level fallback) {
            if (value == null || value.isBlank()) return fallback;
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            for (Level level : values()) {
                if (level.label.equals(normalized)) return level;
            }
            return fallback;
        }
    }
}
