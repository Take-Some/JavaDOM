package dev.takesome.htmldom.desktop;

import java.util.Objects;

/** Base HtmlDom desktop configuration shared by host panels and DevTools. */
public record HtmlDomConfig(
        DevToolsAvailability allowDevTools,
        DevToolsWindowType devToolsWindowType,
        DevToolsZOrder devToolsZOrder,
        DevToolsClosePolicy devToolsClosePolicy,
        int animationFrameIntervalMs,
        int transientUiResetDelayMs,
        int animationWorkerLimit,
        int preferredWidth,
        int preferredHeight,
        RenderQuality renderQuality
) {
    public HtmlDomConfig(
            DevToolsAvailability allowDevTools,
            DevToolsWindowType devToolsWindowType,
            DevToolsZOrder devToolsZOrder,
            DevToolsClosePolicy devToolsClosePolicy
    ) {
        this(allowDevTools, devToolsWindowType, devToolsZOrder, devToolsClosePolicy, 16, 900, defaultAnimationWorkerLimit(), 900, 700, RenderQuality.HIGH_QUALITY);
    }

    public HtmlDomConfig {
        allowDevTools = Objects.requireNonNullElse(allowDevTools, DevToolsAvailability.ENABLED);
        devToolsWindowType = Objects.requireNonNullElse(devToolsWindowType, DevToolsWindowType.STANDALONE_FRAME);
        devToolsZOrder = Objects.requireNonNullElse(devToolsZOrder, DevToolsZOrder.SAME_LEVEL);
        devToolsClosePolicy = Objects.requireNonNullElse(devToolsClosePolicy, DevToolsClosePolicy.CLOSE_WITH_HOST);
        animationFrameIntervalMs = clamp(animationFrameIntervalMs, 4, 250, 16);
        transientUiResetDelayMs = clamp(transientUiResetDelayMs, 0, 60_000, 900);
        animationWorkerLimit = clamp(animationWorkerLimit, 1, 32, defaultAnimationWorkerLimit());
        preferredWidth = clamp(preferredWidth, 1, 16_384, 900);
        preferredHeight = clamp(preferredHeight, 1, 16_384, 700);
        renderQuality = Objects.requireNonNullElse(renderQuality, RenderQuality.HIGH_QUALITY);
    }

    public static HtmlDomConfig defaults() {
        return new HtmlDomConfig(
                DevToolsAvailability.ENABLED,
                DevToolsWindowType.STANDALONE_FRAME,
                DevToolsZOrder.SAME_LEVEL,
                DevToolsClosePolicy.CLOSE_WITH_HOST
        );
    }

    public boolean devToolsAllowed() {
        return allowDevTools.enabled();
    }

    public HtmlDomConfig withAllowDevTools(DevToolsAvailability value) {
        return new HtmlDomConfig(value, devToolsWindowType, devToolsZOrder, devToolsClosePolicy, animationFrameIntervalMs, transientUiResetDelayMs, animationWorkerLimit, preferredWidth, preferredHeight, renderQuality);
    }

    public HtmlDomConfig withDevToolsWindowType(DevToolsWindowType value) {
        return new HtmlDomConfig(allowDevTools, value, devToolsZOrder, devToolsClosePolicy, animationFrameIntervalMs, transientUiResetDelayMs, animationWorkerLimit, preferredWidth, preferredHeight, renderQuality);
    }

    public HtmlDomConfig withDevToolsZOrder(DevToolsZOrder value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, value, devToolsClosePolicy, animationFrameIntervalMs, transientUiResetDelayMs, animationWorkerLimit, preferredWidth, preferredHeight, renderQuality);
    }

    public HtmlDomConfig withDevToolsClosePolicy(DevToolsClosePolicy value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, devToolsZOrder, value, animationFrameIntervalMs, transientUiResetDelayMs, animationWorkerLimit, preferredWidth, preferredHeight, renderQuality);
    }

    public HtmlDomConfig withAnimationFrameIntervalMs(int value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, devToolsZOrder, devToolsClosePolicy, value, transientUiResetDelayMs, animationWorkerLimit, preferredWidth, preferredHeight, renderQuality);
    }

    public HtmlDomConfig withTransientUiResetDelayMs(int value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, devToolsZOrder, devToolsClosePolicy, animationFrameIntervalMs, value, animationWorkerLimit, preferredWidth, preferredHeight, renderQuality);
    }

    public HtmlDomConfig withAnimationWorkerLimit(int value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, devToolsZOrder, devToolsClosePolicy, animationFrameIntervalMs, transientUiResetDelayMs, value, preferredWidth, preferredHeight, renderQuality);
    }

    @Deprecated(forRemoval = false, since = "1.0.23")
    public int animationWorkerThreads() {
        return animationWorkerLimit;
    }

    @Deprecated(forRemoval = false, since = "1.0.23")
    public HtmlDomConfig withAnimationWorkerThreads(int value) {
        return withAnimationWorkerLimit(value);
    }

    public HtmlDomConfig withPreferredSize(int width, int height) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, devToolsZOrder, devToolsClosePolicy, animationFrameIntervalMs, transientUiResetDelayMs, animationWorkerLimit, width, height, renderQuality);
    }

    public HtmlDomConfig withRenderQuality(RenderQuality value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, devToolsZOrder, devToolsClosePolicy, animationFrameIntervalMs, transientUiResetDelayMs, animationWorkerLimit, preferredWidth, preferredHeight, value);
    }

    private static int defaultAnimationWorkerLimit() {
        return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    }

    private static int clamp(int value, int min, int max, int fallback) {
        int safe = value <= 0 && min > 0 ? fallback : value;
        return Math.max(min, Math.min(max, safe));
    }

    public enum DevToolsAvailability {
        DISABLED,
        ENABLED,
        SYSTEM_PROPERTY;

        private static final String PROPERTY = "htmldom.devtools.enabled";

        boolean enabled() {
            return switch (this) {
                case DISABLED -> false;
                case ENABLED -> true;
                case SYSTEM_PROPERTY -> Boolean.parseBoolean(System.getProperty(PROPERTY, "true"));
            };
        }
    }

    public enum DevToolsWindowType {
        STANDALONE_FRAME,
        OWNERLESS_DIALOG,
        OWNED_DIALOG
    }

    public enum DevToolsZOrder {
        PASSIVE,
        SAME_LEVEL,
        ALWAYS_ON_TOP;

        boolean alwaysOnTop() {
            return this == ALWAYS_ON_TOP;
        }

        boolean bringToFrontOnOpen() {
            return this != PASSIVE;
        }
    }

    public enum DevToolsClosePolicy {
        CLOSE_WITH_HOST,
        KEEP_OPEN_FOR_DEBUG;

        boolean closeWithHost() {
            return this == CLOSE_WITH_HOST;
        }
    }

    public enum RenderQuality {
        SPEED(false, false),
        BALANCED(true, false),
        HIGH_QUALITY(true, true);

        private final boolean antialiasing;
        private final boolean textAntialiasing;

        RenderQuality(boolean antialiasing, boolean textAntialiasing) {
            this.antialiasing = antialiasing;
            this.textAntialiasing = textAntialiasing;
        }

        boolean antialiasing() { return antialiasing; }
        boolean textAntialiasing() { return textAntialiasing; }
    }
}
