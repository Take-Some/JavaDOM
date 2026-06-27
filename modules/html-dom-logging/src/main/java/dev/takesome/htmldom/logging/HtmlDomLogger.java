package dev.takesome.htmldom.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Log4j2 adapter used by HtmlDom modules. */
public final class HtmlDomLogger {
    private final String name;
    private final Logger logger;

    HtmlDomLogger(String name) {
        this.name = name == null || name.isBlank() ? "dev.takesome.htmldom" : name;
        this.logger = LogManager.getLogger(this.name);
    }

    public boolean traceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean debugEnabled() {
        return logger.isDebugEnabled();
    }

    public void trace(String message, Object... args) {
        if (logger.isTraceEnabled()) {
            logger.trace(safe(message), args);
        }
    }

    public void debug(String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(safe(message), args);
        }
    }

    public void info(String message, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(safe(message), args);
        }
    }

    public void warn(String message, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(safe(message), args);
        }
    }

    public void error(String message, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(safe(message), args);
        }
    }

    public void error(String message, Throwable error, Object... args) {
        if (!logger.isErrorEnabled()) {
            return;
        }
        if (error == null) {
            logger.error(safe(message), args);
            return;
        }
        logger.error(safe(message), appendThrowable(args, error));
    }

    private static Object[] appendThrowable(Object[] args, Throwable error) {
        if (args == null || args.length == 0) {
            return new Object[]{error};
        }
        Object[] copy = new Object[args.length + 1];
        System.arraycopy(args, 0, copy, 0, args.length);
        copy[copy.length - 1] = error;
        return copy;
    }

    private static String safe(String message) {
        return message == null ? "" : message;
    }
}
