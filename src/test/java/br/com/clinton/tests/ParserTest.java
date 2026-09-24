package br.com.clinton.tests;

import static org.junit.jupiter.api.Assertions.*;

import br.com.clinton.model.*;
import br.com.clinton.parser.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

class ParserTest {
    @TempDir Path temp;

    String fixture(String name) throws Exception {
        return Path.of(getClass().getResource("/results/" + name).toURI()).toString();
    }

    @Test
    void brunoAllIterationsAndAssertionFailures() throws Exception {
        var r = new BrunoResultParser().parse(fixture("bruno.json"));
        assertEquals(4, r.getSummary().getTotalRequests());
        assertEquals(2, r.getSummary().getFailedRequests());
        assertEquals(8, r.getSummary().getTotalAssertions());
        assertEquals(6, r.getSummary().getSuccessfulAssertions());
        assertNull(r.getExecutions().getFirst().getRequestBody());
        assertInstanceOf(Map.class, r.getExecutions().get(1).getRequestBody());
        assertInstanceOf(Map.class, r.getExecutions().getFirst().getResponseBody());
        assertNull(r.getExecutions().get(1).getResponseBody());
        assertEquals(
                "expected 201 to equal 200",
                r.getExecutions().get(1).getAssertions().getFirst().getErrorMessage());
    }

    @Test
    void newmanBufferUtf8QueryAndSkipped() throws Exception {
        var r = new PostmanResultParser().parse(fixture("newman.json"));
        assertEquals(2, r.getSummary().getTotalRequests());
        assertEquals(1, r.getSummary().getFailedRequests());
        assertEquals(3, r.getSummary().getTotalAssertions());
        assertEquals(2, r.getSummary().getFailedAssertions());
        var get = r.getExecutions().getFirst();
        assertEquals(
                "https://example.test:8443/v1/posts?q=one&token=fictional-secret", get.getUrl());
        assertEquals("Olá", ((Map<?, ?>) get.getResponseBody()).get("title"));
        assertNull(get.getRequestBody());
        var post = r.getExecutions().get(1);
        assertInstanceOf(Map.class, post.getRequestBody());
        assertEquals("<xml>text</xml>", post.getResponseBody());
        assertTrue(post.getAssertions().get(1).isSkipped());
        assertFalse(post.isSuccessful());
        assertEquals(201, post.getHttpStatus());
    }

    @Test
    void technicalErrorAndNonJsonBodies() throws Exception {
        Path file = temp.resolve("r.json");
        Files.writeString(
                file,
                """
[{"results":[{"status":"pass","error":"connection reset","request":{"data":"<xml>ok</xml>"},"response":{"status":200},"testResults":[{"status":"fail","description":"script","error":{"message":"broken"}}]}]}]
""");
        var r = new BrunoResultParser().parse(file.toString()).getExecutions().getFirst();
        assertFalse(r.isSuccessful());
        assertEquals("<xml>ok</xml>", r.getRequestBody());
        assertEquals("broken", r.getAssertions().getFirst().getErrorMessage());
    }

    @Test
    void newmanMissingResponseAndGlobalFailure() throws Exception {
        Path file = temp.resolve("r.json");
        Files.writeString(
                file,
                """
{"run":{"failures":[{"error":{"message":"script error"}}],"executions":[{"request":{"url":{"raw":"https://example.test?q=1"},"body":{"raw":"text"}},"requestError":{"message":"DNS failed"}}]}}
""");
        var r = new PostmanResultParser().parse(file.toString());
        assertTrue(r.isExecutionFailed());
        assertFalse(r.getExecutions().getFirst().isSuccessful());
        assertEquals("text", r.getExecutions().getFirst().getRequestBody());
    }

    @Test
    void malformedAndEmptyResultsRejected() throws Exception {
        Path f = temp.resolve("invalid.json");
        for (String data : List.of("{}", "[]", "[{\"results\":[]}]", "null")) {
            Files.writeString(f, data);
            assertThrows(Exception.class, () -> new BrunoResultParser().parse(f.toString()));
        }
        Files.writeString(f, "{\"run\":{\"executions\":[]}}");
        assertThrows(Exception.class, () -> new PostmanResultParser().parse(f.toString()));
    }
}
