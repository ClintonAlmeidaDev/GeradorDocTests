package br.com.clinton.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** defaults < properties file < AUDIT_* environment < CLI. */
public final class CliOptions {
    private static final Set<String> ALLOWED =
            Set.of(
                    "runner",
                    "collection",
                    "environment",
                    "company",
                    "executor",
                    "output-dir",
                    "pdf-output",
                    "json-output",
                    "keep-raw-results",
                    "mask-key",
                    "tool-arg",
                    "tool-args",
                    "environment-file",
                    "bruno-env",
                    "timeout-seconds",
                    "config",
                    "help",
                    "version",
                    "openapi",
                    "generate",
                    "collection-name");
    private final Map<String, String> values = new LinkedHashMap<>();
    public final List<String> maskKeys = new ArrayList<>(), toolArgs = new ArrayList<>();

    public static CliOptions parse(String[] args, Map<String, String> env) throws IOException {
        CliOptions o = new CliOptions();
        Map<String, String> cli = new LinkedHashMap<>();
        List<String> masks = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--") || arg.equals("--tool-args")) {
                o.toolArgs.addAll(Arrays.asList(args).subList(i + 1, args.length));
                break;
            }
            if (!arg.startsWith("--"))
                throw new IllegalArgumentException("Argumento inválido. Use --help.");
            String[] kv = arg.substring(2).split("=", 2);
            String key = kv[0];
            if (!ALLOWED.contains(key))
                throw new IllegalArgumentException("Opção desconhecida: --" + key);
            boolean flag = Set.of("help", "version", "keep-raw-results").contains(key);
            String val =
                    kv.length == 2 ? kv[1] : flag ? "true" : (++i < args.length ? args[i] : null);
            if (val == null || val.isBlank() || (!key.equals("tool-arg") && val.startsWith("--")))
                throw new IllegalArgumentException("Falta valor para --" + key);
            if (key.equals("mask-key")) masks.add(val);
            else if (key.equals("tool-arg")) o.toolArgs.add(val);
            else cli.put(key, val);
        }
        o.values.putAll(
                Map.of(
                        "runner",
                        "auto",
                        "environment",
                        "LOCAL",
                        "company",
                        "SUA_EMPRESA",
                        "executor",
                        System.getProperty("user.name", "unknown"),
                        "output-dir",
                        "audit-output",
                        "timeout-seconds",
                        "1800",
                        "keep-raw-results",
                        "false"));
        String config = cli.getOrDefault("config", env.get("AUDIT_CONFIG"));
        if (config != null) {
            Properties p = new Properties();
            try (var reader = Files.newBufferedReader(Path.of(config))) {
                p.load(reader);
            }
            for (String k : p.stringPropertyNames()) {
                if (!ALLOWED.contains(k) || k.startsWith("tool-"))
                    throw new IllegalArgumentException("Chave de configuração não suportada: " + k);
                o.values.put(k, p.getProperty(k));
            }
        }
        for (String key : ALLOWED) {
            String v = env.get("AUDIT_" + key.replace('-', '_').toUpperCase(Locale.ROOT));
            if (v != null && !key.startsWith("tool-")) o.values.put(key, v);
        }
        o.values.putAll(cli);
        if (o.values.containsKey("mask-key"))
            o.maskKeys.addAll(Arrays.asList(o.get("mask-key").split(",")));
        o.maskKeys.addAll(masks);
        if (!Set.of("auto", "bruno", "postman").contains(o.get("runner")))
            throw new IllegalArgumentException("Runner deve ser auto, bruno ou postman.");
        if (!Set.of("true", "false").contains(o.get("keep-raw-results")))
            throw new IllegalArgumentException("keep-raw-results deve ser true ou false.");
        try {
            if (o.timeout() < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("timeout-seconds deve ser inteiro positivo.");
        }
        if (o.get("openapi") != null && o.get("collection") != null)
            throw new IllegalArgumentException("Escolha --openapi ou --collection, não ambos.");
        if (o.get("generate") != null && o.get("openapi") == null)
            throw new IllegalArgumentException("--generate exige --openapi.");
        // Reporter options are owned by the orchestrator; additional exports bypass raw retention.
        for (String a : o.toolArgs)
            if (a.startsWith("--reporter")
                    || a.startsWith("--export-")
                    || Set.of("-o", "--output", "-f", "--format", "-r").contains(a.split("=")[0]))
                throw new IllegalArgumentException(
                        "Argumentos de reporter/export são gerenciados pela ferramenta.");
        return o;
    }

    public String get(String key) {
        return values.get(key);
    }

    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public boolean flag(String key) {
        return Boolean.parseBoolean(values.get(key));
    }

    public long timeout() {
        return Long.parseLong(get("timeout-seconds"));
    }

    public Path outputDir() {
        return Path.of(get("output-dir")).toAbsolutePath().normalize();
    }
}
