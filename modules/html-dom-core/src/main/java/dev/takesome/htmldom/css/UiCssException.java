package dev.takesome.htmldom.css;

/** CSS registry/parser/runtime exception. */
public final class UiCssException extends RuntimeException {
    public UiCssException(String message) {
        super(message);
    }

    public UiCssException(String message, Throwable cause) {
        super(message, cause);
    }
}
