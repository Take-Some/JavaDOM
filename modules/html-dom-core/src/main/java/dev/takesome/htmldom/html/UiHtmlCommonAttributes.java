package dev.takesome.htmldom.html;



import java.util.LinkedHashSet;

import java.util.Set;



/** Shared attribute groups for HTML-like tag definitions. */

public final class UiHtmlCommonAttributes {

    private static final Set<String> COMMON = Set.of("id", "class", "style", "title", "data-*");

    private static final Set<String> ACTION = Set.of("data-command", "data-action", "data-target-id", "event", "action", "command");

    private static final Set<String> BINDING = Set.of("bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style", "bind-x", "bind-y", "bind-w", "bind-h");

    private static final Set<String> ROOT = Set.of("stylesheet", "ui-skins", "ui-chrome", "chrome", "skins", "skin-set", "skinset");

    private static final Set<String> INPUT = Set.of("type", "name", "value", "checked", "disabled", "placeholder", "readonly", "required", "min", "max", "step");

    private static final Set<String> MEDIA = Set.of("src", "alt", "width", "height");

    private static final Set<String> ICON = Set.of("x", "y", "w", "h", "width", "height", "icon", "fa", "font-awesome", "fontAwesome", "color", "icon-color", "scale", "icon-scale", "opacity");



    private UiHtmlCommonAttributes() {

    }



    public static Set<String> common(String... extra) {

        return merge(COMMON, extra);

    }



    public static Set<String> actionable(String... extra) {

        return merge(COMMON, ACTION, extra);

    }



    public static Set<String> bindable(String... extra) {

        return merge(COMMON, ACTION, BINDING, extra);

    }



    public static Set<String> root(String... extra) {

        return merge(COMMON, ACTION, BINDING, ROOT, extra);

    }



    public static Set<String> interactiveControl(String... extra) {

        return merge(COMMON, ACTION, BINDING, extra);

    }



    public static Set<String> inputControl(String... extra) {

        return merge(COMMON, ACTION, BINDING, INPUT, extra);

    }



    public static Set<String> media(String... extra) {

        return merge(COMMON, ACTION, BINDING, MEDIA, extra);

    }



    public static Set<String> icon(String... extra) {

        return merge(COMMON, ICON, extra);

    }



    public static Set<String> styleRaw(String... extra) {

        return merge(COMMON, extra);

    }



    private static Set<String> merge(Set<String> first, String... extra) {

        LinkedHashSet<String> out = new LinkedHashSet<>();

        if (first != null) out.addAll(first);

        add(out, extra);

        return Set.copyOf(out);

    }



    @SafeVarargs

    private static Set<String> merge(Set<String> first, Set<String> second, String... extra) {

        LinkedHashSet<String> out = new LinkedHashSet<>();

        if (first != null) out.addAll(first);

        if (second != null) out.addAll(second);

        add(out, extra);

        return Set.copyOf(out);

    }



    private static Set<String> merge(Set<String> first, Set<String> second, Set<String> third, String... extra) {

        LinkedHashSet<String> out = new LinkedHashSet<>();

        if (first != null) out.addAll(first);

        if (second != null) out.addAll(second);

        if (third != null) out.addAll(third);

        add(out, extra);

        return Set.copyOf(out);

    }



    private static Set<String> merge(Set<String> first, Set<String> second, Set<String> third, Set<String> fourth, String... extra) {

        LinkedHashSet<String> out = new LinkedHashSet<>();

        if (first != null) out.addAll(first);

        if (second != null) out.addAll(second);

        if (third != null) out.addAll(third);

        if (fourth != null) out.addAll(fourth);

        add(out, extra);

        return Set.copyOf(out);

    }



    private static void add(LinkedHashSet<String> out, String... extra) {

        if (extra == null) return;

        for (String value : extra) if (value != null && !value.isBlank()) out.add(value.trim());

    }

}
