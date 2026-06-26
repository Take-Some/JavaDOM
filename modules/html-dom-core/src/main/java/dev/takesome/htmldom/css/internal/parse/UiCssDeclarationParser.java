package dev.takesome.htmldom.css.internal.parse;

import dev.takesome.htmldom.css.UiCssDeclaration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses declaration blocks and inline style declaration lists. */
public final class UiCssDeclarationParser {
    public List<UiCssDeclaration> declarations(String block) {
        ArrayList<UiCssDeclaration> out = new ArrayList<>();
        for (String declaration : splitDeclarations(block)) {
            int colon = declaration.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String property = declaration.substring(0, colon).trim();
            String value = declaration.substring(colon + 1).trim();
            if (!property.isBlank()) {
                out.add(new UiCssDeclaration(property, value));
            }
        }
        return out;
    }

    public Map<String, String> declarationMap(String block) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (UiCssDeclaration declaration : declarations(block)) {
            out.put(declaration.property(), declaration.value());
        }
        return out;
    }

    private List<String> splitDeclarations(String block) {
        ArrayList<String> out = new ArrayList<>();
        if (block == null || block.isBlank()) {
            return out;
        }
        int start = 0;
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < block.length(); i++) {
            char ch = block.charAt(i);
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
            if (ch == '(') {
                depth++;
            } else if (ch == ')' && depth > 0) {
                depth--;
            } else if (ch == ';' && depth == 0) {
                addDeclaration(out, block, start, i);
                start = i + 1;
            }
        }
        addDeclaration(out, block, start, block.length());
        return out;
    }

    private void addDeclaration(ArrayList<String> out, String block, int start, int end) {
        String declaration = block.substring(start, end).trim();
        if (!declaration.isBlank()) {
            out.add(declaration);
        }
    }
}
