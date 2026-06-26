package dev.takesome.htmldom.runtime;

import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.dom.UiDomDocument;

/**
 * Public neutral inspection source for editor/devtools adapters.
 *
 * <p>This is intentionally not an editor dependency. Runtime modules can expose
 * this object without depending on {@code :editor}; the editor module converts it
 * to DevTools-specific snapshot models.</p>
 */
public record UiRuntimeInspectionSource(
        UiDomDocument document,
        UiCssLayoutResult layout,
        float viewportWidth,
        float viewportHeight,
        long capturedAtMillis
) {
    public UiRuntimeInspectionSource {
        viewportWidth = finiteNonNegative(viewportWidth);
        viewportHeight = finiteNonNegative(viewportHeight);
        capturedAtMillis = Math.max(0L, capturedAtMillis);
    }

    public boolean available() {
        return document != null && document.rootOptional().isPresent() && layout != null;
    }

    public static UiRuntimeInspectionSource empty() {
        return new UiRuntimeInspectionSource(null, null, 0f, 0f, 0L);
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
