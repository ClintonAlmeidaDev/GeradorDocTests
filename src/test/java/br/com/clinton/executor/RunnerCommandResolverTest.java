package br.com.clinton.executor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

class RunnerCommandResolverTest {
    @TempDir Path temp;

    Path npm(String tool, Path dir) throws Exception {
        Files.createDirectories(dir);
        Path launcher = dir.resolve(tool + ".cmd");
        Files.writeString(launcher, "@echo off\r\nexit /b 99\r\n");
        Path pkg =
                dir.resolve("node_modules")
                        .resolve(tool.equals("bru") ? "@usebruno/cli" : "newman");
        Files.createDirectories(pkg.resolve("bin"));
        Files.writeString(
                pkg.resolve("package.json"), "{\"bin\":{\"" + tool + "\":\"bin/entry.js\"}}");
        Files.writeString(pkg.resolve("bin/entry.js"), "// fixture");
        return launcher;
    }

    Path node() throws Exception {
        Path dir = Files.createDirectories(temp.resolve("Program Files/Node"));
        return Files.writeString(dir.resolve("node.exe"), "");
    }

    @Test
    void windowsUsesPathCasePathextAndNpmBinWithoutShell() throws Exception {
        Path launcher = npm("bru", temp.resolve("OneDrive - Empresa & Filhos/Usuário 100%/npm"));
        Path node = node();
        Map<String, String> env =
                Map.of(
                        "Path",
                        launcher.getParent() + ";" + node.getParent(),
                        "PATHEXT",
                        ".COM;.EXE;.BAT;.CMD");
        var resolved = RunnerCommandResolver.resolve("bru", "BRU_EXECUTABLE", env, true);
        assertEquals(
                List.of(
                        node.toString(),
                        launcher.getParent()
                                .resolve("node_modules/@usebruno/cli/bin/entry.js")
                                .toString()),
                resolved.prefix());
        assertFalse(resolved.prefix().toString().contains("cmd.exe"));
        assertTrue(resolved.environment().containsKey("Path"));
        assertFalse(resolved.environment().containsKey("PATH"));
    }

    @Test
    void appDataFallbackAndExplicitCmdWork() throws Exception {
        Path appData = temp.resolve("Roaming");
        Path launcher = npm("newman", appData.resolve("npm"));
        Path node = node();
        var env = Map.of("APPDATA", appData.toString(), "NODE_EXECUTABLE", node.toString());
        var auto = RunnerCommandResolver.resolve("newman", "NEWMAN_EXECUTABLE", env, true);
        var explicit =
                RunnerCommandResolver.resolve(launcher.toString(), "NEWMAN_EXECUTABLE", env, true);
        assertEquals(auto.prefix(), explicit.prefix());
        assertEquals(node.toString(), auto.prefix().getFirst());
    }

    @Test
    void missingNodeAndBrokenNpmGiveActionableErrors() throws Exception {
        Path launcher = npm("bru", temp.resolve("npm"));
        var error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                RunnerCommandResolver.resolve(
                                        launcher.toString(), "BRU_EXECUTABLE", Map.of(), true));
        assertTrue(error.getMessage().contains("NODE_EXECUTABLE"));
        Files.delete(launcher.getParent().resolve("node_modules/@usebruno/cli/package.json"));
        error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                RunnerCommandResolver.resolve(
                                        launcher.toString(), "BRU_EXECUTABLE", Map.of(), true));
        assertTrue(error.getMessage().contains("package.json"));
    }

    @Test
    void explicitJavascriptUsesNodeAndNativeExeStaysDirect() throws Exception {
        Path node = node(), script = Files.writeString(temp.resolve("entry.js"), "");
        assertEquals(
                List.of(node.toString(), script.toString()),
                RunnerCommandResolver.resolve(
                                script.toString(),
                                "BRU_EXECUTABLE",
                                Map.of("NODE_EXECUTABLE", node.toString()),
                                true)
                        .prefix());
        assertEquals(
                List.of(node.toString()),
                RunnerCommandResolver.resolve(node.toString(), "BRU_EXECUTABLE", Map.of(), true)
                        .prefix());
    }
}
