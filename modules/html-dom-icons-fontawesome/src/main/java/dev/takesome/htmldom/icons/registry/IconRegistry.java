package dev.takesome.htmldom.icons.registry;

import dev.takesome.htmldom.icons.UiIcon;
import dev.takesome.htmldom.icons.internal.IconKeys;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime registry for icon-font glyph descriptors.
 *
 * <p>Exact icon ids are strict and unique. Friendly aliases are intentionally
 * first-wins so a standard bundled registry can contain solid, regular and
 * brands glyphs with overlapping symbolic names. Use exact ids such as
 * {@code fontawesome:regular:circle-check} when the style matters.</p>
 */
public final class IconRegistry {
    private final String name;
    private final Map<String, UiIcon> iconsById = new LinkedHashMap<>();
    private final Map<String, UiIcon> iconsByKey = new LinkedHashMap<>();

    private IconRegistry(String name) {
        this.name = IconKeys.key(name).isEmpty() ? "icons" : IconKeys.key(name);
    }

    public static IconRegistry create(String name) {
        return new IconRegistry(name);
    }

    public synchronized IconRegistry register(UiIcon icon) {
        if (icon == null) {
            return this;
        }

        String id = requireKey(icon.id(), "icon id");
        UiIcon existing = iconsById.get(id);
        if (existing != null && !sameIcon(existing, icon)) {
            throw new IllegalArgumentException("Duplicate icon id '" + icon.id() + "' already points to " + existing);
        }

        iconsById.putIfAbsent(id, icon);
        bindStrictAlias(icon.id(), icon);
        bindFriendlyAlias(icon.symbolicName(), icon);
        bindStrictAlias(icon.familyId() + ":" + icon.styleId() + ":" + icon.symbolicName(), icon);
        bindStrictAlias(icon.familyId() + "-" + icon.styleId() + "-" + icon.symbolicName(), icon);

        if (icon instanceof Enum<?>) {
            bindFriendlyAlias(((Enum<?>) icon).name(), icon);
        }

        return this;
    }

    public synchronized IconRegistry registerAll(Collection<? extends UiIcon> icons) {
        if (icons == null) {
            return this;
        }
        for (UiIcon icon : icons) {
            register(icon);
        }
        return this;
    }

    public synchronized IconRegistry merge(IconRegistry registry) {
        if (registry == null) {
            return this;
        }
        return registerAll(registry.iconsView().values());
    }

    public synchronized Optional<UiIcon> find(String idOrAlias) {
        String key = IconKeys.key(idOrAlias);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(iconsByKey.get(key));
    }

    public synchronized UiIcon require(String idOrAlias) {
        return find(idOrAlias).orElseThrow(() -> new IllegalArgumentException(
                "Unknown icon '" + idOrAlias + "' in registry '" + name + "'. Available ids: " + iconsById.keySet()
        ));
    }

    public synchronized boolean contains(String idOrAlias) {
        return find(idOrAlias).isPresent();
    }

    public synchronized int size() {
        return iconsById.size();
    }

    public String name() {
        return name;
    }

    public synchronized Map<String, UiIcon> iconsView() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(iconsById));
    }

    private void bindStrictAlias(String alias, UiIcon icon) {
        String key = IconKeys.key(alias);
        if (key.isEmpty()) {
            return;
        }

        UiIcon existing = iconsByKey.get(key);
        if (existing != null && !sameIcon(existing, icon)) {
            throw new IllegalArgumentException(
                    "Duplicate strict icon alias '" + alias + "' already points to " + existing.id()
            );
        }

        iconsByKey.putIfAbsent(key, icon);
    }

    private void bindFriendlyAlias(String alias, UiIcon icon) {
        String key = IconKeys.key(alias);
        if (key.isEmpty()) {
            return;
        }
        iconsByKey.putIfAbsent(key, icon);
    }

    private static String requireKey(String value, String label) {
        String key = IconKeys.key(value);
        if (key.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return key;
    }

    private static boolean sameIcon(UiIcon left, UiIcon right) {
        return left.id().equals(right.id()) && left.codePoint() == right.codePoint();
    }
}
