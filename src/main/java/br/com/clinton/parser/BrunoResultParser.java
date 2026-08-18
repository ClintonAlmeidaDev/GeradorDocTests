package br.com.clinton.parser;

import br.com.clinton.model.AuditReport;
import br.com.clinton.model.ExecutionSummary;
import br.com.clinton.model.RequestExecution;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.clinton.model.AssertionResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrunoResultParser implements ResultParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AuditReport parse(String jsonPath) throws IOException {

        JsonNode rootNode = objectMapper.readTree(
                new File(jsonPath)
        );

        if (!rootNode.isArray() || rootNode.isEmpty()) {
            throw new IllegalStateException(
                    "JSON do Bruno não possui execuções."
            );
        }

        JsonNode executionNode = rootNode.get(0);

        JsonNode resultsNode = executionNode.path("results");

        if (!resultsNode.isArray()) {
            throw new IllegalStateException(
                    "JSON do Bruno não possui uma lista de resultados."
            );
        }

        List<RequestExecution> executions = new ArrayList<>();

        for (JsonNode resultNode : resultsNode) {
            executions.add(
                    parseRequestExecution(resultNode)
            );
        }

        ExecutionSummary summary =
                createSummary(executions);

        AuditReport auditReport = new AuditReport();

        auditReport.setExecutions(executions);
        auditReport.setSummary(summary);

        return auditReport;
    }



    private RequestExecution parseRequestExecution(
            JsonNode resultNode
    ) {



        JsonNode requestNode =
                resultNode.path("request");

        JsonNode responseNode =
                resultNode.path("response");

        RequestExecution execution =
                new RequestExecution();

        List<AssertionResult> assertions =
                parseAssertions(resultNode);

        execution.setAssertions(assertions);

        execution.setName(
                resultNode.path("name").asText("")
        );

        execution.setMethod(
                requestNode
                        .path("method")
                        .asText("")
                        .toUpperCase(Locale.ROOT)
        );

        execution.setUrl(
                requestNode.path("url").asText("")
        );

        execution.setHttpStatus(
                responseNode.path("status").asInt(0)
        );

        execution.setStatusText(
                responseNode.path("statusText").asText("")
        );

        execution.setResponseTimeMs(
                responseNode.path("responseTime").asLong(0)
        );

        execution.setResponseSizeBytes(
                responseNode.path("size").asLong(0)
        );

        if (responseNode.has("data")
                && !responseNode.get("data").isNull()) {

            Object responseBody = objectMapper.convertValue(
                    responseNode.get("data"),
                    Object.class
            );

            execution.setResponseBody(responseBody);
        }

        JsonNode errorNode =
                resultNode.get("error");

        boolean hasError =
                errorNode != null
                        && !errorNode.isNull();

        boolean requestPassed =
                "pass".equalsIgnoreCase(
                        resultNode
                                .path("status")
                                .asText("")
                );

        boolean assertionsPassed =
                assertions.stream()
                        .allMatch(
                                AssertionResult::isSuccessful
                        );

        execution.setSuccessful(
                requestPassed
                        && !hasError
                        && assertionsPassed
        );

        return execution;
    }

    private ExecutionSummary createSummary(
            List<RequestExecution> executions
    ) {

        int successfulRequests = (int) executions.stream()
                .filter(RequestExecution::isSuccessful)
                .count();

        int totalRequests = executions.size();

        int failedRequests =
                totalRequests - successfulRequests;

        int totalAssertions =
                executions.stream()
                        .mapToInt(
                                execution ->
                                        execution
                                                .getAssertions()
                                                .size()
                        )
                        .sum();

        ExecutionSummary summary =
                new ExecutionSummary();

        int successfulAssertions =
                (int) executions.stream()
                        .flatMap(
                                execution ->
                                        execution
                                                .getAssertions()
                                                .stream()
                        )
                        .filter(
                                AssertionResult::isSuccessful
                        )
                        .count();

        int failedAssertions =
                totalAssertions
                        - successfulAssertions;

        summary.setTotalRequests(totalRequests);
        summary.setSuccessfulRequests(successfulRequests);
        summary.setFailedRequests(failedRequests);
        summary.setTotalAssertions(
                totalAssertions
        );

        summary.setSuccessfulAssertions(
                successfulAssertions
        );

        summary.setFailedAssertions(
                failedAssertions
        );
        return summary;
    }

    private List<AssertionResult> parseAssertions(
            JsonNode resultNode
    ) {

        List<AssertionResult> assertions =
                new ArrayList<>();

        JsonNode assertionResultsNode =
                resultNode.path("assertionResults");

        if (!assertionResultsNode.isArray()) {
            return assertions;
        }

        for (JsonNode assertionNode : assertionResultsNode) {

            AssertionResult assertion =
                    new AssertionResult();

            assertion.setExpression(
                    assertionNode
                            .path("lhsExpr")
                            .asText("")
            );

            assertion.setOperator(
                    assertionNode
                            .path("operator")
                            .asText("")
            );

            assertion.setExpectedValue(
                    assertionNode
                            .path("rhsOperand")
                            .asText("")
            );

            assertion.setErrorMessage(
                    assertionNode
                            .path("error")
                            .asText("")
            );

            assertion.setSuccessful(
                    "pass".equalsIgnoreCase(
                            assertionNode
                                    .path("status")
                                    .asText("")
                    )
            );

            assertions.add(assertion);
        }

        return assertions;
    }
}