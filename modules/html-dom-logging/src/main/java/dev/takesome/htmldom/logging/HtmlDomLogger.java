package dev.takesome.htmldom.logging;

import org.fusesource.jansi.Ansi;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small dependency-light logger for HtmlDom modules. */
public final class HtmlDomLogger {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final String name;

    HtmlDomLogger(String name) {
        this.name = name == null || name.isBlank() ? "dev.takesome.htmldom" : name;
    }

    public void debug(String message, Object... args) {
        if (Boolean.getBoolean("htmldom.debug")) log(Level.DEBUG, message, args);
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
        if (error != null) error.printStackTrace(System.err);
    }

    private void log(Level level, String message, Object... args) {
        String line = "[" + TIME.format(LocalTime.now()) + "] [" + level.label + "] [" + shortName() + "] " + format(message, args);
        if (Boolean.getBoolean("htmldom.log.noColor")) {
            System.err.println(line);
            return;
        }
        try {
            System.err.println(Ansi.ansi().fg(level.color).a(line).reset());
        } catch (Throwable ignored) {
            System.err.println(line);
        }
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
        DEBUG("DEBUG", Ansi.Color.CYAN),
        INFO("INFO", Ansi.Color.GREEN),
        WARN("WARN", Ansi.Color.YELLOW),
        ERROR("ERROR", Ansi.Color.RED);

        private final String label;
        private final Ansi.Color color;

        Level(String label, Ansi.Color color) {
            this.label = label.toUpperCase(Locale.ROOT);
            this.color = color;
        }
    }
}
