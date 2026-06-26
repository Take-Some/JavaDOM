package dev.takesome.htmldom.css.properties.text;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;
import dev.takesome.htmldom.css.UiCssFontFamilyResolver;
import dev.takesome.htmldom.css.UiCssValue;
import java.util.Set;

public final class FontFamilyCssProperty extends UiCssStringPropertySpec {
    public FontFamilyCssProperty() {
        super("font-family", Set.of(), true);
    }

    @Override
    public UiCssValue initialValue() {
        return UiCssValue.typed(name(), UiCssFontFamilyResolver.DEFAULT_STACK);
    }
}
