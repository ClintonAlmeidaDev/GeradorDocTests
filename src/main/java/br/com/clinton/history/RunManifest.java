package br.com.clinton.history;

import br.com.clinton.model.*;

import com.fasterxml.jackson.databind.*;

import java.nio.file.*;
import java.util.*;

public final class RunManifest {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void write(
            Path output,
            AuditReport sanitized,
            String executionId,
            long duration,
            Path pdf,
            boolean failed)
            throws java.io.IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schemaVersion", 1);
        m.put("executionId", executionId);
        m.put("timestamp", sanitized.getMetadata().getExecutionDate());
        m.put("collection", sanitized.getMetadata().getCollectionName());
        m.put("environment", sanitized.getMetadata().getEnvironment());
        m.put("result", failed ? "FAIL" : "PASS");
        m.put("durationMs", duration);
        m.put("summary", sanitized.getSummary());
        m.put("traceability", sanitized.getMetadata().getTraceability());
        m.put("toolVersion", sanitized.getMetadata().getTraceability().get("Tool Version"));
        m.put("pdf", pdf.getFileName().toString());
        List<Map<String, Object>> requests = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        for (RequestExecution r : sanitized.getExecutions()) {
            String base = r.getMethod() + " " + r.getUrl() + " | " + r.getName();
            int n = occurrences.merge(base, 1, Integer::sum);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", base + " #" + n);
            entry.put("successful", r.isSuccessful());
            entry.put("responseTimeMs", r.getResponseTimeMs());
            entry.put(
                    "assertions",
                    r.getAssertions().stream()
                            .map(
                                    a ->
                                            Map.of(
                                                    "expression",
                                                    Objects.toString(a.getExpression(), ""),
                                                    "successful",
                                                    a.isSuccessful(),
                                                    "skipped",
                                                    a.isSkipped()))
                            .toList());
            requests.add(entry);
        }
        m.put("requests", requests);
        Files.writeString(
                output,
                JSON.writerWithDefaultPrettyPrinter().writeValueAsString(m),
                StandardOpenOption.CREATE_NEW);
    }

    public static String compare(Path before, Path after) throws java.io.IOException {
        JsonNode a = JSON.readTree(before.toFile()), b = JSON.readTree(after.toFile());
        if (a.path("schemaVersion").asInt() != 1
                || b.path("schemaVersion").asInt() != 1
                || !a.path("requests").isArray()
                || !b.path("requests").isArray())
            throw new IllegalArgumentException("Manifest incompatível.");
        if (!a.path("collection").equals(b.path("collection"))
                || !a.path("environment").equals(b.path("environment")))
            throw new IllegalArgumentException("Compare a mesma collection e ambiente.");
        Map<String, JsonNode> old = index(a), next = index(b);
        StringBuilder out = new StringBuilder();
        for (var e : next.entrySet()) {
            String id = e.getKey();
            JsonNode r = e.getValue(), prev = old.get(id);
            if (prev == null) {
                out.append("NOVA: ").append(id).append('\n');
                continue;
            }
            if (prev.path("successful").asBoolean() != r.path("successful").asBoolean())
                out.append(r.path("successful").asBoolean() ? "RECUPERADA: " : "REGRESSÃO: ")
                        .append(id)
                        .append('\n');
            out.append("TEMPO: ")
                    .append(id)
                    .append(" ")
                    .append(
                            r.path("responseTimeMs").asLong()
                                    - prev.path("responseTimeMs").asLong())
                    .append(" ms\n");
            List<String> prior = assertions(prev), current = assertions(r);
            for (String s : current)
                if (!prior.remove(s)) out.append("ASSERTION NOVA: ").append(s).append('\n');
            for (String s : prior) out.append("ASSERTION REMOVIDA: ").append(s).append('\n');
        }
        for (String id : old.keySet())
            if (!next.containsKey(id)) out.append("REMOVIDA: ").append(id).append('\n');
        return new br.com.clinton.sanitizer.SensitiveDataSanitizer().sanitizeText(out.toString());
    }

    private static List<String> assertions(JsonNode r) {
        List<String> l = new ArrayList<>();
        r.path("assertions").forEach(a -> l.add(a.path("expression").asText()));
        return l;
    }

    private static Map<String, JsonNode> index(JsonNode n) {
        Map<String, JsonNode> m = new LinkedHashMap<>();
        n.path("requests").forEach(r -> m.put(r.path("id").asText(), r));
        return m;
    }
}
