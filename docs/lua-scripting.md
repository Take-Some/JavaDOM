# Lua scripting ABI

HtmlDom uses Lua instead of browser JavaScript. The Lua runtime is provided by `html-dom-scripting-lua`.

## Runtime location

Bundled showcase scripts live at:

```text
modules/html-dom-desktop/src/main/resources/html-dom/bundled/showcase.lua
```

## Click hooks

The desktop panel calls Lua hooks around native action handling:

```lua
function onClick(action, elementId)
    -- return true to consume native handling
end

function afterClick(action, elementId)
end
```

## Transition hooks

When a transition finishes, DevTools/event runtime can call:

```lua
function onTransitionEnd(propertyName, targetId, currentTargetId, elapsedMs)
end
```

## DOM mutation

The Lua binding exposes a `dom` table for document lookup and mutation. Runtime mutations are visible to cascade/layout/paint on the next repaint cycle.
