package dev.takesome.htmldom.markup.internal.parse.recovery;

import dev.takesome.htmldom.markup.internal.parse.syntax.UiHtmlSyntaxProfile;

import java.util.List;
import java.util.Set;

/** Parser recovery rules for optional end tags and mismatched closing tags. */
public final class UiHtmlRecoveryPolicy {
    private final UiHtmlSyntaxProfile syntax;

    public UiHtmlRecoveryPolicy(UiHtmlSyntaxProfile syntax) {
        this.syntax = syntax;
    }

    public int optionalAutoCloseCount(List<String> openTags, String nextTag) {
        if (openTags == null || openTags.size() <= 1 || nextTag == null || nextTag.isBlank()) {
            return 0;
        }

        String current = openTags.get(openTags.size() - 1);
        if ("p".equals(nextTag) && "p".equals(current)) {
            return 1;
        }
        if ("li".equals(nextTag) && "li".equals(current)) {
            return 1;
        }
        if (("dt".equals(nextTag) || "dd".equals(nextTag)) && ("dt".equals(current) || "dd".equals(current))) {
            return 1;
        }
        if (("option".equals(nextTag) || "optgroup".equals(nextTag)) && ("option".equals(current) || "optgroup".equals(current))) {
            return 1;
        }
        if (syntax.isHeadingTag(nextTag) && syntax.isHeadingTag(current)) {
            return 1;
        }
        if ("button".equals(nextTag) && "button".equals(current)) {
            return 1;
        }
        if ("tr".equals(nextTag)) {
            return countTrailing(openTags, Set.of("td", "th", "tr"));
        }
        if ("td".equals(nextTag) || "th".equals(nextTag)) {
            return countTrailing(openTags, Set.of("td", "th"));
        }
        if ("tbody".equals(nextTag) || "thead".equals(nextTag) || "tfoot".equals(nextTag)) {
            return countTrailing(openTags, Set.of("td", "th", "tr", "tbody", "thead", "tfoot"));
        }
        return 0;
    }

    public UiHtmlCloseResolution resolveClosing(List<String> openTags, String closingTag) {
        if (openTags == null || openTags.size() <= 1 || closingTag == null || closingTag.isBlank()) {
            return UiHtmlCloseResolution.unmatched();
        }
        int match = -1;
        for (int i = openTags.size() - 1; i >= 1; i--) {
            if (openTags.get(i).equals(closingTag)) {
                match = i;
                break;
            }
        }
        if (match < 0) {
            return UiHtmlCloseResolution.unmatched();
        }
        String top = openTags.get(openTags.size() - 1);
        return UiHtmlCloseResolution.matched(match, match != openTags.size() - 1, top);
    }

    private int countTrailing(List<String> openTags, Set<String> tags) {
        int count = 0;
        for (int i = openTags.size() - 1; i >= 1; i--) {
            if (!tags.contains(openTags.get(i))) {
                break;
            }
            count++;
        }
        return count;
    }
}
