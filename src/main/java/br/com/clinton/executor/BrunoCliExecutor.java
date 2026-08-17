package br.com.clinton.executor;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class BrunoCliExecutor {

    private final String bruExecutable;

    public BrunoCliExecutor() {
        this(
                System.getenv()
                        .getOrDefault("BRU_EXECUTABLE", "bru")
        );
    }

    public BrunoCliExecutor(String bruExecutable) {
        this.bruExecutable = bruExecutable;
    }

    public void execute(
            String collectionDirStr,
            String outputPathStr
    ) throws IOException, InterruptedException {

        System.out.println("Executando coleção Bruno via CLI...");

        File collectionDir =
                new File(collectionDirStr).getAbsoluteFile();

        File outputFile =
                new File(outputPathStr).getAbsoluteFile();

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        bruExecutable,
                        "run",
                        "--reporter-json",
                        outputFile.getAbsolutePath()
                );

        processBuilder.directory(collectionDir);

        Map<String, String> env =
                processBuilder.environment();

        File bruFile =
                new File(bruExecutable);

        if (bruFile.isAbsolute()
                && bruFile.getParentFile() != null) {

            String currentPath =
                    env.getOrDefault("PATH", "");

            env.put(
                    "PATH",
                    bruFile.getParentFile().getAbsolutePath()
                            + File.pathSeparator
                            + currentPath
            );
        }

        env.put(
                "NODE_OPTIONS",
                "--experimental-global-webcrypto"
        );

        processBuilder.inheritIO();

        Process process =
                processBuilder.start();

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {
            System.out.println(
                    "Avisos ou falhas detectadas durante os testes do Bruno "
                            + "(Exit code: "
                            + exitCode
                            + ")."
            );
        }
    }
}