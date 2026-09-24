package br.com.clinton.parser;

import br.com.clinton.model.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.util.*;

public class BrunoResultParser implements ResultParser {
    @Override
    public AuditReport parse(String path) throws IOException {
        JsonNode root = ParserSupport.JSON.readTree(new File(path));
        if (root == null || !root.isArray())
            throw new IllegalArgumentException("JSON Bruno deve ter raiz array.");
        List<RequestExecution> requests = new ArrayList<>();
        for (JsonNode iteration : root) {
            if (!iteration.path("results").isArray())
                throw new IllegalArgumentException("Bruno: results ausente.");
            for (JsonNode result : iteration.path("results")) requests.add(parseRequest(result));
        }
        return ParserSupport.report(requests);
    }

    private RequestExecution parseRequest(JsonNode result) {
        JsonNode req = result.path("request"), res = result.path("response");
        RequestExecution r = new RequestExecution();
        r.setName(result.path("name").asText(result.path("test").path("name").asText("")));
        r.setMethod(req.path("method").asText("").toUpperCase(Locale.ROOT));
        r.setUrl(req.path("url").asText(""));
        r.setRequestBody(ParserSupport.body(req.get("data")));
        r.setResponseBody(ParserSupport.body(res.get("data")));
        r.setRequestHeaders(ParserSupport.body(req.get("headers")));
        r.setResponseHeaders(ParserSupport.body(res.get("headers")));
        r.setHttpStatus(res.path("status").asInt());
        r.setStatusText(res.path("statusText").asText(""));
        r.setResponseTimeMs(res.path("responseTime").asLong());
        r.setResponseSizeBytes(res.path("size").asLong());
        r.setTechnicalError(ParserSupport.error(result.get("error")));
        for (String field : List.of("assertionResults", "testResults")) {
            for (JsonNode a : result.path(field)) {
                AssertionResult assertion = new AssertionResult();
                assertion.setExpression(
                        a.path("lhsExpr")
                                .asText(
                                        a.path("description")
                                                .asText(a.path("name").asText("Teste"))));
                assertion.setOperator(a.path("operator").asText(""));
                assertion.setExpectedValue(
                        a.path("rhsOperand").asText(a.path("rhsExpr").asText("")));
                assertion.setErrorMessage(ParserSupport.error(a.get("error")));
                assertion.setSkipped(
                        a.path("skipped").asBoolean()
                                || "skip".equalsIgnoreCase(a.path("status").asText()));
                assertion.setSuccessful(
                        !assertion.isSkipped()
                                && "pass".equalsIgnoreCase(a.path("status").asText())
                                && assertion.getErrorMessage().isEmpty());
                r.getAssertions().add(assertion);
            }
        }
        r.setSuccessful(
                r.getHttpStatus() > 0
                        && "pass".equalsIgnoreCase(result.path("status").asText())
                        && r.getTechnicalError().isEmpty()
                        && r.getAssertions().stream().allMatch(AssertionResult::isSuccessful));
        return r;
    }
}
