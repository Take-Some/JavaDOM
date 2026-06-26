package dev.takesome.htmldom.css.transition;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.lowerTrimToEmpty;
import static dev.takesome.htmldom.support.validation.HtmlDomValidator.textOrEmpty;
import dev.takesome.htmldom.css.UiCssPropertyRegistry;
import dev.takesome.htmldom.css.UiCssTokenSplitter;
import dev.takesome.htmldom.css.properties.transition.TransitionDelayCssProperty;
import dev.takesome.htmldom.css.properties.transition.TransitionDurationCssProperty;
import dev.takesome.htmldom.css.properties.transition.TransitionPropertyCssProperty;
import dev.takesome.htmldom.css.properties.transition.TransitionTimingFunctionCssProperty;
import dev.takesome.htmldom.dom.UiDomElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Resolves native CSS transition-* computed properties into engine transition descriptors. */
public final class UiCssTransitionResolver {
    private final TransitionPropertyCssProperty property;
    private final TransitionDurationCssProperty duration;
    private final TransitionDelayCssProperty delay;
    private final TransitionTimingFunctionCssProperty timingFunction;

    public UiCssTransitionResolver() {
        this(UiCssPropertyRegistry.loadBuiltins());
    }

    public UiCssTransitionResolver(UiCssPropertyRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        this.property = registry.requireType(TransitionPropertyCssProperty.class);
        this.duration = registry.requireType(TransitionDurationCssProperty.class);
        this.delay = registry.requireType(TransitionDelayCssProperty.class);
        this.timingFunction = registry.requireType(TransitionTimingFunctionCssProperty.class);
    }

    public UiCssTransitionDescriptor resolve(UiDomElement element) {
        List<UiCssTransitionDescriptor> all = resolveAll(element);
        return all.isEmpty() ? new UiCssTransitionDescriptor("all", 0L, 0L, "ease") : all.get(0);
    }

    public List<UiCssTransitionDescriptor> resolveAll(UiDomElement element) {
        List<String> properties = UiCssTokenSplitter.splitTopLevelComma(value(element, property.name(), "all"));
        List<String> durations = UiCssTokenSplitter.splitTopLevelComma(value(element, duration.name(), "0ms"));
        List<String> delays = UiCssTokenSplitter.splitTopLevelComma(value(element, delay.name(), "0ms"));
        List<String> timingFunctions = UiCssTokenSplitter.splitTopLevelComma(value(element, timingFunction.name(), "ease"));
        int count = Math.max(1, properties.size());
        ArrayList<UiCssTransitionDescriptor> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String p = item(properties, i, "all");
            out.add(new UiCssTransitionDescriptor(
                    p,
                    timeMillis(item(durations, i, item(durations, 0, "0ms"))),
                    timeMillis(item(delays, i, item(delays, 0, "0ms"))),
                    item(timingFunctions, i, item(timingFunctions, 0, "ease"))
            ));
        }
        return List.copyOf(out);
    }


    public List<UiCssTransitionDescriptor> resolveAll(Map<String, String> style) {
        List<String> properties = UiCssTokenSplitter.splitTopLevelComma(value(style, property.name(), "all"));
        List<String> durations = UiCssTokenSplitter.splitTopLevelComma(value(style, duration.name(), "0ms"));
        List<String> delays = UiCssTokenSplitter.splitTopLevelComma(value(style, delay.name(), "0ms"));
        List<String> timingFunctions = UiCssTokenSplitter.splitTopLevelComma(value(style, timingFunction.name(), "ease"));
        int count = Math.max(1, properties.size());
        ArrayList<UiCssTransitionDescriptor> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(new UiCssTransitionDescriptor(
                    item(properties, i, "all"),
                    timeMillis(item(durations, i, item(durations, 0, "0ms"))),
                    timeMillis(item(delays, i, item(delays, 0, "0ms"))),
                    item(timingFunctions, i, item(timingFunctions, 0, "ease"))
            ));
        }
        return List.copyOf(out);
    }

    private String value(Map<String, String> style, String property, String fallback) {
        if (style == null || property == null) return fallback;
        String value = style.getOrDefault(property, "");
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String value(UiDomElement element, String property, String fallback) {
        String value = textOrEmpty(element, item -> item.style(property, ""));
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String item(List<String> values, int index, String fallback) {
        if (values == null || values.isEmpty()) return fallback;
        if (index < values.size()) return values.get(index);
        return fallback;
    }

    private long timeMillis(String raw) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank()) return 0L;
        try {
            if (value.endsWith("ms")) return Math.max(0L, Math.round(Double.parseDouble(value.substring(0, value.length() - 2).trim())));
            if (value.endsWith("s")) return Math.max(0L, Math.round(Double.parseDouble(value.substring(0, value.length() - 1).trim()) * 1000.0));
            return Math.max(0L, Math.round(Double.parseDouble(value)));
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }
}
