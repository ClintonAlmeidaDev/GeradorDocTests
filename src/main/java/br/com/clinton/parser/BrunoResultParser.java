package br.com.clinton.parser;

import br.com.clinton.model.AuditReport;
import br.com.clinton.model.ExecutionSummary;
import br.com.clinton.model.RequestExecution;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrunoResultParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        if (responseNode.has("data")
                && !responseNode.get("data").isNull()) {

            Object responseBody = objectMapper.convertValue(
                    responseNode.get("data"),
                    Object.class
            );

            execution.setResponseBody(responseBody);
        }

        JsonNode errorNode = resultNode.get("error");

        boolean hasError =
                errorNode != null && !errorNode.isNull();

        boolean passed =
                "pass".equalsIgnoreCase(
                        resultNode.path("status").asText("")
                );

        execution.setSuccessful(
                passed && !hasError
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

        ExecutionSummary summary =
                new ExecutionSummary();

        summary.setTotalRequests(totalRequests);
        summary.setSuccessfulRequests(successfulRequests);
        summary.setFailedRequests(failedRequests);

        return summary;
    }
}