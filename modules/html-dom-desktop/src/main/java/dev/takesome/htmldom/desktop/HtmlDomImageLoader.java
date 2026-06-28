package dev.takesome.htmldom.desktop;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import dev.takesome.htmldom.desktop.resource.HtmlDomResource;
import dev.takesome.htmldom.desktop.resource.HtmlDomResourceBundle;
import dev.takesome.htmldom.desktop.resource.HtmlDomResourceKind;
import dev.takesome.htmldom.desktop.resource.HtmlDomResourceResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Shared raster image loader for CSS background images and HTML img elements. */
final class HtmlDomImageLoader {
    private final ClassLoader classLoader;
    private final List<String> basePaths;
    private final HtmlDomResourceResolver resourceResolver;
    private final Map<String, BufferedImage> cache = new ConcurrentHashMap<>();

    HtmlDomImageLoader(ClassLoader classLoader, String... basePaths) {
        this.classLoader = classLoader == null ? HtmlDomImageLoader.class.getClassLoader() : classLoader;
        ArrayList<String> out = new ArrayList<>();
        if (basePaths != null) {
            for (String basePath : basePaths) {
                String normalized = normalizePath(basePath);
                if (!normalized.isBlank()) out.add(stripFileName(normalized));
            }
        }
        this.basePaths = List.copyOf(out);
        this.resourceResolver = new HtmlDomResourceResolver(this.classLoader, HtmlDomResourceBundle.forDocument("", this.basePaths.toArray(String[]::new)));
    }

    BufferedImage load(String resource) {
        String key = normalizePath(stripQueryAndFragment(resource));
        if (key.isBlank()) return null;
        return cache.computeIfAbsent(key, this::read);
    }

    private BufferedImage read(String resource) {
        java.util.Optional<HtmlDomResource> found = resourceResolver.resolve(resource, HtmlDomResourceKind.IMAGE);
        if (found.isPresent()) {
            HtmlDomResource res = found.get();
            BufferedImage img = readBytes(res.bytes(), res.resolvedPath(), res.source());
            if (img != null) return img;
        }
        for (Candidate candidate : candidates(resource)) {
            BufferedImage image = candidate.read();
            if (image != null) return image;
        }
        return null;
    }

    private List<Candidate> candidates(String resource) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String normalized = normalizePath(resource);
        if (normalized.isBlank()) return List.of();
        names.add(normalized);
        names.add(stripLeadingSlash(normalized));
        if (!absoluteLike(normalized)) {
            for (String base : basePaths) {
                if (!base.isBlank()) names.add(base + "/" + normalized);
            }
        }
        ArrayList<Candidate> out = new ArrayList<>();
        for (String name : names) {
            out.add(new FileCandidate(name));
            out.add(new ClasspathCandidate(name));
        }
        return out;
    }

    private BufferedImage readBytes(byte[] bytes, String sourceName, String uri) {
        if (bytes == null || bytes.length == 0) return null;
        if (svgSource(sourceName, bytes)) return readSvg(bytes, uri == null || uri.isBlank() ? sourceName : uri);
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(stream);
        } catch (IOException ignored) {
            return null;
        }
    }

    private BufferedImage readSvg(byte[] bytes, String uri) {
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
            TranscoderInput input = new TranscoderInput(stream);
            if (uri != null && !uri.isBlank()) input.setURI(uri);
            transcoder.transcode(input, null);
            return transcoder.image();
        } catch (IOException | TranscoderException | RuntimeException ignored) {
            return null;
        }
    }

    private boolean svgSource(String name, byte[] bytes) {
        String lower = normalizePath(name).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".svg") || lower.endsWith(".svgz")) return true;
        int max = Math.min(bytes.length, 256);
        String head = new String(bytes, 0, max, java.nio.charset.StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return head.contains("<svg") || head.contains("<!doctype svg");
    }

    private Path filePath(String resource) {
        if (resource == null || resource.isBlank()) return null;
        String value = normalizePath(resource);
        try {
            if (value.toLowerCase(Locale.ROOT).startsWith("file:")) return Path.of(URI.create(value));
            return Path.of(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean absoluteLike(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.startsWith("file:") || lower.matches("^[a-z]:/.*") || lower.startsWith("//") || lower.startsWith("/");
    }

    private static String stripFileName(String value) {
        String normalized = normalizePath(value);
        if (normalized.isBlank()) return "";
        if (normalized.endsWith("/")) return normalized.substring(0, normalized.length() - 1);
        int slash = normalized.lastIndexOf('/');
        if (slash < 0) return "";
        return normalized.substring(0, slash);
    }

    private static String stripLeadingSlash(String path) {
        String value = normalizePath(path);
        while (value.startsWith("/")) value = value.substring(1);
        return value;
    }

    private static String stripQueryAndFragment(String path) {
        if (path == null) return "";
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int cut = -1;
        if (query >= 0) cut = query;
        if (fragment >= 0) cut = cut < 0 ? fragment : Math.min(cut, fragment);
        return cut < 0 ? path : path.substring(0, cut);
    }

    private static String normalizePath(String resource) {
        return resource == null ? "" : resource.trim().replace('\\', '/');
    }

    private interface Candidate {
        BufferedImage read();
    }

    private final class FileCandidate implements Candidate {
        private final String resource;
        private FileCandidate(String resource) {
            this.resource = resource;
        }
        @Override public BufferedImage read() {
            Path path = filePath(resource);
            if (path == null || !Files.isRegularFile(path)) return null;
            try {
                return readBytes(Files.readAllBytes(path), resource, path.toUri().toString());
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    private final class ClasspathCandidate implements Candidate {
        private final String resource;
        private ClasspathCandidate(String resource) {
            this.resource = resource;
        }
        @Override public BufferedImage read() {
            String path = stripLeadingSlash(resource);
            if (path.isBlank()) return null;
            try (InputStream stream = classLoader.getResourceAsStream(path)) {
                if (stream == null) return null;
                return readBytes(stream.readAllBytes(), path, classLoader.getResource(path) == null ? path : classLoader.getResource(path).toString());
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    private static final class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage image;

        @Override public BufferedImage createImage(int width, int height) {
            return new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        }

        @Override public void writeImage(BufferedImage image, TranscoderOutput output) {
            this.image = image;
        }

        BufferedImage image() {
            return image;
        }
    }
}
