package br.com.clinton.parser;

import br.com.clinton.model.AuditReport;
import br.com.clinton.model.RequestExecution;

public class PostmanResultParserSmokeTest {

    public static void main(String[] args)
            throws Exception {

        ResultParser parser =
                new PostmanResultParser();

        AuditReport report =
                parser.parse(
                        "./target/newman-results-java.json"
                );

        System.out.println(
                "Requests normalizadas: "
                        + report
                        .getSummary()
                        .getTotalRequests()
        );

        System.out.println(
                "Requests com sucesso: "
                        + report
                        .getSummary()
                        .getSuccessfulRequests()
        );

        System.out.println(
                "Requests com falha: "
                        + report
                        .getSummary()
                        .getFailedRequests()
        );

        System.out.println(
                "Assertions: "
                        + report
                        .getSummary()
                        .getTotalAssertions()
        );

        System.out.println(
                "Assertions aprovadas: "
                        + report
                        .getSummary()
                        .getSuccessfulAssertions()
        );

        System.out.println(
                "Assertions falhas: "
                        + report
                        .getSummary()
                        .getFailedAssertions()
        );

        for (RequestExecution request
                : report.getExecutions()) {

            System.out.println(
                    request.getMethod()
                            + " "
                            + request.getUrl()
                            + " -> HTTP "
                            + request.getHttpStatus()
                            + " "
                            + request.getStatusText()
                            + " | "
                            + (
                            request.isSuccessful()
                                    ? "SUCESSO"
                                    : "FALHA"
                    )
            );

            System.out.println(
                    "Request body: "
                            + request.getRequestBody()
            );

            System.out.println(
                    "Response body: "
                            + request.getResponseBody()
            );
        }
    }
}