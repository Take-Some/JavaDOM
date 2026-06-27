package dev.takesome.htmldom.support.logging;

/** Backward-compatible facade over the central JANSI-backed HtmlDom logger. */
public final class HtmlDomLog {
    private HtmlDomLog() {
    }

    public static Logger logger(Class<?> type) {
        return new Logger(dev.takesome.htmldom.logging.HtmlDomLog.logger(type));
    }

    public static Logger logger(String name) {
        return new Logger(dev.takesome.htmldom.logging.HtmlDomLog.logger(name));
    }

    public static final class Logger {
        private final dev.takesome.htmldom.logging.HtmlDomLogger delegate;

        private Logger(dev.takesome.htmldom.logging.HtmlDomLogger delegate) {
            this.delegate = delegate;
        }

        public boolean traceEnabled() {
            return delegate.traceEnabled();
        }

        public boolean debugEnabled() {
            return delegate.debugEnabled();
        }

        public void trace(String message, Object... args) {
            delegate.trace(message, args);
        }

        public void debug(String message, Object... args) {
            delegate.debug(message, args);
        }

        public void info(String message, Object... args) {
            delegate.info(message, args);
        }

        public void warn(String message, Object... args) {
            delegate.warn(message, args);
        }

        public void error(String message, Object... args) {
            delegate.error(message, args);
        }

        public void error(String message, Throwable error, Object... args) {
            delegate.error(message, error, args);
        }
    }
}
