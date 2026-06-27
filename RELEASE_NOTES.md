# HtmlDom 1.0.10 — Configurable DevTools and Layout Architecture

**Authors:** Take Some()

HtmlDom 1.0.10 is an architecture release for DevTools policy configuration and the CSS/layout rendering pipeline.

## DevTools architecture

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

## CSS / layout architecture

- Split large layout responsibilities out of `UiCssLayoutEngine` into dedicated engines.
- Added box sizing, flex layout, inline formatting, and scroll extent helpers.
- Added named color and border style helpers for desktop painting.
- Reduced direct layout/paint coupling and prepared the renderer for more complete HTML/CSS behavior.

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
gradlew.bat :html-dom-desktop:compileJava :html-dom-desktop:aioJar --console=plain
```

## Release trigger

```bat
git tag -a v1.0.10 -m "HtmlDom 1.0.10 — Configurable DevTools and Layout Architecture"
git push origin main v1.0.10
```
