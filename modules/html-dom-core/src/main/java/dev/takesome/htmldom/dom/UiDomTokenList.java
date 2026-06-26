package dev.takesome.htmldom.dom;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Token list wrapper used for the {@code class} attribute. */
public final class UiDomTokenList {
    private final UiDomElement element;

    UiDomTokenList(UiDomElement element) {
        this.element = element;
    }

    public boolean contains(String token) {
        return tokens().contains(normalize(token));
    }

    public void add(String token) {
        LinkedHashSet<String> next = new LinkedHashSet<>(tokens());
        next.add(normalize(token));
        write(next);
    }

    public void remove(String token) {
        LinkedHashSet<String> next = new LinkedHashSet<>(tokens());
        next.remove(normalize(token));
        write(next);
    }

    public void toggle(String token) {
        LinkedHashSet<String> next = new LinkedHashSet<>(tokens());
        String normalized = normalize(token);
        if (!next.remove(normalized)) next.add(normalized);
        write(next);
    }

    public List<String> values() {
        return List.copyOf(tokens());
    }

    private Set<String> tokens() {
        String raw = element.attribute("class", "");
        if (raw.isBlank()) return Set.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Arrays.stream(raw.trim().split("\\s+")).map(this::normalize).filter(value -> !value.isBlank()).forEach(out::add);
        return out;
    }

    private void write(Set<String> tokens) {
        element.setAttribute("class", String.join(" ", tokens));
    }

    private String normalize(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("class token must not be blank");
        return token.trim().toLowerCase(Locale.ROOT);
    }
}
