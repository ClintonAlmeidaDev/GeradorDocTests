package br.com.clinton.executor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BrunoCliExecutor implements CollectionExecutor {
    private final String executable;
    private final List<String> extra;
    private final long timeout;
    private final String folder;

    public BrunoCliExecutor() {
        this(System.getenv().getOrDefault("BRU_EXECUTABLE", "bru"));
    }

    public BrunoCliExecutor(String executable) {
        this(executable, List.of(), 1800);
    }

    public BrunoCliExecutor(String executable, List<String> extra, long timeout) {
        this(executable, extra, timeout, ".");
    }

    public BrunoCliExecutor(String executable, List<String> extra, long timeout, String folder) {
        this.folder = folder;
        this.executable = executable;
        this.extra = List.copyOf(extra);
        this.timeout = timeout;
    }

    @Override
    public CollectionExecutionResult execute(String collection, String output)
            throws IOException, InterruptedException {
        Path dir = Path.of(collection).toAbsolutePath();
        if (!Files.isDirectory(dir)) throw new IOException("Diretório Bruno inexistente.");
        Path out = Path.of(output).toAbsolutePath();
        List<String> args = new ArrayList<>(List.of("run", folder, "-r"));
        args.addAll(extra);
        args.addAll(List.of("--reporter-json", out.toString()));
        return ExternalProcess.run(executable, "BRU_EXECUTABLE", dir, out, args, timeout);
    }
}
