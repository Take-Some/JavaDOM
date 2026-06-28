package dev.takesome.htmldom.desktop.resource;

import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Namespace-aware resource resolver with checksum/load-time logging and strict
 * unresolved diagnostics. It is intentionally host-agnostic: classpath and file
 * namespaces can be composed per HtmlDom document or embedding application.
 */
public final class HtmlDomResourceResolver {
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(HtmlDomResourceResolver.class);
    private final ClassLoader classLoader;
    private final HtmlDomResourceBundle bundle;

    public HtmlDomResourceResolver(ClassLoader classLoader, HtmlDomResourceBundle bundle) {
        this.classLoader = classLoader == null ? HtmlDomResourceResolver.class.getClassLoader() : classLoader;
        this.bundle = bundle == null ? HtmlDomResourceBundle.forDocument("") : bundle;
    }

    public Optional<HtmlDomResource> resolve(String request, HtmlDomResourceKind kind) {
        return resolve(request, kind, "");
    }

    public Optional<HtmlDomResource> resolve(String request, HtmlDomResourceKind kind, String basePath) {
        String cleanRequest = normalizePath(stripQueryAndFragment(request));
        HtmlDomResourceKind safeKind = kind == null ? HtmlDomResourceKind.BINARY : kind;
        if (cleanRequest.isBlank() || external(cleanRequest) || dataUrl(cleanRequest)) return Optional.empty();
        long started = System.nanoTime();
        ArrayList<String> searched = new ArrayList<>();
        Optional<HtmlDomResource> resolved = namespacedRequest(cleanRequest)
                ? resolveExplicitNamespace(cleanRequest, safeKind, started, searched)
                : resolveOrdered(cleanRequest, safeKind, normalizePath(basePath), started, searched);
        if (resolved.isPresent()) return resolved;
        LOG.warn("HtmlDom resource unresolved kind='{}' request='{}' strict={} searched={}", safeKind, request, bundle.strict(), searched);
        if (bundle.strict()) throw new HtmlDomResourceNotFoundException(safeKind, request, searched);
        return Optional.empty();
    }

    public Optional<String> resolveText(String request, HtmlDomResourceKind kind) {
        return resolve(request, kind).map(HtmlDomResource::textUtf8);
    }

    public Optional<String> resolveText(String request, HtmlDomResourceKind kind, String basePath) {
        return resolve(request, kind, basePath).map(HtmlDomResource::textUtf8);
    }

    public Optional<byte[]> resolveBytes(String request, HtmlDomResourceKind kind) {
        return resolve(request, kind).map(HtmlDomResource::bytes);
    }

    public Optional<byte[]> resolveBytes(String request, HtmlDomResourceKind kind, String basePath) {
        return resolve(request, kind, basePath).map(HtmlDomResource::bytes);
    }

    public String normalizeForCssUrl(String request, String basePath) {
        String clean = normalizePath(stripQueryAndFragment(request));
        if (clean.isBlank() || external(clean) || dataUrl(clean) || absoluteFilePath(clean)) return request == null ? "" : request;
        String base = normalizePath(basePath);
        String resolved = clean.startsWith("/") || base.isBlank() ? clean : normalizePath(base + "/" + clean);
        String absolute = absoluteCssPath(resolved);
        resolve(absolute, HtmlDomResourceKind.IMAGE).ifPresentOrElse(
                resource -> LOG.debug("HtmlDom CSS url verified request='{}' resolved='{}' namespace='{}' checksum='{}'", request, absolute, resource.namespace(), shortChecksum(resource.sha256())),
                () -> LOG.warn("HtmlDom CSS url unresolved request='{}' resolved='{}' base='{}'", request, absolute, base)
        );
        return absolute;
    }

    public HtmlDomResourceBundle bundle() {
        return bundle;
    }

    private Optional<HtmlDomResource> resolveExplicitNamespace(String request, HtmlDomResourceKind kind, long started, List<String> searched) {
        int marker = request.indexOf("://");
        String namespaceId = request.substring(0, marker);
        String resource = request.substring(marker + 3);
        HtmlDomResourceNamespace namespace = bundle.namespace(namespaceId);
        if (namespace == null) {
            searched.add(namespaceId + "://" + resource + " [unknown namespace]");
            return Optional.empty();
        }
        return resolveNamespace(namespace, resource, kind, started, searched);
    }

    private Optional<HtmlDomResource> resolveOrdered(String request, HtmlDomResourceKind kind, String basePath, long started, List<String> searched) {
        LinkedHashSet<String> requests = new LinkedHashSet<>();
        requests.add(request);
        requests.add(HtmlDomResourceNamespace.stripLeadingSlash(request));
        if (!basePath.isBlank() && !HtmlDomResourceNamespace.absoluteLike(request)) requests.add(normalizePath(basePath + "/" + HtmlDomResourceNamespace.stripLeadingSlash(request)));
        if (absoluteFilePath(request) || fileUrl(request)) {
            Optional<HtmlDomResource> direct = readFilePath(request, kind, "file", started, searched);
            if (direct.isPresent()) return direct;
        }
        for (String item : requests) {
            for (HtmlDomResourceNamespace namespace : bundle.namespaces()) {
                Optional<HtmlDomResource> resolved = resolveNamespace(namespace, item, kind, started, searched);
                if (resolved.isPresent()) return resolved;
            }
        }
        return Optional.empty();
    }

    private Optional<HtmlDomResource> resolveNamespace(HtmlDomResourceNamespace namespace, String request, HtmlDomResourceKind kind, long started, List<String> searched) {
        String path = namespace.resolve(request);
        if (path.isBlank()) return Optional.empty();
        if (namespace.filesystemEnabled()) {
            Optional<HtmlDomResource> file = readFilePath(path, kind, namespace.id(), started, searched);
            if (file.isPresent()) return file;
        }
        if (namespace.classpathEnabled()) {
            Optional<HtmlDomResource> cp = readClasspath(path, kind, namespace.id(), started, searched);
            if (cp.isPresent()) return cp;
        }
        return Optional.empty();
    }

    private Optional<HtmlDomResource> readClasspath(String path, HtmlDomResourceKind kind, String namespace, long started, List<String> searched) {
        String resource = HtmlDomResourceNamespace.stripLeadingSlash(path);
        if (resource.isBlank()) return Optional.empty();
        searched.add(namespace + ":classpath:" + resource);
        try (InputStream stream = classLoader.getResourceAsStream(resource)) {
            if (stream == null) return Optional.empty();
            byte[] bytes = stream.readAllBytes();
            return Optional.of(resource(kind, namespace, resource, resource, "classpath:" + resource, bytes, started));
        } catch (IOException error) {
            LOG.error("HtmlDom resource classpath read failed namespace='{}' kind='{}' path='{}'", error, namespace, kind, resource);
            return Optional.empty();
        }
    }

    private Optional<HtmlDomResource> readFilePath(String raw, HtmlDomResourceKind kind, String namespace, long started, List<String> searched) {
        Path path = toPath(raw);
        if (path == null) return Optional.empty();
        searched.add(namespace + ":file:" + path);
        if (!Files.isRegularFile(path)) return Optional.empty();
        try {
            byte[] bytes = Files.readAllBytes(path);
            return Optional.of(resource(kind, namespace, raw, path.toString().replace('\\', '/'), path.toUri().toString(), bytes, started));
        } catch (IOException error) {
            LOG.error("HtmlDom resource file read failed namespace='{}' kind='{}' path='{}'", error, namespace, kind, path);
            return Optional.empty();
        }
    }

    private HtmlDomResource resource(HtmlDomResourceKind kind, String namespace, String request, String resolvedPath, String source, byte[] bytes, long started) {
        String sha = sha256(bytes);
        long elapsed = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
        LOG.info("HtmlDom resource resolved kind='{}' namespace='{}' request='{}' source='{}' bytes={} sha256='{}' elapsedMs={}", kind, namespace, request, source, bytes == null ? 0 : bytes.length, shortChecksum(sha), elapsed);
        return new HtmlDomResource(kind, namespace, request, resolvedPath, source, bytes == null ? new byte[0] : bytes, sha, elapsed);
    }

    private Path toPath(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = normalizePath(raw);
        try {
            if (fileUrl(value)) return Path.of(URI.create(value));
            return Path.of(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes == null ? new byte[0] : bytes));
        } catch (RuntimeException | java.security.NoSuchAlgorithmException error) {
            return "";
        }
    }

    private static String shortChecksum(String sha) {
        return sha == null || sha.length() <= 12 ? (sha == null ? "" : sha) : sha.substring(0, 12);
    }

    private static boolean namespacedRequest(String value) { return value != null && value.contains("://") && !external(value); }
    private static boolean fileUrl(String value) { return value != null && value.toLowerCase(Locale.ROOT).startsWith("file:"); }
    private static boolean dataUrl(String value) { return value != null && value.toLowerCase(Locale.ROOT).startsWith("data:"); }
    private static boolean external(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("jar:");
    }
    private static boolean absoluteFilePath(String value) {
        String normalized = normalizePath(value);
        return normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("//");
    }
    private static String absoluteCssPath(String value) {
        String normalized = normalizePath(value);
        if (normalized.isBlank() || normalized.startsWith("/") || absoluteFilePath(normalized)) return normalized;
        return "/" + HtmlDomResourceNamespace.stripLeadingSlash(normalized);
    }
    private static String normalizePath(String raw) { return raw == null ? "" : raw.trim().replace('\\', '/').replaceAll("/++", "/"); }
    private static String stripQueryAndFragment(String path) {
        if (path == null) return "";
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int cut = -1;
        if (query >= 0) cut = query;
        if (fragment >= 0) cut = cut < 0 ? fragment : Math.min(cut, fragment);
        return cut < 0 ? path : path.substring(0, cut);
    }
}
