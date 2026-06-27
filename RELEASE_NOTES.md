# HtmlDom 1.0.10 — Configurable DevTools Policies

**Authors:** Take Some()

HtmlDom 1.0.10 introduces instance-level desktop configuration for DevTools behavior. DevTools policy is now owned by HtmlDom configuration instead of ad-hoc system properties or host-specific window hacks.

## Release scope

- Added `HtmlDomConfig` as the base desktop runtime configuration object.
- Added enum-driven DevTools policies:
  - `DevToolsAvailability`
  - `DevToolsWindowType`
  - `DevToolsZOrder`
  - `DevToolsClosePolicy`
- `HtmlDomSwingPanel` now accepts `HtmlDomConfig` in constructor overloads while preserving legacy constructors through `HtmlDomConfig.defaults()`.
- DevTools window creation is now config-driven: standalone frame, ownerless dialog, or owned dialog.
- DevTools z-order is now config-driven: passive, same-level, or always-on-top.
- DevTools lifecycle is tied to the inspected host by default through `CLOSE_WITH_HOST`.

## Package coordinates

```gradle
dependencies {
    implementation 'dev.takesome:html-dom-aio:1.0.10'

    implementation 'dev.takesome:html-dom-core:1.0.10'
    implementation 'dev.takesome:html-dom-desktop:1.0.10'
    implementation 'dev.takesome:html-dom-fonts:1.0.10'
    implementation 'dev.takesome:html-dom-icons-fontawesome:1.0.10'
    implementation 'dev.takesome:html-dom-scripting-lua:1.0.10'
    implementation 'dev.takesome:html-dom-devtools:1.0.10'
}
```

## Local verification

```bat
gradlew.bat clean test packageRelease --console=plain --no-daemon
```

## Release trigger

```bat
git tag -a v1.0.10 -m "HtmlDom 1.0.10 — Configurable DevTools Policies"
git push origin main v1.0.10
```
