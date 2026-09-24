package br.com.clinton.tests;

import static org.junit.jupiter.api.Assertions.*;

import br.com.clinton.history.*;
import br.com.clinton.openapi.*;

import com.fasterxml.jackson.databind.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;

class OpenApiHistoryTest {
    @TempDir Path temp;

    @Test
    void generatesBothFormatsFromYamlAndJson() throws Exception {
        new OpenApiBootstrap().generate(Path.of("examples/minimal-openapi.yaml"), "postman", temp);
        var collection =
                new ObjectMapper()
                        .readTree(temp.resolve("openapi.postman_collection.json").toFile());
        assertEquals(2, collection.path("item").size());
        assertTrue(collection.toString().contains("{{id}}"));
        assertTrue(collection.toString().contains("{{token}}"));
        assertFalse(collection.toString().contains("pm.test"));
        new OpenApiBootstrap().generate(Path.of("examples/minimal-openapi.yaml"), "bruno", temp);
        assertTrue(Files.exists(temp.resolve("openapi-bruno/001-request.yml")));
        Path spec = temp.resolve("spec.json");
        Files.writeString(spec, "{\"openapi\":\"3.1.0\",\"paths\":{\"/hello\":{\"get\":{}}}}");
        new OpenApiBootstrap().generate(spec, "postman", temp.resolve("json"));
        assertThrows(Exception.class, () -> new OpenApiBootstrap().generate(spec, "postman", temp));
    }

    @Test
    void rejectsRemoteRefs() throws Exception {
        Path spec = temp.resolve("spec.json");
        Files.writeString(
                spec,
                "{\"openapi\":\"3.0.0\",\"paths\":{\"/hello\":{\"$ref\":\"https://example.test/spec\"}}}");
        assertThrows(
                IllegalArgumentException.class,
                () -> new OpenApiBootstrap().generate(spec, "postman", temp));
    }

    @Test
    void comparisonShowsRegressionAndAssertions() throws Exception {
        Path a = temp.resolve("a.json"), b = temp.resolve("b.json");
        Files.writeString(
                a,
                "{\"schemaVersion\":1,\"requests\":[{\"id\":\"GET"
                    + " /\",\"successful\":true,\"responseTimeMs\":10,\"assertions\":[{\"expression\":\"old\"}]}]}");
        Files.writeString(
                b,
                "{\"schemaVersion\":1,\"requests\":[{\"id\":\"GET"
                    + " /\",\"successful\":false,\"responseTimeMs\":20,\"assertions\":[{\"expression\":\"new\"}]}]}");
        String result = RunManifest.compare(a, b);
        assertTrue(result.contains("REGRESSÃO"));
        assertTrue(result.contains("10 ms"));
        assertTrue(result.contains("ASSERTION NOVA: new"));
        assertTrue(result.contains("ASSERTION REMOVIDA: old"));
        assertTrue(RunManifest.compare(b, a).contains("RECUPERADA"));
    }
}
