package br.com.clinton.tests;

import static org.junit.jupiter.api.Assertions.*;

import br.com.clinton.executor.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

@EnabledOnOs({OS.LINUX, OS.MAC})
class ExecutorTest {
    @TempDir Path temp;

    Path executable(String body) throws Exception {
        Path p = temp.resolve("fake-runner");
        Files.writeString(p, "#!/bin/sh\n" + body);
        assertTrue(p.toFile().setExecutable(true));
        return p;
    }

    @Test
    void processArgumentsWithSpacesAndFreshReporter() throws Exception {
        Path script =
                executable("for arg do out=\"$arg\"; done\nprintf '[{}]' > \"$out\"\nexit 1\n");
        Path result = temp.resolve("with spaces/result.json");
        var r =
                new BrunoCliExecutor(script.toString(), List.of("--env", "with spaces"), 5)
                        .execute(temp.toString(), result.toString());
        assertEquals(1, r.getExitCode());
        assertTrue(r.isReportGenerated());
    }

    @Test
    void staleReporterRemovedAndMissingExecutable() throws Exception {
        Path result = temp.resolve("raw.json");
        Files.writeString(result, "stale");
        var r =
                new BrunoCliExecutor(executable("exit 1\n").toString())
                        .execute(temp.toString(), result.toString());
        assertFalse(r.isReportGenerated());
        assertThrows(
                IllegalStateException.class,
                () ->
                        new BrunoCliExecutor(temp.resolve("absent").toString())
                                .execute(temp.toString(), result.toString()));
    }

    @Test
    void timeoutTerminatesProcess() throws Exception {
        var executor = new BrunoCliExecutor(executable("sleep 30\n").toString(), List.of(), 1);
        assertThrows(
                IllegalStateException.class,
                () -> executor.execute(temp.toString(), temp.resolve("r.json").toString()));
    }
}
