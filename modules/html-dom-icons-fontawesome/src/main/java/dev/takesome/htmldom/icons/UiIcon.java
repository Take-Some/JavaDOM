package dev.takesome.htmldom.icons;

/**
 * Backend-neutral glyph descriptor for icon fonts.
 *
 * <p>The engine should pass icon ids or UiIcon instances around instead of raw
 * unicode literals such as "\uf013". This keeps UI definitions stable when the
 * icon font, style or glyph source changes.</p>
 */
public interface UiIcon {
    String id();

    String familyId();

    String styleId();

    String symbolicName();

    int codePoint();

    default String text() {
        return new String(Character.toChars(codePoint()));
    }
}
