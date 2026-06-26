package dev.takesome.htmldom.support.validation;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class HtmlDomValidator {
    private HtmlDomValidator() {}
    public static <T> T notNull(T value, String field) { if (value == null) throw new IllegalArgumentException(field + " must not be null"); return value; }
    public static String nonBlank(String value, String field) { String v = trimToEmpty(value); if (v.isBlank()) throw new IllegalArgumentException(field + " must not be blank"); return v; }
    public static String requiredField(String value, String owner, String field, Object source) { return nonBlank(value, owner + "." + field); }
    public static void argument(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
    public static boolean any(java.util.function.BooleanSupplier... conditions) { if (conditions == null) return false; for (var c : conditions) if (c != null && c.getAsBoolean()) return true; return false; }
    public static int positive(int value, String field) { if (value <= 0) throw new IllegalArgumentException(field + " must be positive"); return value; }
    public static long positive(long value, String field) { if (value <= 0) throw new IllegalArgumentException(field + " must be positive"); return value; }
    public static int nonNegative(int value, String field) { if (value < 0) throw new IllegalArgumentException(field + " must be non-negative"); return value; }
    public static long nonNegative(long value, String field) { if (value < 0) throw new IllegalArgumentException(field + " must be non-negative"); return value; }
    public static int atLeast(int value, int minimum, String field) { if (value < minimum) throw new IllegalArgumentException(field + " must be >= " + minimum); return value; }
    public static long atLeast(long value, long minimum, String field) { if (value < minimum) throw new IllegalArgumentException(field + " must be >= " + minimum); return value; }
    public static float finite(float value, String field) { if (!Float.isFinite(value)) throw new IllegalArgumentException(field + " must be finite"); return value; }
    public static float positiveFinite(float value, String field) { finite(value, field); if (value <= 0f) throw new IllegalArgumentException(field + " must be positive"); return value; }
    public static float nonNegativeFinite(float value, String field) { finite(value, field); if (value < 0f) throw new IllegalArgumentException(field + " must be non-negative"); return value; }
    public static boolean hasPositiveFiniteSize(float width, float height) { return Float.isFinite(width) && Float.isFinite(height) && width > 0f && height > 0f; }
    public static Duration nonNegative(Duration value, String field) { notNull(value, field); if (value.isNegative()) throw new IllegalArgumentException(field + " must be non-negative"); return value; }
    public static <T> List<T> immutableList(List<T> value, String field) { return List.copyOf(value == null ? List.of() : value); }
    public static <K,V> Map<K,V> immutableMap(Map<K,V> value, String field) { return Map.copyOf(value == null ? Map.of() : value); }
    public static boolean isBlank(String value) { return value == null || value.trim().isBlank(); }
    public static float clamp01(float value) { if (!Float.isFinite(value)) return 0f; return Math.max(0f, Math.min(1f, value)); }
    public static String emptyIfNull(String value) { return value == null ? "" : value; }
    public static String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
    public static String trimToEmpty(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    public static String lowerTrimToEmpty(String value, Locale locale) { return trimToEmpty(value).toLowerCase(locale == null ? Locale.ROOT : locale); }
    public static String toStringOrEmpty(Object value) { return value == null ? "" : String.valueOf(value); }
    public static String prefixIfNotBlank(String value, String prefix) { String v = trimToEmpty(value); return v.isBlank() ? "" : emptyIfNull(prefix) + v; }
    public static <T> String textOrEmpty(T value, Function<? super T, String> textReader) { return value == null || textReader == null ? "" : emptyIfNull(textReader.apply(value)); }
}
