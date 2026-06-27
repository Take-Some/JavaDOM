package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssParser;
import dev.takesome.htmldom.css.UiStylesheet;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;
import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Resolves author styles from constructor CSS, <style> blocks and <link rel="stylesheet"> tags. */
final class HtmlDomStylesheetLoader {
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(HtmlDomStylesheetLoader.class);
    private final UiCssParser parser = new UiCssParser();
    private final ClassLoader classLoader;

    HtmlDomStylesheetLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? HtmlDomStylesheetLoader.class.getClassLoader() : classLoader;
    }

    UiStylesheet load(UiDomDocument document, String explicitCss, String sourcePath, String baseResourcePath) {
        UiStylesheet stylesheet = UiStylesheet.empty();
        LoadStats stats = new LoadStats();
        String source = safeSource(sourcePath);
        String base = normalizeBase(baseResourcePath, source);

        if (explicitCss != null && !explicitCss.isBlank()) {
            stylesheet = stylesheet.plus(parseChunk("constructor", source + "#constructor-css", explicitCss, base));
            stats.explicitBlocks++;
        }

        if (document != null && document.rootOptional().isPresent()) {
            for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
                if ("style".equals(element.tagName())) {
                    String css = element.textContent();
                    if (css == null || css.isBlank()) {
                        LOG.warn("UI CSS inline <style> is empty source='{}' element='{}'", source, selector(element));
                        stats.skippedInlineBlocks++;
                        continue;
                    }
                    stylesheet = stylesheet.plus(parseChunk("inline-style", source + "#" + selector(element), css, base));
                    stats.inlineBlocks++;
                    continue;
                }
                if ("link".equals(element.tagName())) {
                    stats.linkNodes++;
                    LinkDecision decision = classifyLink(element);
                    if (!decision.shouldLoad()) {
                        LOG.debug("UI CSS link skipped source='{}' element='{}' reason='{}' rel='{}' href='{}' type='{}' media='{}'",
                                source, selector(element), decision.reason(), element.attribute("rel", ""), element.attribute("href", ""), element.attribute("type", ""), element.attribute("media", ""));
                        stats.skippedLinks++;
                        continue;
                    }
                    String href = element.attribute("href", "");
                    String resolved = resolveHref(href, base);
                    LOG.info("UI CSS link discovered source='{}' href='{}' resolved='{}' media='{}'", source, href, resolved, element.attribute("media", ""));
                    String css = readStylesheet(resolved);
                    if (css == null) {
                        LOG.warn("UI CSS link failed source='{}' href='{}' resolved='{}'", source, href, resolved);
                        stats.failedLinks++;
                        continue;
                    }
                    stylesheet = stylesheet.plus(parseChunk("link", resolved, css, normalizeBase("", resolved)));
                    stats.loadedLinks++;
                }
            }
        }

        LOG.info("UI CSS resolved source='{}' base='{}' explicitBlocks={} inlineBlocks={} skippedInlineBlocks={} linkNodes={} loadedLinks={} skippedLinks={} failedLinks={} rules={} fontFaces={} keyframes={}",
                source,
                base,
                stats.explicitBlocks,
                stats.inlineBlocks,
                stats.skippedInlineBlocks,
                stats.linkNodes,
                stats.loadedLinks,
                stats.skippedLinks,
                stats.failedLinks,
                stylesheet.rules().size(),
                stylesheet.fontFaces().size(),
                stylesheet.keyframes().size());
        return stylesheet;
    }

    private UiStylesheet parseChunk(String kind, String name, String css, String urlBase) {
        String safeName = name == null || name.isBlank() ? "<inline>" : name;
        long started = System.nanoTime();
        String normalizedCss = resolveCssUrls(css == null ? "" : css, urlBase);
        try {
            UiStylesheet parsed = parser.parse(normalizedCss);
            LOG.info("UI CSS parsed kind='{}' source='{}' chars={} rules={} fontFaces={} keyframes={} elapsedMs={}",
                    kind,
                    safeName,
                    normalizedCss.length(),
                    parsed.rules().size(),
                    parsed.fontFaces().size(),
                    parsed.keyframes().size(),
                    elapsedMs(started));
            warnAboutUnsupportedImports(kind, safeName, normalizedCss);
            return parsed;
        } catch (RuntimeException error) {
            LOG.error("UI CSS parse failed kind='{}' source='{}' chars={}", error, kind, safeName, normalizedCss.length());
            return UiStylesheet.empty();
        }
    }

    private String resolveCssUrls(String css, String base) {
        if (css == null || css.isBlank() || !css.toLowerCase(Locale.ROOT).contains("url(")) return css == null ? "" : css;
        StringBuilder out = new StringBuilder(css.length() + 32);
        int index = 0;
        while (index < css.length()) {
            int start = indexOfUrl(css, index);
            if (start < 0) {
                out.append(css, index, css.length());
                break;
            }
            int bodyStart = start + 4;
            int bodyEnd = css.indexOf(')', bodyStart);
            if (bodyEnd < 0) {
                out.append(css, index, css.length());
                break;
            }
            out.append(css, index, bodyStart);
            String original = css.substring(bodyStart, bodyEnd);
            out.append(resolveCssUrlToken(original, base));
            index = bodyEnd;
        }
        return out.toString();
    }

    private int indexOfUrl(String css, int fromIndex) {
        String lower = css.toLowerCase(Locale.ROOT);
        return lower.indexOf("url(", Math.max(0, fromIndex));
    }

    private String resolveCssUrlToken(String token, String base) {
        String raw = token == null ? "" : token.trim();
        if (raw.isBlank()) return token == null ? "" : token;
        char quote = 0;
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            quote = raw.charAt(0);
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        String resolved = resolveCssAssetUrl(raw, base);
        if (quote == 0) return resolved;
        return quote + resolved + quote;
    }

    private String resolveCssAssetUrl(String url, String base) {
        String clean = normalizePath(stripQueryAndFragment(url));
        if (clean.isBlank() || externalUrl(clean) || dataUrl(clean) || absoluteFilePath(clean)) return url;
        if (clean.startsWith("/")) return clean;
        if (base == null || base.isBlank()) return clean;
        String resolved = normalizePath(base + "/" + clean);
        String absoluteResource = absoluteResourceUrl(resolved);
        LOG.debug("UI CSS url resolved base='{}' url='{}' resolved='{}'", base, url, absoluteResource);
        return absoluteResource;
    }

    private String absoluteResourceUrl(String value) {
        String normalized = normalizePath(value);
        if (normalized.isBlank() || absoluteFilePath(normalized) || normalized.startsWith("/")) return normalized;
        return "/" + stripLeadingSlash(normalized);
    }

    private boolean resourceExists(String value) {
        String resource = stripLeadingSlash(stripQueryAndFragment(value));
        if (resource.isBlank()) return false;
        try (InputStream stream = classLoader.getResourceAsStream(resource)) {
            return stream != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean dataUrl(String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).startsWith("data:");
    }

    private boolean absoluteFilePath(String value) {
        if (value == null || value.isBlank()) return false;
        String normalized = value.trim().replace('\\', '/');
        return normalized.startsWith("//") || normalized.matches("^[A-Za-z]:/.*");
    }

    private void warnAboutUnsupportedImports(String kind, String name, String css) {
        if (css == null) return;
        if (css.toLowerCase(Locale.ROOT).contains("@import")) {
            LOG.warn("UI CSS @import is not expanded yet kind='{}' source='{}'; use <link rel=\"stylesheet\"> for deterministic desktop loading", kind, name);
        }
    }

    private String readStylesheet(String resolved) {
        if (resolved == null || resolved.isBlank()) return null;
        if (externalUrl(resolved)) {
            LOG.warn("UI CSS external URL is not supported in desktop runtime url='{}'", resolved);
            return null;
        }
        String resource = stripLeadingSlash(stripQueryAndFragment(resolved));
        try (InputStream stream = classLoader.getResourceAsStream(resource)) {
            if (stream != null) {
                byte[] bytes = stream.readAllBytes();
                LOG.info("UI CSS resource loaded resource='{}' bytes={}", resource, bytes.length);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException error) {
            LOG.error("UI CSS resource read failed resource='{}'", error, resource);
            return null;
        }

        Path path = Path.of(resource);
        if (Files.isRegularFile(path)) {
            try {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                LOG.info("UI CSS file loaded path='{}' chars={}", path, text.length());
                return text;
            } catch (IOException error) {
                LOG.error("UI CSS file read failed path='{}'", error, path);
            }
        }
        LOG.warn("UI CSS resource not found resource='{}'", resource);
        return null;
    }

    private LinkDecision classifyLink(UiDomElement element) {
        if (element == null) return LinkDecision.skip("missing-element");
        if (element.hasAttribute("disabled")) return LinkDecision.skip("disabled");
        String rel = element.attribute("rel", "");
        if (!hasRelToken(rel, "stylesheet")) return LinkDecision.skip("rel-not-stylesheet");
        String href = element.attribute("href", "");
        if (href.isBlank()) return LinkDecision.skip("missing-href");
        String type = element.attribute("type", "").trim().toLowerCase(Locale.ROOT);
        if (!type.isBlank() && !"text/css".equals(type)) return LinkDecision.skip("type-not-css");
        String media = element.attribute("media", "");
        if (!mediaAccepted(media)) return LinkDecision.skip("media-not-screen");
        return LinkDecision.loaded();
    }

    private boolean hasRelToken(String rel, String expected) {
        if (rel == null || expected == null || expected.isBlank()) return false;
        for (String token : rel.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (expected.equals(token.trim())) return true;
        }
        return false;
    }

    private boolean mediaAccepted(String media) {
        if (media == null || media.isBlank()) return true;
        String normalized = media.toLowerCase(Locale.ROOT);
        for (String token : normalized.split(",")) {
            String item = token.trim();
            if (item.isBlank() || "all".equals(item) || "screen".equals(item) || item.startsWith("screen ") || item.contains(" screen")) {
                return true;
            }
        }
        return false;
    }

    private String resolveHref(String href, String base) {
        String cleanHref = normalizePath(stripQueryAndFragment(href));
        if (cleanHref.isBlank() || externalUrl(cleanHref)) return cleanHref;
        if (cleanHref.startsWith("/")) return cleanHref;
        if (base == null || base.isBlank()) return cleanHref;
        return normalizePath(base + "/" + cleanHref);
    }

    private String normalizeBase(String explicitBase, String sourcePath) {
        String base = normalizePath(explicitBase);
        if (!base.isBlank()) return trimTrailingSlash(base);
        String source = normalizePath(sourcePath);
        int slash = source.lastIndexOf('/');
        return slash < 0 ? "" : source.substring(0, slash);
    }

    private String safeSource(String sourcePath) {
        String source = normalizePath(sourcePath);
        return source.isBlank() ? "desktop.ui.html" : source;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "";
        return path.trim().replace('\\', '/').replaceAll("/++", "/");
    }

    private String stripQueryAndFragment(String path) {
        if (path == null) return "";
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int cut = -1;
        if (query >= 0) cut = query;
        if (fragment >= 0) cut = cut < 0 ? fragment : Math.min(cut, fragment);
        return cut < 0 ? path : path.substring(0, cut);
    }

    private String stripLeadingSlash(String path) {
        String value = path == null ? "" : path.trim().replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        return value;
    }

    private String trimTrailingSlash(String path) {
        String value = path == null ? "" : path.trim().replace('\\', '/');
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private boolean externalUrl(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("data:")
                || normalized.startsWith("file:")
                || normalized.startsWith("jar:");
    }

    private long elapsedMs(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private String selector(UiDomElement element) {
        if (element == null) return "";
        StringBuilder out = new StringBuilder(element.tagName());
        if (!element.id().isBlank()) out.append('#').append(element.id());
        for (String className : element.classList().values()) out.append('.').append(className);
        return out.toString();
    }

    private static final class LoadStats {
        private int explicitBlocks;
        private int inlineBlocks;
        private int skippedInlineBlocks;
        private int linkNodes;
        private int loadedLinks;
        private int skippedLinks;
        private int failedLinks;
    }

    private record LinkDecision(boolean shouldLoad, String reason) {
        private static LinkDecision loaded() { return new LinkDecision(true, ""); }
        private static LinkDecision skip(String reason) { return new LinkDecision(false, reason == null ? "skipped" : reason); }
    }
}
