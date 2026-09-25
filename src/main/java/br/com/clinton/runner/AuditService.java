package br.com.clinton.runner;

import br.com.clinton.auditor.AuditReportGenerator;
import br.com.clinton.config.CliOptions;
import br.com.clinton.context.ExecutionContextCollector;
import br.com.clinton.executor.*;
import br.com.clinton.history.RunManifest;
import br.com.clinton.model.*;
import br.com.clinton.parser.*;
import br.com.clinton.report.*;
import br.com.clinton.sanitizer.SensitiveDataSanitizer;

import org.slf4j.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;

public class AuditService {
    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    public int execute(CliOptions o) throws Exception {
        if (o.get("collection") == null)
            throw new IllegalArgumentException("Informe --collection. Use --help.");
        Path collection = Path.of(o.get("collection")).toAbsolutePath().normalize();
        String runner = CollectionDetector.detect(collection, o.get("runner"));
        Path output = o.outputDir();
        Files.createDirectories(output);
        List<String> extra = new ArrayList<>(o.toolArgs);
        if (o.get("environment-file") != null) {
            Path env = Path.of(o.get("environment-file")).toAbsolutePath();
            if (!Files.isRegularFile(env))
                throw new IllegalArgumentException("Arquivo de ambiente inexistente.");
            extra.addAll(
                    List.of(
                            runner.equals("bruno") ? "--env-file" : "--environment",
                            env.toString()));
        }
        if (o.get("bruno-env") != null) {
            if (!runner.equals("bruno"))
                throw new IllegalArgumentException("--bruno-env exige runner Bruno.");
            extra.addAll(List.of("--env", o.get("bruno-env")));
        }
        if (o.get("folder") != null) {
            if (runner.equals("bruno")) {
                Path folder = collection.resolve(o.get("folder")).normalize();
                if (!folder.startsWith(collection) || !Files.isDirectory(folder))
                    throw new IllegalArgumentException(
                            "--folder deve ser uma pasta existente dentro da collection.");

            } else extra.addAll(List.of("--folder", o.get("folder")));
        }
        ZonedDateTime started = ZonedDateTime.now();
        long nanos = System.nanoTime();
        String id = UUID.randomUUID().toString();
        Path temp = Files.createTempDirectory("api-audit-");
        try {
            try {
                Files.setPosixFilePermissions(
                        temp, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
            } catch (UnsupportedOperationException ignored) {
            }
            Path raw = temp.resolve("result.raw.json");
            LOG.info("Runner: {}. Executando collection...", runner);
            CollectionExecutor executor =
                    runner.equals("bruno")
                            ? new BrunoCliExecutor(
                                    System.getenv().getOrDefault("BRU_EXECUTABLE", "bru"),
                                    extra,
                                    o.timeout(),
                                    o.get("folder", "."))
                            : new NewmanExecutor(
                                    System.getenv().getOrDefault("NEWMAN_EXECUTABLE", "newman"),
                                    extra,
                                    o.timeout());
            if (o.flag("diagnostics")) {
                String executable =
                        runner.equals("bruno")
                                ? System.getenv().getOrDefault("BRU_EXECUTABLE", "bru")
                                : System.getenv().getOrDefault("NEWMAN_EXECUTABLE", "newman");
                var command =
                        RunnerCommandResolver.resolve(
                                executable,
                                runner.equals("bruno") ? "BRU_EXECUTABLE" : "NEWMAN_EXECUTABLE");
                var sanitizer = new SensitiveDataSanitizer(o.maskKeys);
                LOG.info(
                        "Launcher: {}. Modo: {}",
                        sanitizer.sanitizeText(command.prefix().toString()),
                        command.mode());
                LOG.info(
                        "Diretório de execução: {}",
                        sanitizer.sanitizeText(
                                (Files.isDirectory(collection)
                                                ? collection
                                                : collection.getParent())
                                        .toString()));
                LOG.info("Reporter esperado: {}", raw);
                LOG.info(
                        "Recursivo: {}. Pasta selecionada: {}. Argumentos adicionais: {} (valores"
                                + " ocultos).",
                        runner.equals("bruno"),
                        o.get("folder") != null,
                        extra.size());
                if (runner.equals("bruno"))
                    LOG.info(
                            "Estrutura do comando: <launcher> run <pasta ou .> -r <argumentos"
                                    + " ocultos> --reporter-json <reporter esperado>");
                else
                    LOG.info(
                            "Estrutura do comando: <launcher> run <collection> <argumentos ocultos>"
                                    + " --reporters cli,json --reporter-json-export <reporter"
                                    + " esperado>");
            }
            CollectionExecutionResult result =
                    executor.execute(collection.toString(), raw.toString());
            if (!result.isReportGenerated())
                throw new IllegalStateException(
                        "Executor não gerou JSON (código "
                                + result.getExitCode()
                                + "). "
                                + String.join(" ", result.getDiagnostics())
                                + " Execute --doctor --runner="
                                + runner
                                + " e repita com --diagnostics. Confira collection, ambiente e"
                                + " argumentos. A saída bruta do runner é ocultada para proteger"
                                + " dados sensíveis.");
            AuditReport report =
                    (runner.equals("bruno") ? new BrunoResultParser() : new PostmanResultParser())
                            .parse(raw.toString());
            if (result.getExitCode() != 0 && result.getExitCode() != 1)
                throw new IllegalStateException("Executor terminou com erro técnico.");
            boolean failed =
                    result.getExitCode() != 0
                            || report.isExecutionFailed()
                            || report.getSummary().getFailedRequests() > 0;
            report.setExecutionFailed(result.getExitCode() != 0 || report.isExecutionFailed());
            ReportMetadata metadata = new ReportMetadata();
            metadata.setCollectionName(
                    o.get(
                            "collection-name",
                            collection
                                    .getFileName()
                                    .toString()
                                    .replaceFirst("\\.postman_collection\\.json$", "")));
            metadata.setCompanyName(o.get("company"));
            metadata.setEnvironment(o.get("environment"));
            metadata.setExecutor(o.get("executor"));
            metadata.setExecutionDate(started.toString());
            metadata.setTraceability(
                    new ExecutionContextCollector()
                            .collect(
                                    Files.isDirectory(collection)
                                            ? collection
                                            : collection.getParent(),
                                    System.getenv()));
            report.setMetadata(metadata);
            report = new SensitiveDataSanitizer(o.maskKeys).sanitizeReport(report);
            metadata = report.getMetadata();
            Path pdf =
                    o.get("pdf-output") == null
                            ? output.resolve(
                                    ReportFilename.create(
                                            metadata.getCollectionName(),
                                            metadata.getEnvironment(),
                                            started,
                                            failed))
                            : Path.of(o.get("pdf-output")).toAbsolutePath();
            Path json =
                    o.get("json-output") == null
                            ? pdf.resolveSibling(
                                    pdf.getFileName().toString().replaceFirst("\\.pdf$", "")
                                            + ".sanitized.json")
                            : Path.of(o.get("json-output")).toAbsolutePath();
            Path manifest =
                    pdf.resolveSibling(
                            pdf.getFileName().toString().replaceFirst("\\.pdf$", "")
                                    + ".audit-run.json");
            for (Path p : List.of(pdf, json, manifest)) {
                if (Files.exists(p))
                    throw new IllegalArgumentException(
                            "Arquivo de saída já existe. Escolha outro caminho.");
                Files.createDirectories(p.getParent());
            }
            if (pdf.normalize().equals(json.normalize())
                    || json.normalize().equals(manifest.normalize()))
                throw new IllegalArgumentException("Saídas precisam de caminhos distintos.");
            String html = new HtmlReportGenerator().generate(report);
            try {
                AuditReportGenerator.generatePdfFromHtml(html, pdf.toString());
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Falha Playwright/Chromium ao gerar PDF. Verifique instalação do navegador"
                                + " e bibliotecas nativas.");
            }
            Files.writeString(
                    json,
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(report),
                    StandardOpenOption.CREATE_NEW);
            RunManifest.write(
                    manifest, report, id, (System.nanoTime() - nanos) / 1_000_000, pdf, failed);
            ExecutionSummary s = report.getSummary();
            LOG.info(
                    "Requests: {} ({} aprovadas / {} falhas). Assertions: {} ({} pass / {} fail).",
                    s.getTotalRequests(),
                    s.getSuccessfulRequests(),
                    s.getFailedRequests(),
                    s.getTotalAssertions(),
                    s.getSuccessfulAssertions(),
                    s.getFailedAssertions());
            LOG.info(
                    "PDF: {}", new SensitiveDataSanitizer(o.maskKeys).sanitizeText(pdf.toString()));
            if (failed) LOG.warn("Testes possuem falhas.");
            return failed ? 1 : 0;
        } finally {
            Path raw = temp.resolve("result.raw.json");
            try {
                if (o.flag("keep-raw-results") && Files.exists(raw)) {
                    Path retained = output.resolve(id + ".raw.json");
                    Files.copy(raw, retained);
                    try {
                        Files.setPosixFilePermissions(
                                retained,
                                java.nio.file.attribute.PosixFilePermissions.fromString(
                                        "rw-------"));
                    } catch (UnsupportedOperationException ignored) {
                    }
                    LOG.warn(
                            "JSON bruto mantido em {}. Pode conter dados sensíveis; não publique"
                                    + " este arquivo.",
                            retained);
                }
            } finally {
                Files.deleteIfExists(raw);
                Files.deleteIfExists(temp);
            }
        }
    }
}
