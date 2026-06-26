package dev.takesome.htmldom.markup.internal.parse.scanner;

import java.util.Map;

/** Parsed start-tag token payload before token materialization. */
record UiHtmlStartTagData(String name, Map<String, String> attributes, boolean selfClosing) {
}
