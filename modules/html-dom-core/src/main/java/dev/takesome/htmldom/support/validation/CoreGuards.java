package dev.takesome.htmldom.support.validation;

public final class CoreGuards {
    private CoreGuards() {}
    public static <T> T notNull(T value, String field) { return HtmlDomValidator.notNull(value, field); }
    public static void argument(boolean condition, String message) { HtmlDomValidator.argument(condition, message); }
    public static boolean any(java.util.function.BooleanSupplier... conditions) { return HtmlDomValidator.any(conditions); }
}
