# HtmlDom

Desktop HTML-like UI library extracted into a standalone multi-module repository.

This is a desktop stack, not browser/WebView: retained DOM, HTML-like markup, CSS cascade/layout, Lua scripting as the JavaScript replacement, font registry, Font Awesome registry, and a Swing/JFrame renderer.

## Modules

- `html-dom-core` — DOM, HTML registry, markup parser, CSS parser/cascade/layout, user-agent styles.
- `html-dom-fonts` — font registry and built-in classpath font registration.
- `html-dom-icons-fontawesome` — Font Awesome icon registry, glyph descriptors, TTF resources and font registration hooks.
- `html-dom-scripting-lua` — Lua runtime and DOM bindings; Lua is used instead of browser JavaScript.
- `html-dom-devtools` — DOM/layout/line/inline/paint/scroll inspection snapshots.
- `html-dom-desktop` — Swing/JFrame renderer and bundled showcase launcher.

## Font resources

Built-in fonts are registered from:

```text
modules/html-dom-fonts/src/main/resources/html-dom/fonts/built-in-fonts.json
```

The bundled registry loads the copied FS Elliot / Roboto Mono font files from `Java2DGame`.

## Font Awesome

Font Awesome TTFs are copied into:

```text
modules/html-dom-icons-fontawesome/src/main/resources/html-dom/icons/fontawesome/
```

The desktop renderer registers them through `FontAwesomeFonts.register(HtmlDomFonts.registry())`, then resolves icon classes such as:

```html
<i class="fa-solid fa-code"></i>
<i class="fa-solid fa-bolt"></i>
```

## Lua instead of JS

Bundled Lua lives at:

```text
modules/html-dom-desktop/src/main/resources/html-dom/bundled/showcase.lua
```

The Swing panel loads it through `HtmlDomLuaRuntime`; scripts can mutate DOM via the exposed `dom` table.

## Build

```bat
gradlew.bat clean compileJava --console=plain --no-daemon
```

## Run desktop showcase

```bat
gradlew.bat :html-dom-desktop:run --console=plain --no-daemon
```

## Build executable showcase jar

```bat
gradlew.bat bundledHtmlUiJar --console=plain --no-daemon
```

Run it:

```bat
java -jar modules\html-dom-desktopuild\libs\html-dom-ui-0.1.0-bundled.jar
```

## Paint tree / scroll containers

The core layout result now carries block boxes, line boxes, inline boxes and scroll boxes. The paint layer is split into physical phases:

```text
paintBackground
paintBorder
paintContent
paintOutline
paintPositionedDescendants
paintScrollbars
```

DevTools snapshots include layout nodes, paint nodes and scroll container nodes with content size, viewport size and scroll offsets.
