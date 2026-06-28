package dev.takesome.htmldom.desktop.resource;

import java.nio.charset.StandardCharsets;

/** Resolved resource payload plus trace metadata. */
public record HtmlDomResource(
        HtmlDomResourceKind kind,
        String namespace,
        String request,
        String resolvedPath,
        String source,
        byte[] bytes,
        String sha256,
        long elapsedMs
) {
    public String textUtf8() {
        return new String(bytes == null ? new byte[0] : bytes, StandardCharsets.UTF_8);
    }

    public int size() {
        return bytes == null ? 0 : bytes.length;
    }
}
