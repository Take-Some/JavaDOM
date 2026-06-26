package dev.takesome.htmldom.icons.fontawesome;

import dev.takesome.htmldom.icons.UiIcon;
import dev.takesome.htmldom.icons.registry.IconRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bundled Font Awesome styles composed as one logical icon family.
 */
public final class FontAwesomeBundle {
    public static final String REGISTRY_NAME = "fontawesome-bundled";

    private static final List<FontAwesomeStyle> STYLES = List.of(FontAwesomeStyle.values());

    private FontAwesomeBundle() {
    }

    public static List<FontAwesomeStyle> styles() {
        return STYLES;
    }

    public static List<UiIcon> icons() {
        ArrayList<UiIcon> icons = new ArrayList<>();
        for (FontAwesomeStyle style : STYLES) {
            icons.addAll(style.icons());
        }
        return Collections.unmodifiableList(icons);
    }

    public static IconRegistry registry() {
        IconRegistry registry = IconRegistry.create(REGISTRY_NAME);
        for (FontAwesomeStyle style : STYLES) {
            registry.registerAll(style.icons());
        }
        return registry;
    }
}
