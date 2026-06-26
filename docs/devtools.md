# DevTools

HtmlDom DevTools is a custom Java2D inspector. It is not a Swing `JTree`/`JTable` sheet and it is not a browser DevTools embed. It is painted by HtmlDom desktop code.

## Opening DevTools

Press `F12` in the desktop showcase.

## Elements viewer

The left panel is a Chrome-like DOM list with:

```text
expand/collapse nodes
Font Awesome chevrons
row hover
row selection
right-click context menu
live interface highlight
```

The live highlight paints selector and dimensions above the selected block and visualizes the box model.

## Right-side panels

DevTools currently exposes:

| Panel | Purpose |
| --- | --- |
| `Styles` | Inline style view. |
| `Computed` | Runtime editable computed values. |
| `HTML` | Editable HTML source for the selected node. |
| `Layout` | Layout, hit-test, scroll and paint metadata. |
| `Attributes` | Node attributes, node type and pseudo-state. |
| `Events` | Event log from the dispatcher. |
| `Path` | DOM path for the selected node. |

## HTML editor

`Edit as HTML` opens a text editor in the right panel. It supports:

```text
line numbers
row grid
selection
caret movement
syntax highlighting
Ctrl+Enter apply
Esc cancel
Apply / Cancel painted buttons
```

Applying HTML reparses the edited source and replaces the selected node in the retained DOM.

## Undo / Redo / Save

DevTools keeps DOM snapshots for editing operations. `Save` serializes the current DOM back to the bundled showcase HTML file when available.

## Event log

The event log stores type, target, current target, phase, bubbling flag, default-prevented flag and composed path.
