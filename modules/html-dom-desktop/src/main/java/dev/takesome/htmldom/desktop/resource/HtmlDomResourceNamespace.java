package dev.takesome.htmldom.desktop.resource;

import java.util.Objects;

/**
 * A named resource lookup namespace. Namespace IDs make logs and diagnostics
 * explain where a resource was searched instead of only printing a raw path.
 */
public record HtmlDomResourceNamespace(String id, String basePath, boolean classpathEnabled, boolean filesystemEnabled) {
    public HtmlDomResourceNamespace {
        id = normalizeId(id);
        basePath = normalizeBase(basePath);
    }

    public static HtmlDomResourceNamespace classpath(String id, String basePath) {
        return new HtmlDomResourceNamespace(id, basePath, true, false);
    }

    public static HtmlDomResourceNamespace filesystem(String id, String basePath) {
        return new HtmlDomResourceNamespace(id, basePath, false, true);
    }

    public static HtmlDomResourceNamespace hybrid(String id, String basePath) {
        return new HtmlDomResourceNamespace(id, basePath, true, true);
    }

    public String resolve(String resource) {
        String clean = normalizePath(resource);
        if (clean.isBlank()) return "";
        if (absoluteLike(clean) || basePath.isBlank()) return stripLeadingSlash(clean);
        return normalizePath(basePath + "/" + stripLeadingSlash(clean));
    }

    private static String normalizeId(String raw) {
        String value = Objects.requireNonNullElse(raw, "").trim();
        if (value.isBlank()) throw new IllegalArgumentException("resource namespace id must not be blank");
        return value;
    }

    private static String normalizeBase(String raw) {
        String value = normalizePath(raw);
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.trim().replace('\\', '/').replaceAll("/++", "/");
    }

    static String stripLeadingSlash(String raw) {
        String value = normalizePath(raw);
        while (value.startsWith("/")) value = value.substring(1);
        return value;
    }

    static boolean absoluteLike(String raw) {
        String value = normalizePath(raw).toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("file:") || value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:") || value.startsWith("jar:") || value.startsWith("//") || value.matches("^[a-z]:/.*");
    }
}
