package br.com.clinton.executor;

import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Runs without a shell. Child stdout is untrusted (scripts may print credentials). */
final class ExternalProcess {
    static CollectionExecutionResult run(
            String executable,
            String variable,
            Path cwd,
            Path output,
            List<String> args,
            long timeoutSeconds)
            throws IOException, InterruptedException {
        RunnerCommandResolver.Command resolved =
                RunnerCommandResolver.resolve(executable, variable);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.deleteIfExists(output);
        List<String> command = new ArrayList<>(resolved.prefix());
        command.addAll(args);
        ProcessBuilder builder =
                new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true);
        builder.environment().clear();
        builder.environment().putAll(resolved.environment());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível iniciar o runner. Execute --doctor e confira permissões,"
                        + " NODE_EXECUTABLE e o launcher configurado.");
        }
        RunnerOutputDiagnostics diagnostics = new RunnerOutputDiagnostics();
        // Do not relay arbitrary collection console.log / assertion values to CI logs.
        Thread drain =
                Thread.ofPlatform()
                        .daemon()
                        .start(
                                () -> {
                                    try (var stream = process.getInputStream()) {
                                        diagnostics.drain(stream);
                                    } catch (IOException ignored) {
                                    }
                                });
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                terminate(process);
                throw new IllegalStateException("Timeout do executor. Ajuste --timeout-seconds.");
            }
            drain.join(2000);
            LoggerFactory.getLogger(ExternalProcess.class)
                    .info("Executor concluído (código {}).", process.exitValue());
            return new CollectionExecutionResult(
                    process.exitValue(),
                    Files.isRegularFile(output) && Files.size(output) > 0,
                    diagnostics.hints());
        } catch (InterruptedException e) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private static void terminate(Process p) {
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
        try {
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
