package dev.takesome.htmldom.css;




import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import dev.takesome.htmldom.css.transition.UiCssTransitionInterpolator;



import java.util.ArrayList;

import java.util.Comparator;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;



public final class UiCssKeyframesRule {

    private final String name;

    private final List<UiCssKeyframe> frames;



    public UiCssKeyframesRule(String name, List<UiCssKeyframe> frames) {

        this.name = trimToEmpty(name);

        ArrayList<UiCssKeyframe> sorted = new ArrayList<>(frames == null ? List.of() : frames);

        sorted.sort(Comparator.comparingDouble(UiCssKeyframe::offset));

        this.frames = List.copyOf(sorted);

    }



    public String name() { return name; }

    public List<UiCssKeyframe> frames() { return frames; }

    public boolean empty() { return name.isBlank() || frames.isEmpty(); }



    public Map<String, String> sample(double progress) {

        if (frames.isEmpty()) return Map.of();

        double t = Math.max(0.0, Math.min(1.0, progress));

        UiCssKeyframe prev = frames.get(0);

        UiCssKeyframe next = frames.get(frames.size() - 1);

        for (UiCssKeyframe frame : frames) {

            if (frame.offset() <= t) prev = frame;

            if (frame.offset() >= t) { next = frame; break; }

        }

        if (prev == next || Math.abs(next.offset() - prev.offset()) < 0.000001) return prev.declarations();

        double local = (t - prev.offset()) / (next.offset() - prev.offset());

        LinkedHashMap<String, String> out = new LinkedHashMap<>();

        out.putAll(prev.declarations());

        for (Map.Entry<String, String> entry : next.declarations().entrySet()) {

            String property = entry.getKey();

            String from = prev.declarations().get(property);

            String to = entry.getValue();

            if (from != null && UiCssTransitionInterpolator.interpolable(from, to)) {

                out.put(property, UiCssTransitionInterpolator.interpolate(from, to, local));

            } else {

                out.put(property, local < 1.0 ? (from == null ? to : from) : to);

            }

        }

        return out;

    }

}
