package dev.takesome.htmldom.fonts;

import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Desktop font registry used by HtmlDom renderers. */
public final class HtmlDomFontRegistry {
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(HtmlDomFontRegistry.class);
    private static final String DEFAULT_DESCRIPTOR = "html-dom/fonts/fonts.json";
    private static final String DEFAULT_REPOSITORY = "html-dom/fonts";

    private final Map<String, HtmlDomFontFace> byId = new LinkedHashMap<>();
    private final Map<String, HtmlDomFontFace> byFamily = new LinkedHashMap<>();
    private boolean builtInsLoaded;

    public synchronized HtmlDomFontRegistry loadBuiltIns() {
        if (builtInsLoaded) return this;
        builtInsLoaded = true;
        List<FontSpec> fonts = readFontsJson(DEFAULT_DESCRIPTOR);
        LOG.info("Loading {} font descriptors from {}", fonts.size(), DEFAULT_DESCRIPTOR);
        for (FontSpec font : fonts) {
            registerFont(font);
        }
        LOG.info("Registered {} HtmlDom fonts", byId.size());
        return this;
    }

    public synchronized HtmlDomFontFace registerClasspath(String id, String classpathPath) {
        return registerClasspath(id, classpathPath, "", false);
    }

    public synchronized HtmlDomFontFace registerClasspath(String id, String classpathPath, String familyAlias, boolean iconFont) {
        String path = cleanPath(classpathPath);
        try (InputStream stream = openClasspath(path)) {
            return registerStream(clean(id).isBlank() ? inferId(path) : clean(id), path, familyAlias, iconFont, stream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to close font resource: " + path, ex);
        }
    }

    public synchronized Optional<HtmlDomFontFace> find(String idOrFamily) {
        String key = key(idOrFamily);
        if (key.isBlank()) return Optional.empty();
        HtmlDomFontFace byExactId = byId.get(key);
        if (byExactId != null) return Optional.of(byExactId);
        return Optional.ofNullable(byFamily.get(key));
    }

    public synchronized Font font(String cssFamilyStack, int style, float size) {
        loadBuiltIns();
        for (String family : cssFamilies(cssFamilyStack)) {
            HtmlDomFontFace face = find(family).orElse(null);
            if (face != null) return face.derive(style, size);
        }
        HtmlDomFontFace defaultFace = byFamily.values().stream().findFirst().orElse(null);
        if (defaultFace != null) return defaultFace.derive(style, size);
        LOG.warn("No registered HtmlDom font found for stack '{}'; using platform fallback", cssFamilyStack);
        return new Font(Font.SANS_SERIF, style, Math.max(1, Math.round(size)));
    }

    public synchronized Map<String, HtmlDomFontFace> fonts() {
        return Map.copyOf(byId);
    }

    private HtmlDomFontFace registerFont(FontSpec spec) {
        try (InputStream stream = openFont(spec)) {
            return registerStream(spec.id().isBlank() ? inferId(spec.path()) : spec.id(), spec.fullPath(), spec.family(), spec.iconFont(), stream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to close font resource: " + spec.fullPath(), ex);
        }
    }

    private HtmlDomFontFace registerStream(String id, String source, String familyAlias, boolean iconFont, InputStream stream) {
        if (stream == null) throw new IllegalStateException("Font resource not found: " + source);
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            String family = clean(familyAlias).isBlank() ? font.getFamily(Locale.ROOT) : familyAlias.trim();
            HtmlDomFontFace face = new HtmlDomFontFace(clean(id).isBlank() ? inferId(source) : clean(id), family, source, font, iconFont);
            byId.put(key(face.id()), face);
            byFamily.putIfAbsent(key(face.family()), face);
            byFamily.putIfAbsent(key(stripQuotes(face.family())), face);
            byFamily.putIfAbsent(key(font.getFontName(Locale.ROOT)), face);
            LOG.info("Registered font id={} family='{}' source={} icon={}", face.id(), face.family(), face.source(), face.iconFont());
            return face;
        } catch (java.awt.FontFormatException | IOException ex) {
            LOG.error("Failed to register font resource {}", ex, source);
            throw new IllegalStateException("Failed to register font resource: " + source, ex);
        }
    }

    private static InputStream openFont(FontSpec spec) {
        String configuredRoot = firstNonBlank(
                System.getProperty("htmldom.font.repository"),
                System.getenv("HTMLDOM_FONT_REPOSITORY"),
                System.getProperty("htmldom.repository"),
                System.getenv("HTMLDOM_REPOSITORY")
        );
        if (!configuredRoot.isBlank()) {
            Path external = Path.of(configuredRoot).resolve(spec.path()).normalize();
            if (Files.isRegularFile(external)) {
                try {
                    return Files.newInputStream(external);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to open external font resource: " + external, ex);
                }
            }
        }
        return openClasspath(spec.fullPath());
    }

    private static InputStream openClasspath(String path) {
        String clean = cleanPath(path);
        InputStream stream = loader().getResourceAsStream(clean);
        if (stream == null) throw new IllegalStateException("Font resource not found on classpath: " + clean);
        return stream;
    }

    private static List<FontSpec> readFontsJson(String resource) {
        String text = readResource(resource);
        if (text.isBlank()) return fallbackArrayDescriptor();
        String repository = field(text, "repository").orElse(DEFAULT_REPOSITORY);
        ArrayList<FontSpec> specs = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{([^{}]*)}").matcher(arrayBody(text, "fonts"));
        while (matcher.find()) {
            String object = matcher.group(1);
            String path = field(object, "path").orElse("");
            if (path.isBlank()) continue;
            specs.add(new FontSpec(
                    field(object, "id").orElse(inferId(path)),
                    repository,
                    path,
                    field(object, "family").orElse(""),
                    Boolean.parseBoolean(field(object, "iconFont").orElse("false"))
            ));
        }
        if (!specs.isEmpty()) return specs;
        return fallbackArrayDescriptor();
    }

    private static List<FontSpec> fallbackArrayDescriptor() {
        ArrayList<FontSpec> specs = new ArrayList<>();
        for (String path : readJsonStringArray("html-dom/fonts/built-in-fonts.json")) {
            specs.add(new FontSpec(inferId(path), "", path, "", false));
        }
        return specs;
    }

    private static String readResource(String resource) {
        try (InputStream stream = loader().getResourceAsStream(resource)) {
            if (stream == null) return "";
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read font descriptor: " + resource, ex);
        }
    }

    private static String arrayBody(String json, String field) {
        int key = json.indexOf('"' + field + '"');
        if (key < 0) return "";
        int start = json.indexOf('[', key);
        int end = json.indexOf(']', start);
        return start >= 0 && end > start ? json.substring(start + 1, end) : "";
    }

    private static Optional<String> field(String json, String name) {
        Pattern pattern = Pattern.compile('"' + Pattern.quote(name) + '"' + "\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(unescape(matcher.group(1)));
    }

    private static String unescape(String value) {
        return value == null ? "" : value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static List<String> readJsonStringArray(String resource) {
        String text = readResource(resource);
        ArrayList<String> values = new ArrayList<>();
        StringBuilder current = null;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (current == null) {
                if (c == '"') current = new StringBuilder();
                continue;
            }
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                values.add(current.toString());
                current = null;
            } else {
                current.append(c);
            }
        }
        return values;
    }

    private static List<String> cssFamilies(String stack) {
        ArrayList<String> out = new ArrayList<>();
        if (stack != null) {
            for (String raw : stack.split(",")) {
                String family = stripQuotes(raw.trim());
                if (!family.isBlank() && !genericFamily(family)) out.add(family);
            }
        }
        return out;
    }

    private static boolean genericFamily(String family) {
        String key = key(family);
        return key.equals("serif") || key.equals("sans-serif") || key.equals("monospace") || key.equals("system-ui") || key.equals("ui-sans-serif") || key.equals("ui-monospace");
    }

    private static String inferId(String path) {
        String file = cleanPath(path);
        int slash = file.lastIndexOf('/');
        if (slash >= 0) file = file.substring(slash + 1);
        int dot = file.lastIndexOf('.');
        if (dot > 0) file = file.substring(0, dot);
        return "font:" + key(file);
    }

    private static ClassLoader loader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? HtmlDomFontRegistry.class.getClassLoader() : loader;
    }

    private static String joinPath(String left, String right) {
        String a = cleanPath(left);
        String b = cleanPath(right);
        if (a.isBlank()) return b;
        if (b.isBlank()) return a;
        return a.endsWith("/") ? a + b : a + "/" + b;
    }

    private static String cleanPath(String value) {
        String path = clean(value).replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        return path;
    }

    private static String key(String value) {
        return stripQuotes(clean(value)).toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private static String stripQuotes(String value) {
        String v = clean(value);
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) return v.substring(1, v.length() - 1);
        return v;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            String clean = clean(value);
            if (!clean.isBlank()) return clean;
        }
        return "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record FontSpec(String id, String repository, String path, String family, boolean iconFont) {
        private String fullPath() {
            return path.startsWith("classpath:") ? cleanPath(path.substring("classpath:".length())) : joinPath(repository, path);
        }
    }
}
