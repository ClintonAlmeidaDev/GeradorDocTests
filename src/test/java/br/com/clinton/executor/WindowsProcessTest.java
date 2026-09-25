package br.com.clinton.executor;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

/** Native process tests: requires Node on Windows, as provisioned by verify.yml. */
@EnabledOnOs(OS.WINDOWS)
class WindowsProcessTest {
    @TempDir Path temp;

    Path launcher(String script) throws Exception {
        Path prefix = Files.createDirectories(temp.resolve("Empresa & Filhos/Coleção 100%"));
        Path pkg = Files.createDirectories(prefix.resolve("node_modules/@usebruno/cli/bin"));
        Files.writeString(pkg.getParent().resolve("package.json"), "{\"bin\":{\"bru\":\"bin/entry.js\"}}");
        Files.writeString(pkg.resolve("entry.js"), script);
        // Running this shim instead of resolving Node would fail the assertions.
        return Files.writeString(prefix.resolve("bru.cmd"), "@exit /b 99\r\n");
    }

    @Test
    void realNodeReceivesLiteralArgumentsAndFreshReporter() throws Exception {
        Path shim = launcher("const fs=require('fs');const args=process.argv.slice(2);"
                + "fs.writeFileSync(args.at(-1),JSON.stringify(args));process.exit(1);");
        Path output = temp.resolve("saída & 100%.json");
        Files.writeString(output, "stale");
        var extra = List.of("-r", "--env-var", "value=ação & %PATH% ! literal", "quote=\"a b\"",
                "\"outer quotes\"", "C:\\space path\\", "slash\\\"quote", "");
        var result = new BrunoCliExecutor(shim.toString(), extra, 15)
                .execute(temp.toString(), output.toString());
        assertEquals(1, result.getExitCode());
        assertTrue(result.isReportGenerated());
        List<String> expected = new ArrayList<>(List.of("run", ".", "-r"));
        expected.addAll(extra);
        expected.addAll(List.of("--reporter-json", output.toString()));
        assertEquals(expected, new ObjectMapper().readValue(output.toFile(), List.class));
    }

    @Test
    void missingReporterHasSafeHintsAndRemovesStaleFile() throws Exception {
        Path shim = launcher("console.error('unknown option PRIVATE_FIXTURE_SECRET');process.exit(1);");
        Path output = temp.resolve("result.json");
        Files.writeString(output, "stale");
        var result = new BrunoCliExecutor(shim.toString(), List.of(), 15)
                .execute(temp.toString(), output.toString());
        assertFalse(result.isReportGenerated());
        assertFalse(Files.exists(output));
        assertTrue(result.getDiagnostics().toString().contains("argumento desconhecido"));
        assertFalse(result.getDiagnostics().toString().contains("PRIVATE_FIXTURE_SECRET"));
    }

    @Test
    void probeUsesRealNodeWithoutExecutingShim() throws Exception {
        Path shim = launcher("if(process.argv[2]==='--version') console.log('4.0.0');else process.exit(99);");
        assertEquals("4.0.0", RunnerProbe.check(shim.toString(), "BRU_EXECUTABLE").version());
    }

    @Test
    void timeoutKillsNativeNodeProcess() throws Exception {
        Path shim = launcher("setInterval(()=>{},1000);");
        var error = assertThrows(IllegalStateException.class,
                () -> new BrunoCliExecutor(shim.toString(), List.of(), 1)
                        .execute(temp.toString(), temp.resolve("result.json").toString()));
        assertTrue(error.getMessage().contains("Timeout"));
    }

    @Test
    void malformedReporterIsCleanedAndExplicitRetentionIsHonored() throws Exception {
        for (boolean keep : List.of(false, true)) {
            Path runtime = Files.createDirectories(temp.resolve("runtime-" + keep));
            Path output = temp.resolve("output-" + keep);
            Path shim = launcher("require('fs').writeFileSync(process.argv.at(-1),"
                    + "'not-json PRIVATE_FIXTURE_SECRET');");
            List<String> command = new ArrayList<>(List.of(
                    Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                    "-Djava.io.tmpdir=" + runtime, "-cp", System.getProperty("java.class.path"),
                    "br.com.clinton.runner.AuditCli", "--runner", "bruno", "--collection",
                    temp.toString(), "--output-dir", output.toString()));
            if (keep) command.add("--keep-raw-results");
            ProcessBuilder builder = new ProcessBuilder(ProcessArguments.literal(command)).redirectErrorStream(true);
            builder.environment().put("BRU_EXECUTABLE", shim.toString());
            Process child = builder.start();
            assertTrue(child.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
            String console = new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(2, child.exitValue(), console);
            assertFalse(console.contains("PRIVATE_FIXTURE_SECRET"));
            try (var paths = Files.list(runtime)) {
                assertEquals(0, paths.count());
            }
            try (var paths = Files.list(output)) {
                var raw = paths.filter(p -> p.toString().endsWith(".raw.json")).toList();
                assertEquals(keep ? 1 : 0, raw.size());
                if (keep) assertTrue(Files.readString(raw.getFirst()).contains("PRIVATE_FIXTURE_SECRET"));
            }
        }
    }
}
