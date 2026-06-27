package dev.takesome.htmldom.desktop;

import java.awt.BasicStroke;
import java.util.Locale;

/** Border shorthand/style parsing and Java2D stroke selection. */
final class HtmlDomBorderStyles {
    private HtmlDomBorderStyles() {
    }

    static String borderStyle(String explicit, String shorthand) {
        String value = explicit == null ? "" : explicit.trim().toLowerCase(Locale.ROOT);
        if (borderStyleToken(value)) return value;
        if (shorthand != null && !shorthand.isBlank()) {
            for (String part : shorthand.trim().toLowerCase(Locale.ROOT).split("\s+")) {
                if (borderStyleToken(part)) return part;
            }
        }
        return "solid";
    }

    private static boolean borderStyleToken(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase(Locale.ROOT)) {
            case "none", "hidden", "solid", "dotted", "dashed", "double", "groove", "ridge", "inset", "outset" -> true;
            default -> false;
        };
    }

    static BasicStroke borderStroke(float width, String style) {
        float safe = Math.max(1f, width);
        return switch (style == null ? "solid" : style.toLowerCase(Locale.ROOT)) {
            case "dotted" -> new BasicStroke(safe, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{safe, safe * 2f}, 0f);
            case "dashed" -> new BasicStroke(safe, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{safe * 4f, safe * 3f}, 0f);
            case "double" -> new BasicStroke(Math.max(1f, safe / 3f));
            default -> new BasicStroke(safe);
        };
    }

}
