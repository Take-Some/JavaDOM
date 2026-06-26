package dev.takesome.htmldom.css.transition;

public record UiCssTransitionDescriptor(
        String property,
        long durationMs,
        long delayMs,
        String timingFunction
) {
    public UiCssTransitionDescriptor {
        property = property == null || property.isBlank() ? "all" : property.trim();
        timingFunction = timingFunction == null || timingFunction.isBlank() ? "ease" : timingFunction.trim();
        durationMs = Math.max(0L, durationMs);
        delayMs = Math.max(0L, delayMs);
    }

    public boolean active() {
        return durationMs > 0L && !"none".equalsIgnoreCase(property);
    }
}
