package com.gamifiedjava.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Repairs common UTF-8 text that was accidentally decoded as Windows-1252. */
public final class MojibakeRepair {
    private static final Map<String, String> REPLACEMENTS = replacements();

    private MojibakeRepair() {}

    public static String repair(String value) {
        if (value == null || value.isEmpty()) return value;
        String repaired = value;
        for (int pass = 0; pass < 3; pass++) {
            String before = repaired;
            for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
                repaired = repaired.replace(entry.getKey(), entry.getValue());
            }
            if (repaired.equals(before)) break;
        }
        return repaired;
    }

    private static Map<String, String> replacements() {
        Map<String, String> values = new LinkedHashMap<>();
        // Double-encoded fragments are repaired first; later passes finish them.
        values.put("\u00C3\u00A2", "\u00E2");
        values.put("\u00C3\u201A", "\u00C2");
        values.put("\u00E2\u201A\u00AC", "\u20AC");
        values.put("\u00E2\u20AC\u009D", "\u201D");

        values.put("\u00E2\u20AC\u201D", "\u2014");
        values.put("\u00E2\u20AC\u201C", "\u2013");
        values.put("\u00E2\u20AC\u00A6", "\u2026");
        values.put("\u00C2\u00B7", "\u00B7");
        values.put("\u00E2\u0153\u201C", "\u2713");
        values.put("\u00E2\u0153\u2014", "\u2717");
        values.put("\u00E2\u2030\u00A5", "\u2265");
        values.put("\u00C3\u2014", "\u00D7");
        values.put("\u00F0\u0178\u201D\u00A5", "\uD83D\uDD25");
        values.put("\u00E2\u2020\u2019", "\u2192");
        values.put("\u00E2\u2020\u0090", "\u2190");
        values.put("\u00E2\u02C6\u2019", "\u2212");
        values.put("\u00E2\u201D\u20AC", "\u2500");
        return values;
    }
}
