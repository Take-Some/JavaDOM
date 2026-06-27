package dev.takesome.htmldom.desktop;

import java.util.Objects;

/** Base HtmlDom desktop configuration shared by host panels and DevTools. */
public record HtmlDomConfig(
        DevToolsAvailability allowDevTools,
        DevToolsWindowType devToolsWindowType,
        DevToolsZOrder devToolsZOrder,
        DevToolsClosePolicy devToolsClosePolicy
) {
    public HtmlDomConfig {
        allowDevTools = Objects.requireNonNullElse(allowDevTools, DevToolsAvailability.ENABLED);
        devToolsWindowType = Objects.requireNonNullElse(devToolsWindowType, DevToolsWindowType.STANDALONE_FRAME);
        devToolsZOrder = Objects.requireNonNullElse(devToolsZOrder, DevToolsZOrder.SAME_LEVEL);
        devToolsClosePolicy = Objects.requireNonNullElse(devToolsClosePolicy, DevToolsClosePolicy.CLOSE_WITH_HOST);
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
        return new HtmlDomConfig(value, devToolsWindowType, devToolsZOrder, devToolsClosePolicy);
    }

    public HtmlDomConfig withDevToolsWindowType(DevToolsWindowType value) {
        return new HtmlDomConfig(allowDevTools, value, devToolsZOrder, devToolsClosePolicy);
    }

    public HtmlDomConfig withDevToolsZOrder(DevToolsZOrder value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, value, devToolsClosePolicy);
    }

    public HtmlDomConfig withDevToolsClosePolicy(DevToolsClosePolicy value) {
        return new HtmlDomConfig(allowDevTools, devToolsWindowType, devToolsZOrder, value);
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
}
