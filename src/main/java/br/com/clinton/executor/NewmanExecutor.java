package br.com.clinton.executor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class NewmanExecutor implements CollectionExecutor {
    private final String executable;
    private final List<String> extra;
    private final long timeout;

    public NewmanExecutor() {
        this(System.getenv().getOrDefault("NEWMAN_EXECUTABLE", "newman"));
    }

    public NewmanExecutor(String executable) {
        this(executable, List.of(), 1800);
    }

    public NewmanExecutor(String executable, List<String> extra, long timeout) {
        this.executable = executable;
        this.extra = List.copyOf(extra);
        this.timeout = timeout;
    }

    @Override
    public CollectionExecutionResult execute(String collection, String output)
            throws IOException, InterruptedException {
        for (String arg : extra)
            if (arg.startsWith("-r"))
                throw new IllegalArgumentException(
                        "Newman: -r seleciona reporters e é gerenciado pela ferramenta. Use"
                            + " --folder para selecionar uma pasta.");
        Path file = Path.of(collection).toAbsolutePath();
        if (!Files.isRegularFile(file)) throw new IOException("Collection Postman inexistente.");
        Path out = Path.of(output).toAbsolutePath();
        List<String> args = new ArrayList<>(List.of("run", file.toString()));
        args.addAll(extra);
        args.addAll(List.of("--reporters", "cli,json", "--reporter-json-export", out.toString()));
        return ExternalProcess.run(
                executable, "NEWMAN_EXECUTABLE", file.getParent(), out, args, timeout);
    }
}
