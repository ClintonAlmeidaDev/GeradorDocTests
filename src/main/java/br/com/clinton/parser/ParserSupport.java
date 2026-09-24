package br.com.clinton.parser;

import br.com.clinton.model.*;

import com.fasterxml.jackson.databind.*;

import java.util.*;

final class ParserSupport {
    static final ObjectMapper JSON = new ObjectMapper();

    static Object body(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (!node.isTextual()) return JSON.convertValue(node, Object.class);
        String text = node.asText();
        if (text.isEmpty()) return null;
        try {
            return JSON.readValue(text, Object.class);
        } catch (Exception ignored) {
            return text;
        }
    }

    static String error(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return "";
        return node.isTextual() ? node.asText() : node.path("message").asText(node.toString());
    }

    static AuditReport report(List<RequestExecution> requests) {
        if (requests.isEmpty())
            throw new IllegalArgumentException("Resultado sem requisições executadas.");
        ExecutionSummary summary = new ExecutionSummary();
        summary.setTotalRequests(requests.size());
        summary.setSuccessfulRequests(
                (int) requests.stream().filter(RequestExecution::isSuccessful).count());
        summary.setFailedRequests(requests.size() - summary.getSuccessfulRequests());
        var assertions = requests.stream().flatMap(r -> r.getAssertions().stream()).toList();
        summary.setTotalAssertions(assertions.size());
        summary.setSuccessfulAssertions(
                (int) assertions.stream().filter(AssertionResult::isSuccessful).count());
        summary.setFailedAssertions(assertions.size() - summary.getSuccessfulAssertions());
        AuditReport report = new AuditReport();
        report.setExecutions(requests);
        report.setSummary(summary);
        return report;
    }
}
