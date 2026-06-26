package dev.takesome.htmldom.icons.fontawesome;

import dev.takesome.htmldom.icons.registry.IconRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Descriptor for one Font Awesome enum-backed icon family.
 */
public final class FontAwesomeIconFamily<T extends Enum<T> & FontAwesomeIcon> {
    private final String styleId;
    private final String registryName;
    private final Supplier<T[]> valuesSupplier;
    private volatile List<T> icons;
    private volatile IconRegistry registry;

    private FontAwesomeIconFamily(String styleId, String registryName, Supplier<T[]> valuesSupplier) {
        this.styleId = clean(styleId, "styleId");
        this.registryName = clean(registryName, "registryName");
        if (valuesSupplier == null) throw new IllegalArgumentException("valuesSupplier must not be null");
        this.valuesSupplier = valuesSupplier;
    }

    public static <T extends Enum<T> & FontAwesomeIcon> FontAwesomeIconFamily<T> of(
            String styleId,
            String registryName,
            Supplier<T[]> valuesSupplier
    ) {
        return new FontAwesomeIconFamily<>(styleId, registryName, valuesSupplier);
    }

    public String styleId() {
        return styleId;
    }

    public String registryName() {
        return registryName;
    }

    public List<T> icons() {
        List<T> cached = icons;
        if (cached != null) return cached;
        synchronized (this) {
            cached = icons;
            if (cached == null) {
                cached = Collections.unmodifiableList(Arrays.asList(valuesSupplier.get()));
                icons = cached;
            }
            return cached;
        }
    }

    public IconRegistry registry() {
        IconRegistry cached = registry;
        if (cached != null) return cached;
        synchronized (this) {
            cached = registry;
            if (cached == null) {
                cached = IconRegistry.create(registryName).registerAll(icons());
                registry = cached;
            }
            return cached;
        }
    }

    private static String clean(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value.trim();
    }
}
