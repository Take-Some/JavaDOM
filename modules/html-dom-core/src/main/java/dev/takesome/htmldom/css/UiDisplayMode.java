package dev.takesome.htmldom.css;

/** Typed display mode parsed from CSS display. */
public record UiDisplayMode(boolean hidden, boolean flex, boolean inlineLevel, boolean atomicInline) {
    public boolean blockLevel() {
        return !hidden && !inlineLevel;
    }
}
