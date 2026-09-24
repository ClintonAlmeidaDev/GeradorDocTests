package br.com.clinton.openapi;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.nio.file.*;
import java.util.*;

/** Offline bootstrap: no remote refs, no inferred business assertions, no server execution. */
public class OpenApiBootstrap {
    private static final ObjectMapper JSON = new ObjectMapper(),
            YAML = new ObjectMapper(new YAMLFactory());
    private static final Set<String> METHODS =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    public void generate(Path source, String format, Path output) throws java.io.IOException {
        if (!Set.of("bruno", "postman").contains(format))
            throw new IllegalArgumentException("--generate deve ser bruno ou postman.");
        JsonNode root = YAML.readTree(source.toFile());
        if (!root.path("openapi").asText().startsWith("3.") || !root.path("paths").isObject())
            throw new IllegalArgumentException("Bootstrap exige OpenAPI 3.x com paths.");
        List<Map<String, Object>> items = new ArrayList<>();
        List<Map<String, Object>> bruno = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> paths = root.path("paths").fields();
        int seq = 0;
        while (paths.hasNext()) {
            var endpoint = paths.next();
            JsonNode path = resolve(root, endpoint.getValue());
            for (String method :
                    List.of("get", "post", "put", "patch", "delete", "head", "options", "trace")) {
                if (!path.has(method)) continue;
                JsonNode op = path.get(method);
                String name =
                        op.path("summary").asText(method.toUpperCase() + " " + endpoint.getKey());
                String url = "{{baseUrl}}" + endpoint.getKey().replaceAll("\\{([^}]+)}", "{{$1}}");
                List<Map<String, String>> headers = new ArrayList<>();
                List<String> query = new ArrayList<>();
                LinkedHashMap<String, JsonNode> params = new LinkedHashMap<>();
                for (JsonNode group : List.of(path.path("parameters"), op.path("parameters")))
                    for (JsonNode p : group) {
                        p = resolve(root, p);
                        params.put(p.path("in").asText() + ":" + p.path("name").asText(), p);
                    }
                for (JsonNode p : params.values()) {
                    String key = p.path("name").asText();
                    String placeholder = "{{" + key + "}}";
                    if (p.path("in").asText().equals("query")) query.add(key + "=" + placeholder);
                    if (p.path("in").asText().equals("header"))
                        headers.add(Map.of("key", key, "value", placeholder));
                }
                if (!query.isEmpty()) url += "?" + String.join("&", query);
                Map<String, Object> request = new LinkedHashMap<>();
                request.put("method", method.toUpperCase());
                request.put("url", url);
                Map<String, Object> http = new LinkedHashMap<>();
                http.put("method", method);
                http.put("url", url);
                JsonNode content = resolve(root, op.path("requestBody")).path("content");
                if (!content.isEmpty()) {
                    String media =
                            content.has("application/json")
                                    ? "application/json"
                                    : content.fieldNames().next();
                    headers.add(Map.of("key", "Content-Type", "value", media));
                    // Do not copy examples: contracts can contain production data or credential
                    // examples.
                    Object skeleton =
                            media.contains("json")
                                    ? sample(
                                            root,
                                            content.path(media).path("schema"),
                                            new HashSet<>(),
                                            0)
                                    : "TODO";
                    String body =
                            media.contains("json")
                                    ? JSON.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(skeleton)
                                    : "TODO";
                    request.put("body", Map.of("mode", "raw", "raw", body));
                    http.put(
                            "body",
                            Map.of("type", media.contains("json") ? "json" : "text", "data", body));
                }
                request.put("header", headers);
                if (!headers.isEmpty())
                    http.put(
                            "headers",
                            headers.stream()
                                    .map(h -> Map.of("name", h.get("key"), "value", h.get("value")))
                                    .toList());
                items.add(
                        Map.of(
                                "name",
                                name,
                                "request",
                                request,
                                "description",
                                "TODO: preencher variáveis, autenticação e assertions de negócio;"
                                    + " revisar schemas de resposta no contrato."));
                bruno.add(
                        Map.of(
                                "info",
                                Map.of("name", name, "type", "http", "seq", ++seq),
                                "http",
                                http));
            }
        }
        if (items.isEmpty())
            throw new IllegalArgumentException("OpenAPI sem endpoints suportados.");
        Files.createDirectories(output);
        Path generated =
                output.resolve(
                        format.equals("bruno")
                                ? "openapi-bruno"
                                : "openapi.postman_collection.json");
        if (Files.exists(generated))
            throw new IllegalArgumentException("Destino bootstrap já existe.");
        if (format.equals("postman")) {
            Map<String, Object> collection =
                    Map.of(
                            "info",
                            Map.of(
                                    "name",
                                    root.path("info").path("title").asText("OpenAPI skeleton"),
                                    "schema",
                                    "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"),
                            "variable",
                            List.of(Map.of("key", "baseUrl", "value", "https://example.invalid")),
                            "item",
                            items);
            Files.writeString(
                    generated,
                    JSON.writerWithDefaultPrettyPrinter().writeValueAsString(collection),
                    StandardOpenOption.CREATE_NEW);
        } else {
            Files.createDirectory(generated);
            Files.writeString(
                    generated.resolve("opencollection.yml"),
                    YAML.writeValueAsString(
                            Map.of(
                                    "opencollection",
                                    "1.0.0",
                                    "info",
                                    Map.of("name", "OpenAPI skeleton"))),
                    StandardOpenOption.CREATE_NEW);
            int index = 0;
            for (var item : bruno)
                Files.writeString(
                        generated.resolve(String.format("%03d-request.yml", ++index)),
                        YAML.writeValueAsString(item),
                        StandardOpenOption.CREATE_NEW);
        }
    }

    private JsonNode resolve(JsonNode root, JsonNode node) {
        if (!node.has("$ref")) return node;
        String ref = node.path("$ref").asText();
        if (!ref.startsWith("#/"))
            throw new IllegalArgumentException(
                    "Referência externa OpenAPI não suportada; forneça contrato bundled.");
        JsonNode resolved = root.at(ref.substring(1));
        if (resolved.isMissingNode())
            throw new IllegalArgumentException("Referência OpenAPI inexistente.");
        return resolved;
    }

    private Object sample(JsonNode root, JsonNode node, Set<String> visited, int depth) {
        if (depth > 8) return "TODO";
        if (node.has("$ref")) {
            String ref = node.path("$ref").asText();
            if (!visited.add(ref)) return "TODO";
            node = resolve(root, node);
        }
        String type = node.path("type").asText(node.has("properties") ? "object" : "string");
        return switch (type) {
            case "object" -> {
                Map<String, Object> m = new LinkedHashMap<>();
                var fields = node.path("properties").fields();
                while (fields.hasNext()) {
                    var f = fields.next();
                    m.put(
                            f.getKey(),
                            sample(root, f.getValue(), new HashSet<>(visited), depth + 1));
                }
                yield m;
            }
            case "array" ->
                    List.of(sample(root, node.path("items"), new HashSet<>(visited), depth + 1));
            case "integer", "number" -> 0;
            case "boolean" -> false;
            default -> "TODO";
        };
    }
}
