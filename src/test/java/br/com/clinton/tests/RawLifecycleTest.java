package br.com.clinton.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@EnabledOnOs({OS.LINUX, OS.MAC})
class RawLifecycleTest {
    @TempDir Path temp;

    @Test
    void malformedReporterIsRemovedEvenWhenParsingFails() throws Exception {
        run(false);
    }

    @Test
    void explicitRetentionPreservesRawAfterParsingFailure() throws Exception {
        run(true);
    }

    void run(boolean keep) throws Exception {
        Path collection = Files.createDirectory(temp.resolve("collection"));
        Path runtime = Files.createDirectory(temp.resolve("runtime"));
        Path output = temp.resolve("output");
        Path executable = temp.resolve("fake-bru");
        Files.writeString(
                executable,
                "#!/bin/sh\n"
                    + "for arg do out=\"$arg\"; done\n"
                    + "printf 'not-json PRIVATE_FIXTURE_SECRET' > \"$out\"\n");
        assertTrue(executable.toFile().setExecutable(true));
        List<String> command =
                new ArrayList<>(
                        List.of(
                                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                                "-Djava.io.tmpdir=" + runtime,
                                "-cp",
                                System.getProperty("java.class.path"),
                                "br.com.clinton.runner.AuditCli",
                                "--runner",
                                "bruno",
                                "--collection",
                                collection.toString(),
                                "--output-dir",
                                output.toString()));
        if (keep) command.add("--keep-raw-results");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("BRU_EXECUTABLE", executable.toString());
        Process child = builder.start();
        assertTrue(child.waitFor(20, TimeUnit.SECONDS));
        String console =
                new String(
                        child.getInputStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(2, child.exitValue(), console);
        assertFalse(console.contains("PRIVATE_FIXTURE_SECRET"));
        try (var paths = Files.list(runtime)) {
            assertEquals(0, paths.count());
        }
        try (var paths = Files.list(output)) {
            var raw = paths.filter(p -> p.toString().endsWith(".raw.json")).toList();
            assertEquals(keep ? 1 : 0, raw.size());
            if (keep)
                assertTrue(Files.readString(raw.getFirst()).contains("PRIVATE_FIXTURE_SECRET"));
        }
    }
}
