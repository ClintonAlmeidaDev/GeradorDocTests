package br.com.clinton.executor;

import java.io.IOException;

public interface CollectionExecutor {

    CollectionExecutionResult execute(
            String collectionPath,
            String outputPath
    ) throws IOException, InterruptedException;
}