package dev.takesome.htmldom.icons.resources;


import dev.takesome.htmldom.icons.UiIcon;
import dev.takesome.htmldom.icons.fontawesome.FontAwesomeBundle;
import dev.takesome.htmldom.icons.fontawesome.FontAwesomeStyle;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Built-in icon-font resource descriptors.
 */
public final class UiIconFontResources {
    public static final String FONT_AWESOME_SOLID_PATH = FontAwesomeStyle.SOLID.resource().classpathPath();
    public static final String FONT_AWESOME_REGULAR_PATH = FontAwesomeStyle.REGULAR.resource().classpathPath();
    public static final String FONT_AWESOME_BRANDS_PATH = FontAwesomeStyle.BRANDS.resource().classpathPath();

    public static final UiIconFontResource FONT_AWESOME_SOLID = FontAwesomeStyle.SOLID.resource();
    public static final UiIconFontResource FONT_AWESOME_REGULAR = FontAwesomeStyle.REGULAR.resource();
    public static final UiIconFontResource FONT_AWESOME_BRANDS = FontAwesomeStyle.BRANDS.resource();

    private UiIconFontResources() {
    }

    public static List<UiIconFontResource> fontAwesomeBundled() {
        return FontAwesomeBundle.styles().stream()
                .map(FontAwesomeStyle::resource)
                .toList();
    }

    public static String key(UiIcon icon) {
        if (icon == null) {
            return "";
        }
        return key(icon.familyId(), icon.styleId());
    }

    public static String key(String familyId, String styleId) {
        String family = familyId == null ? "" : familyId.trim();
        String style = styleId == null ? "" : styleId.trim();
        if (family.isEmpty() || style.isEmpty()) {
            return "";
        }
        return family + ":" + style;
    }

    public static boolean isAvailable(UiIconFontResource resource) {
        return locate(resource) != null;
    }

    public static URL locate(UiIconFontResource resource) {
        if (resource == null) {
            return null;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = UiIconFontResources.class.getClassLoader();
        }
        return loader.getResource(resource.classpathPath());
    }

    public static InputStream open(UiIconFontResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = UiIconFontResources.class.getClassLoader();
        }
        InputStream stream = loader.getResourceAsStream(resource.classpathPath());
        if (stream == null) {
            throw new IllegalStateException("Missing bundled icon font resource: " + resource.classpathPath());
        }
        return stream;
    }
}
