# CSS, layout and paint pipeline

HtmlDom implements a pragmatic desktop CSS pipeline. The goal is deterministic retained UI rendering, not full browser compatibility.

## Pipeline

```text
CSS text
  -> parser
  -> rule/declaration model
  -> cascade
  -> computed style on UiDomElement
  -> layout result
  -> paint tree / Java2D rendering
```

## Layout output

`UiCssLayoutResult` stores:

```text
block boxes
line boxes
inline boxes
scroll boxes
```

The desktop renderer uses this data for paint, hit testing, scrollbars and DevTools snapshots.

## Pseudo-state

Pseudo-state is stored on DOM elements as runtime pseudo flags, not by mutating `classList`.

Supported runtime pseudo flags include:

```text
:hover
:active
:focus
:focus-visible
:focus-within
```

This keeps author classes stable while allowing CSS selectors to react to input state.

## Runtime style editing

DevTools can edit computed values at runtime. The edit is applied as an inline `style` override, then the normal cascade/layout pipeline recalculates the result. This makes the edited value visible to Undo/Redo/Save and to subsequent repaints.

## Scroll containers

Scroll metadata is held separately from the DOM. Paint phases and hit-test use scroll-adjusted graphics contexts when painting scrollable children and scrollbar thumbs.
