package dev.takesome.htmldom.dom;

import static dev.takesome.htmldom.support.validation.HtmlDomValidator.emptyIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class UiDomSelector {
    private static final Set<String> LEGACY_PSEUDO_ELEMENTS = Set.of("before", "after");

    private final List<SelectorPart> chain;
    private final String pseudoElement;

    private UiDomSelector(List<SelectorPart> chain, String pseudoElement) {
        this.chain = List.copyOf(chain == null ? List.of() : chain);
        this.pseudoElement = emptyIfNull(pseudoElement);
    }

    public static UiDomSelector parse(String rawSelector) {
        if (rawSelector == null || rawSelector.isBlank()) throw new IllegalArgumentException("selector must not be blank");
        String selector = rawSelector.trim().toLowerCase(Locale.ROOT);
        PseudoElementSplit split = splitPseudoElement(selector);
        selector = split.selector().trim();
        String pseudoElement = split.pseudoElement();

        ArrayList<SelectorPart> chain = new ArrayList<>();
        for (SelectorToken token : selectorTokens(selector)) {
            if (!token.selector().isBlank()) chain.add(new SelectorPart(parseSimple(token.selector()), token.combinator()));
        }
        if (chain.isEmpty()) throw new IllegalArgumentException("unsupported selector: " + rawSelector);
        return new UiDomSelector(chain, pseudoElement);
    }

    public boolean matches(UiDomElement element) {
        if (element == null || chain.isEmpty()) return false;
        int last = chain.size() - 1;
        if (!chain.get(last).selector().matches(element)) return false;

        UiDomElement cursor = element;
        for (int i = last; i > 0; i--) {
            SelectorPart current = chain.get(i);
            SimpleSelector previous = chain.get(i - 1).selector();

            if (current.combinator() == Combinator.CHILD) {
                cursor = cursor.parent();
                if (cursor == null || !previous.matches(cursor)) return false;
                continue;
            }

            boolean found = false;
            UiDomElement ancestor = cursor.parent();
            while (ancestor != null) {
                if (previous.matches(ancestor)) {
                    cursor = ancestor;
                    found = true;
                    break;
                }
                ancestor = ancestor.parent();
            }
            if (!found) return false;
        }

        return true;
    }

    public String pseudoElement() { return pseudoElement; }

    public boolean hasPseudoElement() { return !pseudoElement.isBlank(); }

    private static PseudoElementSplit splitPseudoElement(String selector) {
        PseudoElementSplit doubleColon = splitDoubleColonPseudoElement(selector);
        if (!doubleColon.pseudoElement().isBlank()) return doubleColon;
        return splitLegacySingleColonPseudoElement(selector);
    }

    private static PseudoElementSplit splitDoubleColonPseudoElement(String selector) {
        int index = selector.indexOf("::");
        if (index < 0) return new PseudoElementSplit(selector, "");
        int tokenStart = index + 2;
        int tokenEnd = pseudoTokenEnd(selector, tokenStart);
        String pseudo = selector.substring(tokenStart, tokenEnd).trim();
        if (pseudo.isBlank()) return new PseudoElementSplit(selector, "");
        String remaining = (selector.substring(0, index) + selector.substring(tokenEnd)).trim();
        return new PseudoElementSplit(remaining, pseudo);
    }

    private static PseudoElementSplit splitLegacySingleColonPseudoElement(String selector) {
        for (int i = 0; i < selector.length(); i++) {
            char ch = selector.charAt(i);
            if (ch != ':' || (i + 1 < selector.length() && selector.charAt(i + 1) == ':')) continue;
            int tokenStart = i + 1;
            int tokenEnd = pseudoTokenEnd(selector, tokenStart);
            String pseudo = selector.substring(tokenStart, tokenEnd).trim();
            if (!LEGACY_PSEUDO_ELEMENTS.contains(pseudo)) continue;
            String remaining = (selector.substring(0, i) + selector.substring(tokenEnd)).trim();
            return new PseudoElementSplit(remaining, pseudo);
        }
        return new PseudoElementSplit(selector, "");
    }

    private static int pseudoTokenEnd(String selector, int from) {
        int index = from;
        while (index < selector.length()) {
            char ch = selector.charAt(index);
            if (ch == '#' || ch == '.' || ch == ':' || ch == '>' || Character.isWhitespace(ch)) break;
            index++;
        }
        return index;
    }

    private static SimpleSelector parseSimple(String raw) {
        if ("*".equals(raw)) return new SimpleSelector("", "", List.of(), List.of(), List.of(), true, false);
        if (":root".equals(raw)) return new SimpleSelector("", "", List.of(), List.of(), List.of(), false, true);
        String tag = "";
        String id = "";
        ArrayList<String> classes = new ArrayList<>();
        ArrayList<String> pseudos = new ArrayList<>();
        ArrayList<SimpleSelector> negations = new ArrayList<>();
        int index = 0;
        while (index < raw.length()) {
            char ch = raw.charAt(index);
            if (ch == '#') {
                int end = nextBreak(raw, index + 1);
                id = raw.substring(index + 1, end);
                index = end;
            } else if (ch == '.') {
                int end = nextBreak(raw, index + 1);
                classes.add(raw.substring(index + 1, end));
                index = end;
            } else if (ch == ':' && startsWithIgnoreCase(raw, index, ":not(")) {
                int close = matchingParen(raw, index + 4);
                if (close < 0) throw new IllegalArgumentException("unsupported selector: " + raw);
                String body = raw.substring(index + 5, close).trim();
                if (body.isBlank()) throw new IllegalArgumentException("unsupported selector: " + raw);
                for (String item : splitNotArguments(body)) {
                    if (item.isBlank()) continue;
                    List<SelectorToken> nested = selectorTokens(item);
                    if (nested.size() != 1 || nested.get(0).combinator() != Combinator.NONE) {
                        throw new IllegalArgumentException("unsupported :not selector: " + raw);
                    }
                    negations.add(parseSimple(nested.get(0).selector()));
                }
                index = close + 1;
            } else if (ch == ':') {
                int end = nextBreak(raw, index + 1);
                String pseudo = raw.substring(index + 1, end);
                if ("root".equals(pseudo)) return new SimpleSelector(tag, id, classes, pseudos, negations, false, true);
                pseudos.add(pseudo);
                index = end;
            } else {
                int end = nextBreak(raw, index);
                tag = raw.substring(index, end);
                index = end;
            }
        }
        if (tag.isBlank() && id.isBlank() && classes.isEmpty() && pseudos.isEmpty() && negations.isEmpty()) throw new IllegalArgumentException("unsupported selector: " + raw);
        return new SimpleSelector(tag, id, classes, pseudos, negations, false, false);
    }

    private static boolean startsWithIgnoreCase(String value, int offset, String prefix) {
        return value != null && prefix != null && offset >= 0 && value.regionMatches(true, offset, prefix, 0, prefix.length());
    }

    private static int matchingParen(String value, int openIndex) {
        if (value == null || openIndex < 0 || openIndex >= value.length() || value.charAt(openIndex) != '(') return -1;
        int depth = 0;
        char quote = 0;
        for (int i = openIndex; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (quote != 0) {
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                continue;
            }
            if (ch == '(') depth++;
            else if (ch == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static List<String> splitNotArguments(String value) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == '(' || ch == '[') depth++;
            else if ((ch == ')' || ch == ']') && depth > 0) depth--;
            if (ch == ',' && depth == 0) {
                out.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isBlank()) out.add(tail);
        return out;
    }

    private static List<SelectorToken> selectorTokens(String selector) {
        ArrayList<SelectorToken> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Combinator pending = Combinator.NONE;
        boolean endedWithChildCombinator = false;
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < selector.length(); i++) {
            char ch = selector.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == '(' || ch == '[') depth++;
            if ((ch == ')' || ch == ']') && depth > 0) depth--;
            if (Character.isWhitespace(ch) && depth == 0) {
                if (add(out, current, pending)) {
                    pending = Combinator.DESCENDANT;
                    endedWithChildCombinator = false;
                }
                continue;
            }
            if (ch == '>' && depth == 0) {
                if (add(out, current, pending)) endedWithChildCombinator = false;
                if (out.isEmpty()) throw new IllegalArgumentException("unsupported selector: " + selector);
                pending = Combinator.CHILD;
                endedWithChildCombinator = true;
                continue;
            }
            current.append(ch);
        }
        if (!add(out, current, pending) && endedWithChildCombinator) throw new IllegalArgumentException("unsupported selector: " + selector);
        return out;
    }

    private static boolean add(List<SelectorToken> out, StringBuilder current, Combinator combinator) {
        String value = current.toString().trim();
        if (!value.isBlank()) {
            if (out.isEmpty() && combinator != Combinator.NONE) throw new IllegalArgumentException("unsupported selector: " + value);
            out.add(new SelectorToken(value, out.isEmpty() ? Combinator.NONE : combinator));
            current.setLength(0);
            return true;
        }
        current.setLength(0);
        return false;
    }

    private static int nextBreak(String selector, int from) {
        int index = from;
        while (index < selector.length()) {
            char ch = selector.charAt(index);
            if (ch == '#' || ch == '.' || ch == ':' || ch == '>') break;
            index++;
        }
        return index;
    }

    private record PseudoElementSplit(String selector, String pseudoElement) { }

    private enum Combinator { NONE, DESCENDANT, CHILD }

    private record SelectorPart(SimpleSelector selector, Combinator combinator) { }

    private record SelectorToken(String selector, Combinator combinator) { }

    private record SimpleSelector(String tagName, String id, List<String> classNames, List<String> pseudoClasses, List<SimpleSelector> notSelectors, boolean any, boolean root) {
        boolean matches(UiDomElement element) {
            if (element == null) return false;
            if (root && element.ownerDocument().root() != element) return false;
            if (!any && !tagName.isBlank() && !tagName.equals(element.tagName())) return false;
            if (!id.isBlank() && !id.equals(element.id())) return false;
            for (String className : classNames) if (!element.classList().contains(className)) return false;
            for (String pseudoClass : pseudoClasses) if (!element.hasPseudoClass(pseudoClass)) return false;
            for (SimpleSelector notSelector : notSelectors) if (notSelector.matches(element)) return false;
            return root || any || !tagName.isBlank() || !id.isBlank() || !classNames.isEmpty() || !pseudoClasses.isEmpty() || !notSelectors.isEmpty();
        }
    }
}
