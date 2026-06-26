package dev.takesome.htmldom.icons.fontawesome;


import dev.takesome.htmldom.icons.resources.UiIconFontResource;
import dev.takesome.htmldom.icons.registry.IconRegistry;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Font Awesome styles supported by the bundled icon/font layer.
 */
public enum FontAwesomeStyle {
    SOLID(
            FontAwesomeIconFamily.of("solid", "fontawesome-solid", FontAwesomeSolidIcon::values),
            "Font Awesome Solid",
            "html-dom/icons/fontawesome/fa-solid-900.ttf"
    ),
    REGULAR(
            FontAwesomeIconFamily.of("regular", "fontawesome-regular", FontAwesomeRegularIcon::values),
            "Font Awesome Regular",
            "html-dom/icons/fontawesome/fa-regular-400.ttf"
    ),
    BRANDS(
            FontAwesomeIconFamily.of("brands", "fontawesome-brands", FontAwesomeBrandIcon::values),
            "Font Awesome Brands",
            "html-dom/icons/fontawesome/fa-brands-400.ttf"
    );

    private final FontAwesomeIconFamily<? extends FontAwesomeIcon> family;
    private final String displayName;
    private final UiIconFontResource resource;

    FontAwesomeStyle(
            FontAwesomeIconFamily<? extends FontAwesomeIcon> family,
            String displayName,
            String classpathPath
    ) {
        this.family = family;
        this.displayName = displayName;
        this.resource = new UiIconFontResource(
                FontAwesomeIcon.FAMILY_ID + ":" + family.styleId(),
                FontAwesomeIcon.FAMILY_ID,
                family.styleId(),
                displayName,
                classpathPath
        );
    }

    public String styleId() {
        return family.styleId();
    }

    public String registryName() {
        return family.registryName();
    }

    public String displayName() {
        return displayName;
    }

    public UiIconFontResource resource() {
        return resource;
    }

    public FontAwesomeIconFamily<? extends FontAwesomeIcon> family() {
        return family;
    }

    public List<? extends FontAwesomeIcon> icons() {
        return family.icons();
    }

    public IconRegistry registry() {
        return family.registry();
    }

    public static Optional<FontAwesomeStyle> find(String styleId) {
        String key = normalize(styleId);
        if (key.isEmpty()) return Optional.empty();
        for (FontAwesomeStyle style : values()) {
            if (style.styleId().equals(key) || style.name().toLowerCase(Locale.ROOT).equals(key)) {
                return Optional.of(style);
            }
        }
        return Optional.empty();
    }

    public static FontAwesomeStyle require(String styleId) {
        return find(styleId).orElseThrow(() -> new IllegalArgumentException("Unknown Font Awesome style: " + styleId));
    }

    private static String normalize(String value) {
        return (value == null ? "" : value.trim().toLowerCase(Locale.ROOT)).replace('_', '-');
    }
}
