-- HtmlDom Lua: replacement for browser JavaScript.
-- The desktop panel calls afterClick(action, id) after native DOM actions.
function afterClick(action, id)
    if action == "pulse" then
        dom.setText("toast", "Lua handled pulse from #" .. id)
    end
end
