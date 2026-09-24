package br.com.clinton.executor;

public class NewmanExecutorSmokeTest {

    public static void main(String[] args)
            throws Exception {

        CollectionExecutor executor =
                new NewmanExecutor();

        CollectionExecutionResult result =
                executor.execute(
                        "./src/test/resources/postman-collection/audit-test.postman_collection.json",
                        "./target/newman-results-java.json"
                );

        System.out.println(
                "Newman exit code: "
                        + result.getExitCode()
        );

        System.out.println(
                "Relatório JSON gerado: "
                        + result.isReportGenerated()
        );
    }
}