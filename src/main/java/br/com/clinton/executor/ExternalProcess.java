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
        Path exe = Path.of(executable);
        String path = System.getenv().getOrDefault("PATH", "");
        if (exe.isAbsolute() && exe.getParent() != null)
            path = exe.getParent() + File.pathSeparator + path;
        boolean found =
                exe.isAbsolute()
                        ? Files.isExecutable(exe)
                        : Arrays.stream(path.split(File.pathSeparator))
                                .anyMatch(
                                        p ->
                                                Files.isExecutable(Path.of(p, executable))
                                                        || Files.isRegularFile(
                                                                Path.of(p, executable + ".cmd")));
        if (!found)
            throw new IllegalStateException(
                    (variable.equals("BRU_EXECUTABLE") ? "Bruno CLI" : "Newman")
                            + " não encontrado. Configure "
                            + variable
                            + " ou PATH.");
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.deleteIfExists(output);
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(args);
        ProcessBuilder builder =
                new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true);
        builder.environment().put("PATH", path);
        Process process = builder.start();
        // Do not relay arbitrary collection console.log / assertion values to CI logs.
        Thread drain =
                Thread.ofPlatform()
                        .daemon()
                        .start(
                                () -> {
                                    try (var stream = process.getInputStream()) {
                                        stream.transferTo(OutputStream.nullOutputStream());
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
                    process.exitValue(), Files.isRegularFile(output) && Files.size(output) > 0);
        } catch (InterruptedException e) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private static void terminate(Process p) {
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
    }
}
