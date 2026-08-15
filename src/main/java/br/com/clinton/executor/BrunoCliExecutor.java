package br.com.clinton.executor;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class BrunoCliExecutor {

    private static final String BRU_EXECUTABLE =
            "/home/clintonalmeida/.nvm/versions/node/v22.23.2/bin/bru";

    private static final String NODE_BIN_PATH =
            "/home/clintonalmeida/.nvm/versions/node/v22.23.2/bin";

    public void execute(
            String collectionDirStr,
            String outputPathStr
    ) throws IOException, InterruptedException {

        System.out.println("Executando coleção Bruno via CLI...");

        File collectionDir = new File(collectionDirStr).getAbsoluteFile();
        File outputFile = new File(outputPathStr).getAbsoluteFile();

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                BRU_EXECUTABLE,
                "run",
                "--reporter-json",
                outputFile.getAbsolutePath()
        );

        processBuilder.directory(collectionDir);

        Map<String, String> env = processBuilder.environment();

        env.put(
                "PATH",
                NODE_BIN_PATH + File.pathSeparator + env.get("PATH")
        );

        env.put(
                "NODE_OPTIONS",
                "--experimental-global-webcrypto"
        );

        processBuilder.inheritIO();

        Process process = processBuilder.start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.out.println(
                    "Avisos ou falhas detectadas durante os testes do Bruno " +
                            "(Exit code: " + exitCode + ")."
            );
        }
    }
}