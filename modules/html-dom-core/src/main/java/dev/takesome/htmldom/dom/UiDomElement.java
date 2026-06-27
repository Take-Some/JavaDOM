package dev.takesome.htmldom.dom;

import dev.takesome.htmldom.css.UiCssStyleImpact;

import static dev.takesome.htmldom.support.validation.HtmlDomValidator.emptyIfNull;
import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Element node in the HtmlDom DOM tree. */
public final class UiDomElement extends UiDomNode {
    private final String tagName;
    private final LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> computedStyle = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> animatedComputedStyle = new LinkedHashMap<>();
    private final LinkedHashMap<String, LinkedHashMap<String, String>> pseudoComputedStyle = new LinkedHashMap<>();
    private final LinkedHashSet<String> pseudoClasses = new LinkedHashSet<>();
    private final UiDomTokenList classList = new UiDomTokenList(this);

    UiDomElement(UiDomDocument ownerDocument, int nodeId, String tagName) {
        super(ownerDocument, nodeId);
        this.tagName = normalizeName(tagName, "tagName");
    }

    @Override
    public UiDomNodeType nodeType() {
        return UiDomNodeType.ELEMENT;
    }

    @Override
    public String nodeName() {
        return tagName;
    }

    public String tagName() {
        return tagName;
    }

    public String id() {
        return attribute("id", "");
    }

    public UiDomTokenList classList() {
        return classList;
    }

    public Map<String, String> attributes() {
        synchronized (attributes) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        }
    }

    public Map<String, String> dataset() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : attributes().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("data-") && key.length() > 5) {
                out.put(datasetName(key.substring(5)), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(out);
    }

    public String data(String name) {
        return data(name, "");
    }

    public String data(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        String normalized = name.trim().startsWith("data-") ? normalizeName(name, "data attribute") : "data-" + dashCase(name);
        return attribute(normalized, fallback);
    }

    public boolean hasAttribute(String name) {
        synchronized (attributes) {
            return attributes.containsKey(normalizeName(name, "attribute name"));
        }
    }

    public String attribute(String name) {
        return attribute(name, "");
    }

    public String attribute(String name, String fallback) {
        synchronized (attributes) {
            String value = attributes.get(normalizeName(name, "attribute name"));
            return value == null ? fallback : value;
        }
    }

    public void setAttribute(String name, String value) {
        String key = normalizeName(name, "attribute name");
        String next = emptyIfNull(value);
        String old;
        synchronized (attributes) {
            old = attributes.put(key, next);
        }
        if (Objects.equals(old, next)) return;
        markDirty(UiDomMutationType.ATTRIBUTE_CHANGED, key, old, next);
    }

    public void removeAttribute(String name) {
        String key = normalizeName(name, "attribute name");
        String old;
        synchronized (attributes) {
            if (!attributes.containsKey(key)) return;
            old = attributes.remove(key);
        }
        markDirty(UiDomMutationType.ATTRIBUTE_REMOVED, key, old, "");
    }

    public Set<String> pseudoClasses() {
        synchronized (pseudoClasses) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(pseudoClasses));
        }
    }

    public boolean hasPseudoClass(String name) {
        String key = normalizeName(name, "pseudo class");
        synchronized (pseudoClasses) {
            return pseudoClasses.contains(key) || nativePseudoClass(key);
        }
    }

    public void setPseudoClass(String name, boolean enabled) {
        String key = normalizeName(name, "pseudo class");
        boolean changed;
        synchronized (pseudoClasses) {
            changed = enabled ? pseudoClasses.add(key) : pseudoClasses.remove(key);
        }
        if (changed) markDirty(UiDomMutationType.ATTRIBUTE_CHANGED, ":" + key, enabled ? "false" : "true", enabled ? "true" : "false");
    }

    public Map<String, String> computedStyle() {
        synchronized (computedStyle) {
            synchronized (animatedComputedStyle) {
                LinkedHashMap<String, String> out = new LinkedHashMap<>(computedStyle);
                out.putAll(animatedComputedStyle);
                return Collections.unmodifiableMap(out);
            }
        }
    }

    public Map<String, String> baseComputedStyle() {
        synchronized (computedStyle) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(computedStyle));
        }
    }

    public Map<String, String> animatedComputedStyle() {
        synchronized (animatedComputedStyle) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(animatedComputedStyle));
        }
    }

    public String style(String property) {
        return style(property, "");
    }

    public String style(String property, String fallback) {
        String key = normalizeStyleProperty(property);
        synchronized (computedStyle) {
            synchronized (animatedComputedStyle) {
                String animated = animatedComputedStyle.get(key);
                if (animated != null) return animated;
                String value = computedStyle.get(key);
                return value == null ? fallback : value;
            }
        }
    }

    public void setComputedStyle(String property, String value) {
        String key = normalizeStyleProperty(property);
        String next = trimToEmpty(value);
        String old;
        synchronized (computedStyle) {
            old = computedStyle.put(key, next);
        }
        if (Objects.equals(old, next)) return;
        markDirty(UiDomMutationType.STYLE_CHANGED, key, old, next);
    }

    public boolean setInternalComputedStyle(String property, String value) {
        String key = normalizeStyleProperty(property);
        String next = trimToEmpty(value);
        synchronized (computedStyle) {
            String old = computedStyle.put(key, next);
            return !Objects.equals(old, next);
        }
    }

    public boolean setAnimatedComputedStyle(String property, String value) {
        String key = normalizeStyleProperty(property);
        String next = trimToEmpty(value);
        synchronized (animatedComputedStyle) {
            String old = animatedComputedStyle.put(key, next);
            return !Objects.equals(old, next);
        }
    }

    public boolean removeAnimatedComputedStyle(String property) {
        String key = normalizeStyleProperty(property);
        synchronized (animatedComputedStyle) {
            return animatedComputedStyle.remove(key) != null;
        }
    }

    public boolean replaceAnimatedComputedStyle(Map<String, String> style) {
        LinkedHashMap<String, String> nextStyle = new LinkedHashMap<>();
        if (style != null) {
            style.forEach((property, value) -> nextStyle.put(normalizeStyleProperty(property), trimToEmpty(value)));
        }
        synchronized (animatedComputedStyle) {
            if (animatedComputedStyle.equals(nextStyle)) return false;
            animatedComputedStyle.clear();
            animatedComputedStyle.putAll(nextStyle);
            return true;
        }
    }

    public boolean clearAnimatedComputedStyle() {
        synchronized (animatedComputedStyle) {
            if (animatedComputedStyle.isEmpty()) return false;
            animatedComputedStyle.clear();
            return true;
        }
    }

    public Map<String, String> pseudoComputedStyle(String pseudoElement) {
        String key = normalizePseudoElement(pseudoElement);
        synchronized (pseudoComputedStyle) {
            Map<String, String> style = pseudoComputedStyle.get(key);
            return style == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(style));
        }
    }

    public String pseudoStyle(String pseudoElement, String property) {
        return pseudoStyle(pseudoElement, property, "");
    }

    public String pseudoStyle(String pseudoElement, String property, String fallback) {
        String pseudo = normalizePseudoElement(pseudoElement);
        synchronized (pseudoComputedStyle) {
            Map<String, String> style = pseudoComputedStyle.get(pseudo);
            if (style == null) return fallback;
            String value = style.get(normalizeStyleProperty(property));
            return value == null ? fallback : value;
        }
    }

    public void setPseudoComputedStyle(String pseudoElement, String property, String value) {
        String pseudo = normalizePseudoElement(pseudoElement);
        String key = normalizeStyleProperty(property);
        String next = trimToEmpty(value);
        String old;
        synchronized (pseudoComputedStyle) {
            LinkedHashMap<String, String> style = pseudoComputedStyle.computeIfAbsent(pseudo, ignored -> new LinkedHashMap<>());
            old = style.put(key, next);
        }
        if (Objects.equals(old, next)) return;
        markDirty(UiDomMutationType.STYLE_CHANGED, "::" + pseudo + "." + key, old, next);
    }

    public void clearComputedStyle() {
        boolean changed;
        synchronized (computedStyle) {
            synchronized (animatedComputedStyle) {
                synchronized (pseudoComputedStyle) {
                    changed = !computedStyle.isEmpty() || !animatedComputedStyle.isEmpty() || !pseudoComputedStyle.isEmpty();
                    if (!changed) return;
                    computedStyle.clear();
                    animatedComputedStyle.clear();
                    pseudoComputedStyle.clear();
                }
            }
        }
        markDirty(UiDomMutationType.STYLE_CHANGED, "*", "", "");
    }


    public UiCssStyleImpact replaceComputedStyle(Map<String, String> style, Map<String, Map<String, String>> pseudoStyle) {
        LinkedHashMap<String, String> nextStyle = new LinkedHashMap<>();
        if (style != null) {
            style.forEach((property, value) -> nextStyle.put(normalizeStyleProperty(property), trimToEmpty(value)));
        }
        LinkedHashMap<String, LinkedHashMap<String, String>> nextPseudoStyle = new LinkedHashMap<>();
        if (pseudoStyle != null) {
            pseudoStyle.forEach((pseudoElement, values) -> {
                LinkedHashMap<String, String> target = new LinkedHashMap<>();
                if (values != null) {
                    values.forEach((property, value) -> target.put(normalizeStyleProperty(property), trimToEmpty(value)));
                }
                if (!target.isEmpty()) nextPseudoStyle.put(normalizePseudoElement(pseudoElement), target);
            });
        }
        UiCssStyleImpact impact = UiCssStyleImpact.NONE;
        synchronized (computedStyle) {
            synchronized (pseudoComputedStyle) {
                if (computedStyle.equals(nextStyle) && pseudoComputedStyle.equals(nextPseudoStyle)) return UiCssStyleImpact.NONE;
                for (Map.Entry<String, String> entry : nextStyle.entrySet()) {
                    if (!Objects.equals(computedStyle.get(entry.getKey()), entry.getValue())) impact = impact.merge(UiCssStyleImpact.of(entry.getKey()));
                }
                for (String property : computedStyle.keySet()) {
                    if (!nextStyle.containsKey(property)) impact = impact.merge(UiCssStyleImpact.of(property));
                }
                if (!pseudoComputedStyle.equals(nextPseudoStyle)) impact = impact.merge(UiCssStyleImpact.PAINT);
                computedStyle.clear();
                computedStyle.putAll(nextStyle);
                pseudoComputedStyle.clear();
                pseudoComputedStyle.putAll(nextPseudoStyle);
            }
        }
        markDirty(UiDomMutationType.STYLE_CHANGED, "*", "", "");
        return impact;
    }

    public boolean matches(UiDomSelector selector) {
        return selector != null && selector.matches(this);
    }

    public boolean matches(String selector) {
        return selector != null && !selector.isBlank() && UiDomSelector.parse(selector).matches(this);
    }

    public UiDomElement closest(String selector) {
        if (selector == null || selector.isBlank()) return null;
        UiDomSelector parsed = UiDomSelector.parse(selector);
        UiDomElement cursor = this;
        while (cursor != null) {
            if (parsed.matches(cursor)) return cursor;
            cursor = cursor.parent();
        }
        return null;
    }

    private boolean nativePseudoClass(String key) {
        if ("disabled".equals(key)) return hasAttribute("disabled");
        if ("enabled".equals(key)) return !hasAttribute("disabled");
        if ("checked".equals(key)) return hasAttribute("checked");
        if ("selected".equals(key)) return hasAttribute("selected");
        if ("focus".equals(key)) return hasAttribute("focused") || hasAttribute("focus");
        return false;
    }

    private static String datasetName(String value) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '-' || ch == '_') {
                upper = out.length() > 0;
                continue;
            }
            out.append(upper ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
            upper = false;
        }
        return out.toString();
    }

    private static String dashCase(String value) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (Character.isUpperCase(ch)) {
                if (out.length() > 0) out.append('-');
                out.append(Character.toLowerCase(ch));
            } else if (ch == '_') {
                out.append('-');
            } else {
                out.append(Character.toLowerCase(ch));
            }
        }
        return out.toString();
    }

    private static String normalizeName(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeStyleProperty(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("style property must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePseudoElement(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("pseudo element must not be blank");
        String out = value.trim().toLowerCase(Locale.ROOT);
        while (out.startsWith(":")) out = out.substring(1);
        if (out.isBlank()) throw new IllegalArgumentException("pseudo element must not be blank");
        return out;
    }
}
