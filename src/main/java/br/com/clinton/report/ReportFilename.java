package br.com.clinton.report;

import java.text.Normalizer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReportFilename {
    public static String slug(String text) {
        String s =
                Normalizer.normalize(text, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9_-]+", "-")
                        .replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "collection" : s.substring(0, Math.min(70, s.length()));
    }

    public static String create(
            String collection, String environment, ZonedDateTime time, boolean failed) {
        return "auditoria_api_"
                + slug(collection)
                + "_"
                + slug(environment)
                + "_"
                + time.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
                + "_"
                + (failed ? "FAIL" : "PASS")
                + ".pdf";
    }
}
