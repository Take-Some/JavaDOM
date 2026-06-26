package dev.takesome.htmldom.scripting.lua;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomText;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

/** Lua runtime for HtmlDom. Lua is the desktop replacement for browser JavaScript. */
public final class HtmlDomLuaRuntime {
    private final UiDomDocument document;
    private final Globals globals;

    public HtmlDomLuaRuntime(UiDomDocument document) {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        this.document = document;
        this.globals = JsePlatform.standardGlobals();
        HtmlDomLuaDomBindings.install(globals, document);
    }

    public UiDomDocument document() {
        return document;
    }

    public void execute(String source, String chunkName) {
        if (source == null || source.isBlank()) return;
        globals.load(source, chunkName == null || chunkName.isBlank() ? "html-dom.lua" : chunkName).call();
    }

    public boolean call(String functionName, String... args) {
        if (functionName == null || functionName.isBlank()) return false;
        LuaValue fn = globals.get(functionName);
        if (fn.isnil() || !fn.isfunction()) return false;
        LuaValue[] values = new LuaValue[args == null ? 0 : args.length];
        for (int i = 0; i < values.length; i++) values[i] = LuaValue.valueOf(args[i] == null ? "" : args[i]);
        try {
            fn.invoke(values);
            return true;
        } catch (LuaError error) {
            throw new IllegalStateException("Lua function failed: " + functionName, error);
        }
    }
}
