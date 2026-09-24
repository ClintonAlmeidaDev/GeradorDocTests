package br.com.clinton.sanitizer;

import br.com.clinton.model.AuditReport;

import com.fasterxml.jackson.databind.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Sanitizes a copy; never modifies the parser's original evidence. */
public class SensitiveDataSanitizer {
    public static final String MASK = "••••••••";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Set<String> keys = new HashSet<>();
    private final Set<String> values = new HashSet<>();

    public SensitiveDataSanitizer() {
        this(List.of());
    }

    public SensitiveDataSanitizer(Collection<String> extra) {
        for (String k :
                List.of(
                        "password",
                        "passwd",
                        "secret",
                        "clientSecret",
                        "token",
                        "accessToken",
                        "refreshToken",
                        "apiKey",
                        "authorization",
                        "proxy-authorization",
                        "x-api-key",
                        "cookie",
                        "set-cookie")) keys.add(normalize(k));
        extra.forEach(k -> keys.add(normalize(k)));
    }

    private String normalize(String key) {
        return key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    public boolean isSensitive(String key) {
        return keys.contains(normalize(key));
    }

    public AuditReport sanitizeReport(AuditReport report) {
        Object tree = JSON.convertValue(report, Object.class);
        values.clear();
        discover(tree);
        return JSON.convertValue(sanitize(tree), AuditReport.class);
    }

    public Object sanitize(Object value) {
        if (value instanceof JsonNode) value = JSON.convertValue(value, Object.class);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            String namedKey =
                    String.valueOf(map.containsKey("key") ? map.get("key") : map.get("name"));
            for (var e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                result.put(
                        key,
                        isSensitive(key) || (key.equals("value") && isSensitive(namedKey))
                                ? MASK
                                : sanitize(e.getValue()));
            }
            return result;
        }
        if (value instanceof Collection<?> list) return list.stream().map(this::sanitize).toList();
        if (value instanceof String text) {
            try {
                Object parsed = JSON.readValue(text, Object.class);
                if (parsed instanceof Map || parsed instanceof List)
                    return JSON.writeValueAsString(sanitize(parsed));
            } catch (Exception ignored) {
            }
            return sanitizeText(text);
        }
        return value;
    }

    private void remember(Object v) {
        if (v instanceof String s && !s.isEmpty() && !MASK.equals(s)) values.add(s);
        if (v instanceof Map<?, ?> m) m.values().forEach(this::remember);
        if (v instanceof Collection<?> c) c.forEach(this::remember);
    }

    private void rememberSensitive(String key, Object value) {
        remember(value);
        if (!(value instanceof String text)) return;
        String normalized = normalize(key);
        if (normalized.endsWith("authorization")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length == 2) remember(parts[1]);
        }
        if (normalized.equals("cookie") || normalized.equals("setcookie")) {
            String[] parts = text.split(";");
            // Set-Cookie attributes (Path, Domain, SameSite) are not credential values.
            int count = normalized.equals("setcookie") ? Math.min(1, parts.length) : parts.length;
            for (int i = 0; i < count; i++) {
                String[] pair = parts[i].trim().split("=", 2);
                if (pair.length == 2) remember(pair[1]);
            }
        }
    }

    private void discover(Object value) {
        if (value instanceof Map<?, ?> map) {
            String namedKey =
                    String.valueOf(map.containsKey("key") ? map.get("key") : map.get("name"));
            map.forEach(
                    (k, v) -> {
                        if (isSensitive(k.toString())
                                || (k.equals("value") && isSensitive(namedKey))) {
                            rememberSensitive(k.equals("value") ? namedKey : k.toString(), v);
                        }
                        discover(v);
                    });
        } else if (value instanceof Collection<?> c) c.forEach(this::discover);
        else if (value instanceof String s) {
            Matcher m = Pattern.compile("[?&]([^=&#]+)=([^&#\\s]*)").matcher(s);
            while (m.find())
                if (isSensitive(decode(m.group(1)))) {
                    remember(m.group(2));
                    remember(decode(m.group(2)));
                }
            Matcher pairs = Pattern.compile("(?im)([\\w-]+)\\s*[:=]\\s*([^\\r\\n&;,]+)").matcher(s);
            while (pairs.find())
                if (isSensitive(pairs.group(1))) rememberSensitive(pairs.group(1), pairs.group(2));
            Matcher xml = Pattern.compile("(?is)<([\\w-]+)>([^<]*)</\\1>").matcher(s);
            while (xml.find()) if (isSensitive(xml.group(1))) remember(xml.group(2));
            try {
                Object parsed = JSON.readValue(s, Object.class);
                if (parsed instanceof Map || parsed instanceof List) discover(parsed);
            } catch (Exception ignored) {
            }
        }
    }

    private String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    public String sanitizeText(String text) {
        if (text == null) return null;
        String result = text.replaceAll("(?i)(https?://)[^/@\\s]+@", "$1" + MASK + "@");
        Matcher query = Pattern.compile("([?&])([^=&#]+)=([^&#\\s]*)").matcher(result);
        StringBuffer out = new StringBuffer();
        while (query.find())
            query.appendReplacement(
                    out,
                    Matcher.quoteReplacement(
                            query.group(1)
                                    + query.group(2)
                                    + "="
                                    + (isSensitive(decode(query.group(2)))
                                            ? MASK
                                            : query.group(3))));
        query.appendTail(out);
        result = out.toString();
        // Form fields, HTTP header lines and common key=value text. Opaque text is not classified
        // as structured JSON.
        Matcher pairs =
                Pattern.compile("(?im)([\\w-]+)(\\s*[:=]\\s*)([^\\r\\n&;,]+)").matcher(result);
        out = new StringBuffer();
        while (pairs.find())
            pairs.appendReplacement(
                    out,
                    Matcher.quoteReplacement(
                            pairs.group(1)
                                    + pairs.group(2)
                                    + (isSensitive(pairs.group(1)) ? MASK : pairs.group(3))));
        pairs.appendTail(out);
        result = out.toString();
        Matcher xml = Pattern.compile("(?is)<([\\w-]+)>([^<]*)</\\1>").matcher(result);
        out = new StringBuffer();
        while (xml.find())
            xml.appendReplacement(
                    out,
                    Matcher.quoteReplacement(
                            isSensitive(xml.group(1))
                                    ? "<" + xml.group(1) + ">" + MASK + "</" + xml.group(1) + ">"
                                    : xml.group()));
        xml.appendTail(out);
        result = out.toString();
        for (String secret :
                values.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList())
            result = result.replace(secret, MASK);
        return result;
    }
}
