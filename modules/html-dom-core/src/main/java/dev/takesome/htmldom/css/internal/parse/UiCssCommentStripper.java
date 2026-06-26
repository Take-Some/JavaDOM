package dev.takesome.htmldom.css.internal.parse;

/** Removes CSS comments before block parsing. Mirrors previous tolerant parser behavior. */
public final class UiCssCommentStripper {
    public String strip(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            int start = source.indexOf("/*", index);
            if (start < 0) {
                out.append(source, index, source.length());
                break;
            }
            out.append(source, index, start);
            int end = source.indexOf("*/", start + 2);
            index = end < 0 ? source.length() : end + 2;
        }
        return out.toString();
    }
}
