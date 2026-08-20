package br.com.clinton.config;

import java.util.HashMap;
import java.util.Map;

public class AuditConfigurationLoader {

    private AuditConfigurationLoader() {
    }

    public static AuditConfiguration load(String[] args) {

        Map<String, String> arguments = parseArguments(args);

        String collectionPath = arguments.getOrDefault(
                "collection",
                "./src/test/resources/bruno-collection"
        );

        String jsonOutputPath = arguments.getOrDefault(
                "json-output",
                "./target/bruno-results.json"
        );

        String pdfOutputPath = arguments.getOrDefault(
                "pdf-output",
                "./Relatorio_Auditoria_Testes.pdf"
        );

        String collectionName = arguments.getOrDefault(
                "collection-name",
                "Minha Coleção Bruno"
        );

        String environment = arguments.getOrDefault(
                "environment",
                "LOCAL"
        );

        String executor = arguments.getOrDefault(
                "executor",
                "Java Automation Service"
        );

        String companyName = arguments.getOrDefault(
                "company",
                "SUA_EMPRESA"
        );

        return new AuditConfiguration(
                collectionPath,
                jsonOutputPath,
                pdfOutputPath,
                collectionName,
                environment,
                executor,
                companyName
        );
    }

    private static Map<String, String> parseArguments(String[] args) {

        Map<String, String> arguments = new HashMap<>();

        for (String arg : args) {

            if (arg == null || !arg.startsWith("--")) {
                continue;
            }

            String argument = arg.substring(2);

            String[] parts = argument.split("=", 2);

            if (parts.length != 2) {
                continue;
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            if (!key.isEmpty() && !value.isEmpty()) {
                arguments.put(key, value);
            }
        }

        return arguments;
    }
}