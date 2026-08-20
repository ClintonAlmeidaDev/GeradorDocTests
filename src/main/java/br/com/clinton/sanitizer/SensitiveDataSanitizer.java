package br.com.clinton.sanitizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SensitiveDataSanitizer {

    private static final String MASK = "••••••••";

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "passwd",
            "secret",
            "clientsecret",
            "token",
            "accesstoken",
            "refreshtoken",
            "apikey",
            "api_key",
            "authorization"
    );

    public Object sanitize(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }

        if (value instanceof List<?> list) {
            return sanitizeList(list);
        }

        return value;
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> source) {

        Map<String, Object> sanitized =
                new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {

            String key =
                    String.valueOf(entry.getKey());

            Object value =
                    entry.getValue();

            if (isSensitive(key)) {

                sanitized.put(
                        key,
                        MASK
                );

            } else {

                sanitized.put(
                        key,
                        sanitize(value)
                );
            }
        }

        return sanitized;
    }

    private List<Object> sanitizeList(List<?> source) {

        List<Object> sanitized =
                new ArrayList<>();

        for (Object value : source) {
            sanitized.add(
                    sanitize(value)
            );
        }

        return sanitized;
    }

    private boolean isSensitive(String key) {

        if (key == null) {
            return false;
        }

        String normalized =
                key.replace("-", "")
                        .replace("_", "")
                        .toLowerCase();

        return SENSITIVE_FIELDS.stream()
                .map(field ->
                        field.replace("-", "")
                                .replace("_", "")
                                .toLowerCase()
                )
                .anyMatch(normalized::equals);
    }
}