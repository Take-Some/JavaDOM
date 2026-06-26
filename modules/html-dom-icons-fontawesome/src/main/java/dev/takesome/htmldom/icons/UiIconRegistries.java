package dev.takesome.htmldom.icons;

import dev.takesome.htmldom.icons.fontawesome.FontAwesomeBundle;
import dev.takesome.htmldom.icons.fontawesome.FontAwesomeStyle;
import dev.takesome.htmldom.icons.registry.IconRegistry;

/**
 * Built-in icon registry factories.
 */
public final class UiIconRegistries {
    private UiIconRegistries() {
    }

    public static IconRegistry standard() {
        return fontAwesomeBundled();
    }

    public static IconRegistry fontAwesomeBundled() {
        return FontAwesomeBundle.registry();
    }

    public static IconRegistry fontAwesomeSolid() {
        return FontAwesomeStyle.SOLID.registry();
    }

    public static IconRegistry fontAwesomeRegular() {
        return FontAwesomeStyle.REGULAR.registry();
    }

    public static IconRegistry fontAwesomeBrands() {
        return FontAwesomeStyle.BRANDS.registry();
    }
}
