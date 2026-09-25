package br.com.clinton.executor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Probes only --version. No collection/scripts or raw output are printed. */
public final class RunnerProbe {
    public record Result(String version, RunnerCommandResolver.Command command) {}

    public static Result check(String executable, String variable)
            throws IOException, InterruptedException {
        var command = RunnerCommandResolver.resolve(executable, variable);
        List<String> args = new ArrayList<>(command.prefix());
        args.add("--version");
        ProcessBuilder builder = new ProcessBuilder(args).redirectErrorStream(true);
        builder.environment().clear();
        builder.environment().putAll(command.environment());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Falha ao iniciar o runner no diagnóstico. Confira o launcher, Node e"
                        + " permissões.");
        }
        StringBuilder output = new StringBuilder();
        Thread drain =
                Thread.ofPlatform()
                        .daemon()
                        .start(
                                () -> {
                                    try (var reader =
                                            new InputStreamReader(
                                                    process.getInputStream(),
                                                    StandardCharsets.UTF_8)) {
                                        char[] chars = new char[1024];
                                        int n;
                                        while ((n = reader.read(chars)) != -1)
                                            synchronized (output) {
                                                if (output.length() < 8192)
                                                    output.append(
                                                            chars,
                                                            0,
                                                            Math.min(n, 8192 - output.length()));
                                            }
                                    } catch (IOException ignored) {
                                    }
                                });
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS))
                throw new IllegalStateException("Timeout ao consultar --version do runner.");
            drain.join(2000);
            String captured;
            synchronized (output) {
                captured = output.toString();
            }
            var version =
                    Pattern.compile(
                                    "(?m)^(?:Bru"
                                        + " CLI\\s+|v)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?:\\s|$)")
                            .matcher(captured);
            if (process.exitValue() != 0 || !version.find())
                throw new IllegalStateException(
                        "O runner não respondeu corretamente a --version. Verifique Node e a"
                            + " instalação npm.");
            return new Result(version.group(1), command);
        } finally {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }
}
