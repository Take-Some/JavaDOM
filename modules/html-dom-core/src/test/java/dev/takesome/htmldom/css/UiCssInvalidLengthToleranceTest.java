package dev.takesome.htmldom.css;

import dev.takesome.htmldom.css.properties.layout.GapCssProperty;
import dev.takesome.htmldom.markup.UiMarkupDocument;
import dev.takesome.htmldom.markup.UiMarkupParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class UiCssInvalidLengthToleranceTest {
    @Test
    void lengthPropertyFallsBackInsteadOfThrowingOnMultiTokenValue() {
        UiMarkupDocument markup = new UiMarkupParser().parse("<html><body><section></section></body></html>");
        var section = markup.dom().querySelector("section").orElseThrow();
        section.setComputedStyle("gap", "5px 10");

        UiCssLength value = assertDoesNotThrow(() -> new GapCssProperty().read(section, UiCssLength.ZERO));

        assertEquals(UiCssLength.ZERO, value);
    }

    @Test
    void layoutDoesNotThrowWhenComputedLengthIsMalformed() {
        UiMarkupDocument markup = new UiMarkupParser().parse("""
                <html>
                  <body>
                    <section class="wrap">
                      <section>Alpha</section>
                      <section>Beta</section>
                    </section>
                  </body>
                </html>
                """);
        UiStylesheet stylesheet = UiCssUserAgentStylesheet.stylesheet().plus(new UiCssParser().parse("""
                .wrap {
                    display: flex;
                    flex-direction: column;
                    gap: 5px 10;
                    width: 200px;
                    height: fit-content;
                }
                """));
        new UiCssCascade().apply(markup.dom(), stylesheet);

        assertDoesNotThrow(() -> new UiCssLayoutEngine().layout(markup.dom(), 640, 480));
    }
}
