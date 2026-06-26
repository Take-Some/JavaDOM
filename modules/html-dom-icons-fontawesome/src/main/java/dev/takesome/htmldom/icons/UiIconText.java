package dev.takesome.htmldom.icons;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Small unicode helpers for icon-font glyph strings.
 */
public final class UiIconText {
    private UiIconText() {
    }

    public static String glyph(UiIcon icon) {
        if (icon == null) {
            return "";
        }
        validateCodePoint(icon.codePoint(), icon.id());
        return new String(Character.toChars(icon.codePoint()));
    }

    public static String characters(Collection<? extends UiIcon> icons) {
        if (icons == null || icons.isEmpty()) {
            return "";
        }

        Set<Integer> codePoints = new LinkedHashSet<>();
        for (UiIcon icon : icons) {
            if (icon == null) {
                continue;
            }
            validateCodePoint(icon.codePoint(), icon.id());
            codePoints.add(icon.codePoint());
        }

        StringBuilder out = new StringBuilder(codePoints.size());
        for (Integer codePoint : codePoints) {
            out.appendCodePoint(codePoint);
        }
        return out.toString();
    }

    private static void validateCodePoint(int codePoint, String id) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid icon code point for " + id + ": " + codePoint);
        }
    }
}
