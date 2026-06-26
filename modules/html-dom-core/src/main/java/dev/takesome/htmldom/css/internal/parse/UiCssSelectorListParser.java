package dev.takesome.htmldom.css.internal.parse;

import java.util.ArrayList;
import java.util.List;

/** Splits comma-separated selector lists without breaking inside strings/functions. */
public final class UiCssSelectorListParser {
    public List<String> split(String selectorBlock) {
        ArrayList<String> out = new ArrayList<>();
        if (selectorBlock == null || selectorBlock.isBlank()) {
            return out;
        }
        int start = 0;
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < selectorBlock.length(); i++) {
            char ch = selectorBlock.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                }
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                continue;
            }
            if (ch == '(' || ch == '[') {
                depth++;
            } else if ((ch == ')' || ch == ']') && depth > 0) {
                depth--;
            } else if (ch == ',' && depth == 0) {
                addSelector(out, selectorBlock, start, i);
                start = i + 1;
            }
        }
        addSelector(out, selectorBlock, start, selectorBlock.length());
        return out;
    }

    private void addSelector(ArrayList<String> out, String source, int start, int end) {
        String selector = source.substring(start, end).trim();
        if (!selector.isBlank()) {
            out.add(selector);
        }
    }
}
