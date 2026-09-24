package br.com.clinton.runner;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;

public final class CollectionDetector {
    public static String detect(Path path, String runner) throws java.io.IOException {
        if (!Files.exists(path)) throw new IllegalArgumentException("Collection inexistente.");
        if (!runner.equals("auto")) {
            if (runner.equals("bruno") != Files.isDirectory(path))
                throw new IllegalArgumentException("Bruno exige diretório; Postman exige arquivo.");
            return runner;
        }
        if (Files.isDirectory(path)
                && (Files.exists(path.resolve("opencollection.yml"))
                        || Files.exists(path.resolve("bruno.json")))) return "bruno";
        if (Files.isRegularFile(path)) {
            var root = new ObjectMapper().readTree(path.toFile());
            String schema = root.path("info").path("schema").asText("");
            if (root.path("item").isArray()
                    && (schema.contains("/collection/v2.")
                            || path.getFileName().toString().endsWith(".postman_collection.json")))
                return "postman";
        }
        throw new IllegalArgumentException(
                "Collection não reconhecida. Informe --runner=bruno|postman.");
    }
}
