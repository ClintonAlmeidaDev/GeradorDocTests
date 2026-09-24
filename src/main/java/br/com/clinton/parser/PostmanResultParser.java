package br.com.clinton.parser;

import br.com.clinton.model.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PostmanResultParser implements ResultParser {
    @Override
    public AuditReport parse(String path) throws IOException {
        JsonNode root = ParserSupport.JSON.readTree(new File(path));
        if (root == null || !root.path("run").path("executions").isArray())
            throw new IllegalArgumentException("Formato Newman inválido: run.executions ausente.");
        List<RequestExecution> requests = new ArrayList<>();
        for (JsonNode node : root.path("run").path("executions")) requests.add(parseRequest(node));
        AuditReport report = ParserSupport.report(requests);
        report.setExecutionFailed(!root.path("run").path("failures").isEmpty());
        return report;
    }

    private RequestExecution parseRequest(JsonNode node) {
        JsonNode req = node.path("request"), res = node.path("response");
        RequestExecution r = new RequestExecution();
        r.setName(node.path("item").path("name").asText(""));
        r.setMethod(req.path("method").asText("").toUpperCase(Locale.ROOT));
        r.setUrl(parseUrl(req.path("url")));
        JsonNode body = req.path("body");
        r.setRequestBody(
                ParserSupport.body(
                        body.has("raw")
                                ? body.get("raw")
                                : body.get(body.path("mode").asText("raw"))));
        r.setRequestHeaders(ParserSupport.body(req.get("header")));
        r.setResponseHeaders(ParserSupport.body(res.get("header")));
        r.setHttpStatus(res.path("code").asInt());
        r.setStatusText(res.path("status").asText(""));
        r.setResponseTimeMs(res.path("responseTime").asLong());
        r.setResponseSizeBytes(res.path("responseSize").asLong());
        JsonNode data = res.path("stream").path("data");
        if (data.isArray()) {
            byte[] bytes = new byte[data.size()];
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) data.get(i).asInt();
            r.setResponseBody(
                    ParserSupport.body(
                            ParserSupport.JSON
                                    .getNodeFactory()
                                    .textNode(new String(bytes, StandardCharsets.UTF_8))));
        } else r.setResponseBody(ParserSupport.body(res.get("body")));
        r.setTechnicalError(ParserSupport.error(node.get("requestError")));
        for (JsonNode a : node.path("assertions")) {
            AssertionResult assertion = new AssertionResult();
            assertion.setExpression(a.path("assertion").asText(""));
            assertion.setSkipped(a.path("skipped").asBoolean());
            assertion.setSuccessful(!assertion.isSkipped() && !a.hasNonNull("error"));
            assertion.setErrorMessage(
                    assertion.isSkipped()
                            ? "Asserção não executada (SKIPPED)."
                            : ParserSupport.error(a.get("error")));
            r.getAssertions().add(assertion);
        }
        r.setSuccessful(
                r.getHttpStatus() > 0
                        && r.getTechnicalError().isEmpty()
                        && r.getAssertions().stream().allMatch(AssertionResult::isSuccessful));
        return r;
    }

    static String parseUrl(JsonNode url) {
        if (url.isTextual()) return url.asText();
        if (url.hasNonNull("raw")) return url.path("raw").asText();
        String host = join(url.path("host"), "."), path = join(url.path("path"), "/");
        String value =
                (url.path("protocol").asText("").isEmpty()
                                ? ""
                                : url.path("protocol").asText() + "://")
                        + host;
        if (url.hasNonNull("port")) value += ":" + url.path("port").asText();
        if (!path.isEmpty()) value += "/" + path;
        List<String> query = new ArrayList<>();
        for (JsonNode q : url.path("query"))
            if (!q.path("disabled").asBoolean())
                query.add(
                        q.path("key").asText()
                                + (q.hasNonNull("value") ? "=" + q.path("value").asText() : ""));
        if (!query.isEmpty()) value += "?" + String.join("&", query);
        if (url.hasNonNull("hash")) value += "#" + url.path("hash").asText();
        return value;
    }

    private static String join(JsonNode node, String separator) {
        if (!node.isArray()) return node.asText("");
        List<String> parts = new ArrayList<>();
        node.forEach(n -> parts.add(n.asText()));
        return String.join(separator, parts);
    }
}
