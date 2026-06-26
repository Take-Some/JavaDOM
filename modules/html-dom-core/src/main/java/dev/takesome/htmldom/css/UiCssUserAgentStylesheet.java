package dev.takesome.htmldom.css;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Built-in module-owned user-agent and component base styles for HtmlDom markup. */
public final class UiCssUserAgentStylesheet {
    private static final String MANIFEST_RESOURCE = "html-dom/styles/user-agent.styles";
    private static final UiStylesheet STYLESHEET = new UiCssParser().parse(css());

    private UiCssUserAgentStylesheet() {
    }

    public static UiStylesheet stylesheet() {
        return STYLESHEET;
    }

    private static String css() {
        StringBuilder out = new StringBuilder(8192);
        for (String resource : stylesheetResources()) {
            out.append('\n').append(readResource(resource)).append('\n');
        }
        return out.toString();
    }

    private static List<String> stylesheetResources() {
        String manifest = readResource(MANIFEST_RESOURCE);
        ArrayList<String> out = new ArrayList<>();
        for (String line : manifest.split("\\R")) {
            String value = line.trim();
            if (value.isBlank() || value.startsWith("#")) continue;
            out.add(value);
        }
        if (out.isEmpty()) throw new IllegalStateException("HtmlDom user-agent stylesheet manifest is empty: " + MANIFEST_RESOURCE);
        return List.copyOf(out);
    }

    private static String readResource(String path) {
        ClassLoader loader = UiCssUserAgentStylesheet.class.getClassLoader();
        try (InputStream stream = loader == null ? ClassLoader.getSystemResourceAsStream(path) : loader.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("HtmlDom stylesheet resource not found: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read HtmlDom stylesheet resource: " + path, exception);
        }
    }
}
