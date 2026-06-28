package dev.takesome.htmldom.desktop;

/**
 * Coarse HtmlDom document lifecycle phases, intentionally mirroring the
 * browser-style split between style recalc, animation/effects, layout, and
 * painting without copying Blink internals.
 */
enum HtmlDomLifecyclePhase {
    IDLE,
    STYLE_RECALC,
    ANIMATION_UPDATE,
    LAYOUT,
    PAINT
}
