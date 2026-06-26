package dev.takesome.htmldom.css.properties.animation;



import dev.takesome.htmldom.css.UiCssDeclaration;

import dev.takesome.htmldom.css.UiCssParseContext;

import dev.takesome.htmldom.css.UiCssShorthandPropertySpec;

import dev.takesome.htmldom.css.UiCssShorthandSupport;

import dev.takesome.htmldom.css.UiCssStringPropertySpec;



import java.util.ArrayList;

import java.util.List;

import java.util.Set;



public final class AnimationCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {

    private static final Set<String> TIMING = Set.of("ease", "linear", "ease-in", "ease-out", "ease-in-out", "step-start", "step-end");

    private static final Set<String> DIRECTIONS = Set.of("normal", "reverse", "alternate", "alternate-reverse");

    private static final Set<String> FILL = Set.of("none", "forwards", "backwards", "both");

    private static final Set<String> PLAY = Set.of("running", "paused");



    public AnimationCssProperty() {

        super("animation", Set.of(), true);

    }



    @Override

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {

        ArrayList<String> names = new ArrayList<>();

        ArrayList<String> durations = new ArrayList<>();

        ArrayList<String> delays = new ArrayList<>();

        ArrayList<String> timings = new ArrayList<>();

        ArrayList<String> iterations = new ArrayList<>();

        ArrayList<String> directions = new ArrayList<>();

        ArrayList<String> fills = new ArrayList<>();

        ArrayList<String> plays = new ArrayList<>();



        for (String group : groups(rawValue)) {

            Parts p = parts(group);

            names.add(p.name.isBlank() ? "none" : p.name);

            durations.add(p.duration.isBlank() ? "0s" : p.duration);

            delays.add(p.delay.isBlank() ? "0s" : p.delay);

            timings.add(p.timing.isBlank() ? "ease" : p.timing);

            iterations.add(p.iteration.isBlank() ? "1" : p.iteration);

            directions.add(p.direction.isBlank() ? "normal" : p.direction);

            fills.add(p.fill.isBlank() ? "none" : p.fill);

            plays.add(p.play.isBlank() ? "running" : p.play);

        }

        if (names.isEmpty()) return List.of();

        return List.of(

                new UiCssDeclaration("animation-name", String.join(", ", names)),

                new UiCssDeclaration("animation-duration", String.join(", ", durations)),

                new UiCssDeclaration("animation-delay", String.join(", ", delays)),

                new UiCssDeclaration("animation-timing-function", String.join(", ", timings)),

                new UiCssDeclaration("animation-iteration-count", String.join(", ", iterations)),

                new UiCssDeclaration("animation-direction", String.join(", ", directions)),

                new UiCssDeclaration("animation-fill-mode", String.join(", ", fills)),

                new UiCssDeclaration("animation-play-state", String.join(", ", plays))

        );

    }



    private Parts parts(String rawValue) {

        Parts p = new Parts();

        for (String token : UiCssShorthandSupport.tokens(rawValue)) {

            String lower = UiCssShorthandSupport.lower(token);

            if (isTime(lower)) {

                if (p.duration.isBlank()) p.duration = token; else if (p.delay.isBlank()) p.delay = token;

            } else if (p.timing.isBlank() && (TIMING.contains(lower) || lower.startsWith("cubic-bezier(") || lower.startsWith("steps("))) {

                p.timing = token;

            } else if (p.iteration.isBlank() && ("infinite".equals(lower) || isNumber(lower))) {

                p.iteration = token;

            } else if (p.direction.isBlank() && DIRECTIONS.contains(lower)) {

                p.direction = token;

            } else if (p.fill.isBlank() && FILL.contains(lower)) {

                p.fill = token;

            } else if (p.play.isBlank() && PLAY.contains(lower)) {

                p.play = token;

            } else if (p.name.isBlank()) {

                p.name = token;

            }

        }

        return p;

    }



    private List<String> groups(String raw) {

        ArrayList<String> out = new ArrayList<>();

        if (raw == null || raw.isBlank()) return out;

        StringBuilder cur = new StringBuilder();

        int depth = 0; char quote = 0;

        for (int i = 0; i < raw.length(); i++) {

            char ch = raw.charAt(i);

            if (quote != 0) { cur.append(ch); if (ch == quote) quote = 0; continue; }

            if (ch == '\'' || ch == '"') { quote = ch; cur.append(ch); continue; }

            if (ch == '(') depth++;

            if (ch == ')' && depth > 0) depth--;

            if (ch == ',' && depth == 0) { add(out, cur); continue; }

            cur.append(ch);

        }

        add(out, cur);

        return out;

    }



    private void add(List<String> out, StringBuilder current) {

        String v = current.toString().trim();

        if (!v.isBlank()) out.add(v);

        current.setLength(0);

    }



    private boolean isTime(String value) { return value.endsWith("ms") || value.endsWith("s"); }



    private boolean isNumber(String value) {

        try { return Double.parseDouble(value) >= 0.0; } catch (RuntimeException ignored) { return false; }

    }



    private static final class Parts {

        String name = ""; String duration = ""; String delay = ""; String timing = "";

        String iteration = ""; String direction = ""; String fill = ""; String play = "";

    }

}
