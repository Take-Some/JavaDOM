package dev.takesome.htmldom.scripting.lua;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

/** DOM functions exposed to Lua scripts. */
public final class HtmlDomLuaDomBindings {
    private HtmlDomLuaDomBindings() {
    }

    public static void install(Globals globals, UiDomDocument document) {
        LuaTable dom = new LuaTable();
        dom.set("addClass", new DomFunction(document, "addClass"));
        dom.set("removeClass", new DomFunction(document, "removeClass"));
        dom.set("toggleClass", new DomFunction(document, "toggleClass"));
        dom.set("setAttribute", new DomFunction(document, "setAttribute"));
        dom.set("getAttribute", new DomFunction(document, "getAttribute"));
        dom.set("setText", new DomFunction(document, "setText"));
        dom.set("text", new DomFunction(document, "text"));
        globals.set("dom", dom);
    }

    private static final class DomFunction extends VarArgFunction {
        private final UiDomDocument document;
        private final String command;

        private DomFunction(UiDomDocument document, String command) {
            this.document = document;
            this.command = command;
        }

        @Override
        public LuaValue call() {
            return LuaValue.NIL;
        }

        @Override
        public LuaValue call(LuaValue id) {
            UiDomElement element = element(id.checkjstring());
            if (element == null) return LuaValue.NIL;
            return switch (command) {
                case "text" -> LuaValue.valueOf(element.textContent());
                default -> LuaValue.NIL;
            };
        }

        @Override
        public LuaValue call(LuaValue id, LuaValue value) {
            UiDomElement element = element(id.checkjstring());
            if (element == null) return LuaValue.NIL;
            String text = value.isnil() ? "" : value.tojstring();
            switch (command) {
                case "addClass" -> element.classList().add(text);
                case "removeClass" -> element.classList().remove(text);
                case "toggleClass" -> {
                    if (element.classList().contains(text)) element.classList().remove(text);
                    else element.classList().add(text);
                }
                case "setText" -> {
                    element.clearChildren();
                    element.appendChild(document.createText(text));
                }
                case "getAttribute" -> { return LuaValue.valueOf(element.attribute(text, "")); }
                default -> { }
            }
            return LuaValue.TRUE;
        }

        @Override
        public LuaValue call(LuaValue id, LuaValue name, LuaValue value) {
            UiDomElement element = element(id.checkjstring());
            if (element == null) return LuaValue.NIL;
            if ("setAttribute".equals(command)) {
                element.setAttribute(name.checkjstring(), value.isnil() ? "" : value.tojstring());
                return LuaValue.TRUE;
            }
            return LuaValue.NIL;
        }

        private UiDomElement element(String id) {
            if (id == null || id.isBlank()) return null;
            String clean = id.startsWith("#") ? id.substring(1) : id;
            return document.getElementById(clean).orElse(null);
        }
    }
}
