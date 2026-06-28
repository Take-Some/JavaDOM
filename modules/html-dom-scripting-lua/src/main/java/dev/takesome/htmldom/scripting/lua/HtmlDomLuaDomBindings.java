package dev.takesome.htmldom.scripting.lua;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
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
        public Varargs invoke(Varargs args) {
            UiDomElement element = element(args.arg(1).tojstring());
            if (element == null) return LuaValue.NIL;
            return switch (command) {
                case "text" -> LuaValue.valueOf(element.textContent());
                case "addClass" -> {
                    element.classList().add(stringArg(args, 2));
                    yield LuaValue.TRUE;
                }
                case "removeClass" -> {
                    element.classList().remove(stringArg(args, 2));
                    yield LuaValue.TRUE;
                }
                case "toggleClass" -> {
                    String token = stringArg(args, 2);
                    if (element.classList().contains(token)) element.classList().remove(token);
                    else element.classList().add(token);
                    yield LuaValue.TRUE;
                }
                case "setText" -> {
                    element.clearChildren();
                    element.appendChild(document.createText(stringArg(args, 2)));
                    yield LuaValue.TRUE;
                }
                case "getAttribute" -> LuaValue.valueOf(element.attribute(stringArg(args, 2), ""));
                case "setAttribute" -> {
                    element.setAttribute(stringArg(args, 2), stringArg(args, 3));
                    yield LuaValue.TRUE;
                }
                default -> LuaValue.NIL;
            };
        }

        private String stringArg(Varargs args, int index) {
            LuaValue value = args.arg(index);
            return value.isnil() ? "" : value.tojstring();
        }

        private UiDomElement element(String id) {
            if (id == null || id.isBlank()) return null;
            String clean = id.startsWith("#") ? id.substring(1) : id;
            return document.getElementById(clean).orElse(null);
        }
    }
}
