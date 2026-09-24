package br.com.clinton.executor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewmanExecutor implements CollectionExecutor {

    private final String newmanExecutable;

    public NewmanExecutor() {
        this(
                System.getenv()
                        .getOrDefault(
                                "NEWMAN_EXECUTABLE",
                                "newman"
                        )
        );
    }

    public NewmanExecutor(String newmanExecutable) {
        this.newmanExecutable = newmanExecutable;
    }

    @Override
    public CollectionExecutionResult execute(
            String collectionPath,
            String outputPath
    ) throws IOException, InterruptedException {

        Path output = Paths.get(outputPath);

        if (output.getParent() != null) {
            Files.createDirectories(
                    output.getParent()
            );
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        newmanExecutable,
                        "run",
                        collectionPath,
                        "--reporters",
                        "cli,json",
                        "--reporter-json-export",
                        outputPath
                );

        configurePath(processBuilder);

        processBuilder.inheritIO();

        Process process =
                processBuilder.start();

        int exitCode =
                process.waitFor();

        boolean reportGenerated =
                Files.exists(output);

        if (exitCode == 0) {

            System.out.println(
                    "Execução Newman concluída com sucesso."
            );

        } else {

            System.out.println(
                    "Avisos ou falhas detectadas durante os testes "
                            + "do Newman (Exit code: "
                            + exitCode
                            + ")."
            );
        }

        return new CollectionExecutionResult(
                exitCode,
                reportGenerated
        );
    }

    private void configurePath(
            ProcessBuilder processBuilder
    ) {

        File executable =
                new File(newmanExecutable);

        if (!executable.isAbsolute()) {
            return;
        }

        File parent =
                executable.getParentFile();

        if (parent == null) {
            return;
        }

        String currentPath =
                processBuilder.environment()
                        .getOrDefault(
                                "PATH",
                                ""
                        );

        processBuilder.environment()
                .put(
                        "PATH",
                        parent.getAbsolutePath()
                                + File.pathSeparator
                                + currentPath
                );
    }
}