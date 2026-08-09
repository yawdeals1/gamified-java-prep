package com.gamifiedjava.studio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Fuzzily parses dates/timestamps coming back from the Studio API JSON rows
 * (ISO local strings, ISO with timezone/UTC suffix, epoch millis, or null).
 */
public final class Ts {

    private Ts() {}

    public static LocalDateTime toDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof Number n) {
            long millis = n.longValue();
            return millis > 10_000_000_000L
                    ? LocalDateTime.ofEpochSecond(millis / 1000, (int) (millis % 1000) * 1_000_000, java.time.ZoneOffset.UTC)
                    : LocalDateTime.ofEpochSecond(millis, 0, java.time.ZoneOffset.UTC);
        }
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            if (s.endsWith("Z") || s.contains("+") || s.contains("-") && s.indexOf('-') > 10) {
                return OffsetDateTime.parse(s).toLocalDateTime();
            }
        } catch (DateTimeParseException ignored) {
            // fall through to local parse
        }
        try {
            return LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDate toDate(Object value) {
        LocalDateTime dt = toDateTime(value);
        return dt != null ? dt.toLocalDate() : null;
    }

    public static String iso(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    public static String iso(LocalDate value) {
        return value != null ? value.toString() : null;
    }
}