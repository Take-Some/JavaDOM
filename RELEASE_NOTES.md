# HtmlDom 1.0.11 — Extracted CSS Positioning Engine

**Authors:** Take Some()

HtmlDom 1.0.11 continues the layout architecture split by moving explicit, relative, and out-of-flow positioning responsibilities out of `UiCssLayoutEngine`.

## Release scope

- Added `UiCssPositioningEngine` in `html-dom-core`.
- Moved explicit bounds resolution into the positioning engine.
- Moved `x` / `y`, `left` / `top` / `right` / `bottom` offset resolution into the positioning engine.
- Moved `position: relative` offset handling into the positioning engine.
- Moved `position: absolute` and `position: fixed` out-of-flow box resolution into the positioning engine.
- Moved containing-block detection for positioned elements into the positioning engine.
- Reduced `UiCssLayoutEngine` responsibility to layout orchestration and delegation across flow, flex, inline, sizing, scroll, and positioning engines.

## Package coordinates

```gradle
dependencies {
    implementation 'dev.takesome:html-dom-aio:1.0.11'

    implementation 'dev.takesome:html-dom-core:1.0.11'
    implementation 'dev.takesome:html-dom-desktop:1.0.11'
    implementation 'dev.takesome:html-dom-fonts:1.0.11'
    implementation 'dev.takesome:html-dom-icons-fontawesome:1.0.11'
    implementation 'dev.takesome:html-dom-scripting-lua:1.0.11'
    implementation 'dev.takesome:html-dom-devtools:1.0.11'
}
```

## Local verification

```bat
gradlew.bat :html-dom-core:compileJava :html-dom-desktop:compileJava :html-dom-desktop:aioJar --console=plain
```

## Release trigger

```bat
git tag -a v1.0.11 -m "HtmlDom 1.0.11 — Extracted CSS Positioning Engine"
git push origin main v1.0.11
```
