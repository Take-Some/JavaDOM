package dev.takesome.htmldom.css;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.emptyIfNull;
import java.util.Locale;

public interface UiIntrinsicTextMeasurer {
    UiIntrinsicTextMetrics measure(String text, String fontId, float scale, float fallbackFontSize);

    default float spaceWidth(String fontId, float scale, float fallbackFontSize) {
        float measured = measure(" ", fontId, scale, fallbackFontSize).width();
        if (measured > 0f) return measured;
        return Math.max(1f, fallbackFontSize) * Math.max(0.01f, scale) / 3f;
    }

    static UiIntrinsicTextMeasurer heuristic() {
        return HeuristicTextMeasurer.INSTANCE;
    }

    final class HeuristicTextMeasurer implements UiIntrinsicTextMeasurer {
        private static final HeuristicTextMeasurer INSTANCE = new HeuristicTextMeasurer();

        private HeuristicTextMeasurer() {
        }

        @Override
        public UiIntrinsicTextMetrics measure(String text, String fontId, float scale, float fallbackFontSize) {
            String normalized = emptyIfNull(text)
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .replaceAll("\\s+", " ");
            if (normalized.isEmpty()) return UiIntrinsicTextMetrics.ZERO;
            float size = Math.max(1f, fallbackFontSize) * Math.max(0.01f, scale);
            boolean title = titleFont(fontId);
            float width = 0f;
            for (int i = 0; i < normalized.length(); i++) width += glyphWidth(normalized.charAt(i), size, title);
            return new UiIntrinsicTextMetrics(width, size * 1.25f);
        }

        private boolean titleFont(String fontId) {
            String id = emptyIfNull(fontId).toLowerCase(Locale.ROOT);
            return id.contains("title") || id.contains("pixel") || id.contains("heading");
        }

        private float glyphWidth(char c, float fontSize, boolean title) {
            if (Character.isWhitespace(c)) return fontSize * (title ? 0.55f : 0.33f);
            if ("ilI.,'!|".indexOf(c) >= 0) return fontSize * (title ? 0.52f : 0.30f);
            if ("mwMW@#%".indexOf(c) >= 0) return fontSize * (title ? 1.12f : 0.92f);
            return fontSize * (title ? 0.92f : 0.56f);
        }
    }
}
