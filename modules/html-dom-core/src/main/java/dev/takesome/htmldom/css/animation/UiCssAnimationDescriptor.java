package dev.takesome.htmldom.css.animation;



public record UiCssAnimationDescriptor(

        String name, long durationMs, long delayMs, String timingFunction, double iterationCount,

        String direction, String fillMode, String playState

) {

    public UiCssAnimationDescriptor {

        name = name == null || name.isBlank() ? "none" : name.trim();

        durationMs = Math.max(0L, durationMs);

        timingFunction = timingFunction == null || timingFunction.isBlank() ? "ease" : timingFunction.trim();

        iterationCount = Double.isFinite(iterationCount) ? iterationCount : -1.0;

        direction = direction == null || direction.isBlank() ? "normal" : direction.trim().toLowerCase(java.util.Locale.ROOT);

        fillMode = fillMode == null || fillMode.isBlank() ? "none" : fillMode.trim().toLowerCase(java.util.Locale.ROOT);

        playState = playState == null || playState.isBlank() ? "running" : playState.trim().toLowerCase(java.util.Locale.ROOT);

    }

    public boolean infinite() { return iterationCount < 0.0; }

    public boolean active() { return durationMs > 0L && !"none".equalsIgnoreCase(name); }

}
