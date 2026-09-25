package br.com.clinton.tests;

import static org.junit.jupiter.api.Assertions.*;

import br.com.clinton.config.*;
import br.com.clinton.context.*;
import br.com.clinton.report.*;
import br.com.clinton.runner.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.time.*;
import java.util.*;

class ConfigurationContextTest {
    @TempDir Path temp;

    @Test
    void brunoRecursivePassThroughIsAccepted() throws Exception {
        var options = CliOptions.parse(new String[] {"--runner=bruno", "--", "-r"}, Map.of());
        assertEquals(List.of("-r"), options.toolArgs);
    }

    @Test
    void precedenceAndRepeatedOptions() throws Exception {
        Path f = temp.resolve("audit.properties");
        Files.writeString(f, "company=File\nenvironment=DEV\nmask-key=cpf\n");
        var o =
                CliOptions.parse(
                        new String[] {
                            "--config",
                            f.toString(),
                            "--company=CLI",
                            "--mask-key=accountNumber",
                            "--",
                            "--env-var",
                            "a=b"
                        },
                        Map.of("AUDIT_COMPANY", "ENV", "AUDIT_ENVIRONMENT", "HML"));
        assertEquals("CLI", o.get("company"));
        assertEquals("HML", o.get("environment"));
        assertEquals(List.of("cpf", "accountNumber"), o.maskKeys);
        assertEquals(List.of("--env-var", "a=b"), o.toolArgs);
    }

    @Test
    void invalidOptionsRejected() {
        for (String[] args :
                List.of(
                        new String[] {"--typo=1"},
                        new String[] {"--collection"},
                        new String[] {"--runner=x"},
                        new String[] {"--timeout-seconds=0"},
                        new String[] {"--", "--reporter-json=/tmp/x"}))
            assertThrows(Exception.class, () -> CliOptions.parse(args, Map.of()));
        assertEquals(2, AuditCli.run("--collection", temp.resolve("missing").toString()));
    }

    @Test
    void detectionAndFilename() throws Exception {
        Files.writeString(temp.resolve("opencollection.yml"), "opencollection: 1.0.0");
        assertEquals("bruno", CollectionDetector.detect(temp, "auto"));
        Path p = temp.resolve("c.json");
        Files.writeString(
                p,
                "{\"info\":{\"schema\":\"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"},\"item\":[]}");
        assertEquals("postman", CollectionDetector.detect(p, "auto"));
        String f =
                ReportFilename.create(
                        "Coleção / ? : teste",
                        "HML",
                        ZonedDateTime.parse("2026-09-23T10:00:00Z"),
                        true);
        assertTrue(f.endsWith("_FAIL.pdf"));
        assertTrue(f.contains("_hml_"));
        assertFalse(f.contains("/"));
        assertTrue(
                ReportFilename.create("c", "LOCAL", ZonedDateTime.now(), false)
                        .endsWith("PASS.pdf"));
    }

    ExecutionContextCollector fake(String status) {
        return new ExecutionContextCollector() {
            protected String git(Path p, String... a) {
                return switch (a[0]) {
                    case "rev-parse" -> a[1].equals("HEAD") ? "abcdef123456" : "feature/test";
                    case "log" -> "Author";
                    case "status" -> status;
                    default -> null;
                };
            }
        };
    }

    @Test
    void localCleanDirtyUnavailable() {
        assertEquals("CLEAN", fake("").collect(temp, Map.of()).get("Workspace"));
        assertEquals("DIRTY", fake(" M x").collect(temp, Map.of()).get("Workspace"));
        var unavailable =
                new ExecutionContextCollector() {
                    protected String git(Path p, String... a) {
                        return null;
                    }
                }.collect(temp, Map.of());
        assertEquals("LOCAL", unavailable.get("Origem"));
        assertEquals("indisponível", unavailable.get("Git"));
    }

    @Test
    void ciProviders() {
        var azure =
                fake("").collect(
                                temp,
                                Map.of(
                                        "TF_BUILD",
                                        "True",
                                        "BUILD_DEFINITIONNAME",
                                        "API",
                                        "BUILD_SOURCEVERSION",
                                        "123456789",
                                        "BUILD_REQUESTEDFOR",
                                        "Runner",
                                        "SYSTEM_PULLREQUEST_PULLREQUESTID",
                                        "42"));
        assertEquals("Azure DevOps", azure.get("Provider"));
        assertEquals("42", azure.get("PR"));
        assertEquals("1234567", azure.get("Short SHA"));
        assertEquals(
                "GitHub Actions",
                fake("").collect(temp, Map.of("GITHUB_ACTIONS", "true")).get("Provider"));
        assertEquals(
                "GitLab CI", fake("").collect(temp, Map.of("GITLAB_CI", "true")).get("Provider"));
        assertEquals(
                "Jenkins",
                fake("").collect(temp, Map.of("JENKINS_URL", "https://example.test"))
                        .get("Provider"));
    }
}
