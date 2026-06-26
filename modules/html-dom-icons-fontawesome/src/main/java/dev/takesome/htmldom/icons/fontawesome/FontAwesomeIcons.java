package dev.takesome.htmldom.icons.fontawesome;

import dev.takesome.htmldom.icons.UiIcon;
import dev.takesome.htmldom.icons.registry.IconRegistry;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/** CSS-class helper for Font Awesome classes such as fa-solid fa-gear. */
public final class FontAwesomeIcons {
    private static final IconRegistry REGISTRY = FontAwesomeBundle.registry();

    private FontAwesomeIcons() {
    }

    public static Optional<UiIcon> fromCssClasses(Collection<String> classes) {
        if (classes == null || classes.isEmpty()) return Optional.empty();
        FontAwesomeStyle style = style(classes);
        for (String raw : classes) {
            String token = normalize(raw);
            if (!token.startsWith("fa-")) continue;
            if (token.equals("fa") || token.equals("fas") || token.equals("far") || token.equals("fab") || token.equals("fa-solid") || token.equals("fa-regular") || token.equals("fa-brands")) continue;
            String name = token.substring(3);
            Optional<UiIcon> exact = REGISTRY.find("fontawesome:" + style.styleId() + ":" + name);
            if (exact.isPresent()) return exact;
            Optional<UiIcon> friendly = REGISTRY.find(name);
            if (friendly.isPresent()) return friendly;
        }
        return Optional.empty();
    }

    public static String glyph(Collection<String> classes) {
        return fromCssClasses(classes).map(UiIcon::text).orElse("");
    }

    public static FontAwesomeStyle style(Collection<String> classes) {
        for (String raw : classes) {
            String token = normalize(raw);
            if (token.equals("fa-regular") || token.equals("far")) return FontAwesomeStyle.REGULAR;
            if (token.equals("fa-brands") || token.equals("fab")) return FontAwesomeStyle.BRANDS;
            if (token.equals("fa-solid") || token.equals("fas")) return FontAwesomeStyle.SOLID;
        }
        return FontAwesomeStyle.SOLID;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
