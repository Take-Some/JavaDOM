package dev.takesome.htmldom.css.transition;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stateful CSS3-like transition timeline for computed style maps.
 *
 * <p>The timeline owns per-element transition state. The caller provides the latest target computed style each frame;
 * this class returns the presented/interpolated style for the requested timestamp.</p>
 */
public final class UiCssTransitionTimeline {
    private final Map<String, ElementState> elements = new HashMap<>();

    public Map<String, String> frame(String elementKey, Map<String, String> targetStyle, List<UiCssTransitionDescriptor> descriptors, long nowMs) {
        String key = elementKey == null || elementKey.isBlank() ? "default" : elementKey;
        LinkedHashMap<String, String> target = copy(targetStyle);
        ElementState state = elements.computeIfAbsent(key, ignored -> new ElementState(target));

        if (state.firstFrame) {
            state.firstFrame = false;
            state.targetStyle = target;
            state.presentedStyle = new LinkedHashMap<>(target);
            return new LinkedHashMap<>(target);
        }

        LinkedHashMap<String, String> beforeChange = presentedAt(state, nowMs);
        startChangedTransitions(state, beforeChange, target, descriptors == null ? List.of() : descriptors, nowMs);
        state.targetStyle = target;

        LinkedHashMap<String, String> presented = presentedAt(state, nowMs);
        state.presentedStyle = new LinkedHashMap<>(presented);
        return presented;
    }

    public Map<String, String> frame(String elementKey, Map<String, String> targetStyle, UiCssTransitionDescriptor descriptor, long nowMs) {
        return frame(elementKey, targetStyle, descriptor == null ? List.of() : List.of(descriptor), nowMs);
    }

    public void reset() {
        elements.clear();
    }

    public void reset(String elementKey) {
        if (elementKey != null) elements.remove(elementKey);
    }

    private void startChangedTransitions(ElementState state, Map<String, String> beforeChange, Map<String, String> target, List<UiCssTransitionDescriptor> descriptors, long nowMs) {
        ArrayList<String> properties = new ArrayList<>();
        for (String property : target.keySet()) if (changed(state.targetStyle.get(property), target.get(property))) properties.add(property);
        for (String property : state.targetStyle.keySet()) if (!target.containsKey(property)) properties.add(property);

        for (String property : properties) {
            String to = target.getOrDefault(property, "");
            String from = beforeChange.getOrDefault(property, state.targetStyle.getOrDefault(property, ""));
            UiCssTransitionDescriptor descriptor = matchingDescriptor(property, descriptors);
            if (descriptor == null || !descriptor.active() || !UiCssTransitionInterpolator.interpolable(from, to)) {
                state.active.remove(property);
                continue;
            }
            state.active.put(property, new ActiveTransition(property, from, to, nowMs, descriptor));
        }
    }

    private LinkedHashMap<String, String> presentedAt(ElementState state, long nowMs) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>(state.targetStyle);
        ArrayList<String> finished = new ArrayList<>();
        for (ActiveTransition transition : state.active.values()) {
            long localMs = nowMs - transition.startedAtMs - transition.descriptor.delayMs();
            if (localMs < 0L) {
                out.put(transition.property, transition.from);
                continue;
            }
            if (transition.descriptor.durationMs() <= 0L || localMs >= transition.descriptor.durationMs()) {
                out.put(transition.property, transition.to);
                finished.add(transition.property);
                continue;
            }
            double raw = localMs / (double) transition.descriptor.durationMs();
            double eased = UiCssTransitionTiming.apply(transition.descriptor.timingFunction(), raw);
            out.put(transition.property, UiCssTransitionInterpolator.interpolate(transition.from, transition.to, eased));
        }
        for (String property : finished) state.active.remove(property);
        return out;
    }

    private UiCssTransitionDescriptor matchingDescriptor(String property, List<UiCssTransitionDescriptor> descriptors) {
        UiCssTransitionDescriptor all = null;
        for (UiCssTransitionDescriptor descriptor : descriptors) {
            if (descriptor == null || !descriptor.active()) continue;
            String name = descriptor.property();
            if ("all".equalsIgnoreCase(name)) all = descriptor;
            if (property.equalsIgnoreCase(name)) return descriptor;
        }
        return all;
    }

    private static boolean changed(String oldValue, String newValue) {
        return !Objects.equals(clean(oldValue), clean(newValue));
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }

    private static LinkedHashMap<String, String> copy(Map<String, String> source) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (source == null) return out;
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank()) out.put(key.trim().toLowerCase(java.util.Locale.ROOT), trimToEmpty(value));
        });
        return out;
    }

    private static final class ElementState {
        private LinkedHashMap<String, String> targetStyle;
        private LinkedHashMap<String, String> presentedStyle;
        private final LinkedHashMap<String, ActiveTransition> active = new LinkedHashMap<>();
        private boolean firstFrame = true;

        private ElementState(LinkedHashMap<String, String> initialTarget) {
            this.targetStyle = new LinkedHashMap<>(initialTarget);
            this.presentedStyle = new LinkedHashMap<>(initialTarget);
        }
    }

    private static final class ActiveTransition {
        private final String property;
        private final String from;
        private final String to;
        private final long startedAtMs;
        private final UiCssTransitionDescriptor descriptor;

        private ActiveTransition(String property, String from, String to, long startedAtMs, UiCssTransitionDescriptor descriptor) {
            this.property = property;
            this.from = from;
            this.to = to;
            this.startedAtMs = startedAtMs;
            this.descriptor = descriptor;
        }
    }
}
