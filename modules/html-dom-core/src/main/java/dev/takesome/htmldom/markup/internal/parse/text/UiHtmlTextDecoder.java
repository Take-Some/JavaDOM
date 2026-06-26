package dev.takesome.htmldom.markup.internal.parse.text;

import dev.takesome.htmldom.markup.internal.parse.syntax.UiHtmlSyntaxProfile;

/** Entity decoding and whitespace normalization shared by scanner and tree conversion. */
public final class UiHtmlTextDecoder {
    private UiHtmlTextDecoder() {
    }

    public static String textFor(String tagName, String value, UiHtmlSyntaxProfile syntax) {
        String raw = value == null ? "" : value;
        if (syntax.isRawTextTag(tagName)) {
            return raw;
        }
        String decoded = decodeEntities(raw);
        return syntax.preservesText(tagName) ? decoded : collapseWhitespace(decoded);
    }

    public static String decodeEntities(String value) {
        if (value == null || value.indexOf('&') < 0) {
            return value == null ? "" : value;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != '&') {
                out.append(ch);
                continue;
            }
            int semi = value.indexOf(';', i + 1);
            if (semi < 0) {
                out.append(ch);
                continue;
            }
            String decoded = decodeEntity(value.substring(i + 1, semi));
            if (decoded == null) {
                out.append('&');
                continue;
            }
            out.append(decoded);
            i = semi;
        }
        return out.toString();
    }

    public static String collapseWhitespace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        boolean inWhitespace = false;
        boolean wrote = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (wrote) {
                    inWhitespace = true;
                }
                continue;
            }
            if (inWhitespace) {
                out.append(' ');
                inWhitespace = false;
            }
            out.append(ch);
            wrote = true;
        }
        return out.toString();
    }

    private static String decodeEntity(String entity) {
        return switch (entity) {
            case "lt" -> "<";
            case "gt" -> ">";
            case "amp" -> "&";
            case "quot" -> "\"";
            case "apos" -> "'";
            case "nbsp" -> " ";
            default -> decodeNumericEntity(entity);
        };
    }

    private static String decodeNumericEntity(String entity) {
        if (entity == null || !entity.startsWith("#")) {
            return null;
        }
        try {
            int codePoint = entity.startsWith("#x") || entity.startsWith("#X")
                    ? Integer.parseInt(entity.substring(2), 16)
                    : Integer.parseInt(entity.substring(1));
            return new String(Character.toChars(codePoint));
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
