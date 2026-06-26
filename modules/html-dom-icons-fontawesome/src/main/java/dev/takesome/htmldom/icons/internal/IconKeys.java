package dev.takesome.htmldom.icons.internal;

import java.util.Locale;

/**
 * Normalization rules for icon ids and aliases.
 */
public final class IconKeys {
    private IconKeys() {
    }

    public static String key(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim()
                .replace('\\', '/')
                .replace('_', '-')
                .replace(' ', '-')
                .toLowerCase(Locale.ROOT);

        StringBuilder out = new StringBuilder(normalized.length());
        boolean previousDash = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean allowed = Character.isLetterOrDigit(c) || c == ':' || c == '/' || c == '.';
            if (allowed) {
                out.append(c);
                previousDash = false;
            } else if (c == '-' || Character.isWhitespace(c)) {
                if (!previousDash && !out.isEmpty()) {
                    out.append('-');
                    previousDash = true;
                }
            }
        }

        while (!out.isEmpty() && out.charAt(out.length() - 1) == '-') {
            out.deleteCharAt(out.length() - 1);
        }

        return out.toString();
    }
}
