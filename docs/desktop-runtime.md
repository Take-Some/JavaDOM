# Desktop runtime

The desktop runtime lives in `html-dom-desktop`. It hosts a retained DOM document inside a Swing `JPanel` and renders through Java2D.

## Main entry points

| Class | Purpose |
| --- | --- |
| `BundledHtmlUi` | Executable showcase launcher. |
| `HtmlDomSwingPanel` | Main host panel. Owns DOM, stylesheet, layout, Lua runtime and paint orchestration. |
| `HtmlDomInputRouter` | Mouse, keyboard, scroll, pointer and click routing. |
| `HtmlDomPaintEngine` | Paint helpers for background, border, outlines, shadows and paint metadata. |
| `HtmlDomTextPaintEngine` | Text and inline-box painting. |
| `HtmlDomControlPaintEngine` | Controls such as inputs, selects, progress and button content. |
| `HtmlDomTransformEngine` | Applies CSS transform-like operations to paint and hit-test coordinates. |
| `HtmlDomTransitionController` | Runtime interpolation for paint-only transitions. |

## Transform support

`HtmlDomTransformEngine` supports the practical subset used by the desktop renderer:

```text
translate / translateX / translateY
scale / scaleX / scaleY
rotate
matrix
transform-origin
```

Transforms are applied before background, border, content, outline and scrollbars. Hit-test shapes are registered under the same transformed graphics context.

## Transition support

`HtmlDomTransitionController` interpolates runtime style changes for:

```text
transform
opacity
color
border-color
background-color
```

When a transition finishes, `transitionend` is dispatched through the event dispatcher with property name and elapsed time.

## Pointer and mouse model

The input router maintains the current pointer target and dispatches:

```text
pointermove / mousemove
pointerover / mouseover
pointerout / mouseout
pointerenter / mouseenter
pointerleave / mouseleave
```

`pointer-events: none` is enforced in hit-test registration.

## DevTools integration

When DevTools highlights a node, `HtmlDomSwingPanel` paints a Chrome-like overlay over the live interface. The overlay includes selector, dimensions, content/padding/border/margin visualization and direct-child outlines.
