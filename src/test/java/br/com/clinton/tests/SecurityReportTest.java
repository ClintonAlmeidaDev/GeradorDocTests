package br.com.clinton.tests;

import static org.junit.jupiter.api.Assertions.*;

import br.com.clinton.model.*;
import br.com.clinton.parser.*;
import br.com.clinton.report.*;
import br.com.clinton.sanitizer.*;

import org.junit.jupiter.api.*;

import java.nio.file.*;
import java.util.*;

class SecurityReportTest {
    @Test
    void cookieAttributesDoNotMaskOrdinaryUrls() {
        var report = new AuditReport();
        var request = new br.com.clinton.model.RequestExecution();
        request.setResponseHeaders(
                Map.of("Set-Cookie", "session=secret-cookie; Path=/; SameSite=Lax"));
        request.setUrl("https://example.test/posts/1");
        request.setTechnicalError("echo secret-cookie");
        report.setExecutions(List.of(request));
        var clean = new SensitiveDataSanitizer().sanitizeReport(report).getExecutions().getFirst();
        assertEquals(request.getUrl(), clean.getUrl());
        assertEquals("echo ••••••••", clean.getTechnicalError());
    }

    @Test
    void redactsAuthorizationAndCookieValuesEchoedInErrors() {
        var report = new AuditReport();
        var request = new br.com.clinton.model.RequestExecution();
        request.setRequestHeaders(
                Map.of(
                        "Authorization",
                        "Bearer bearer-private",
                        "Cookie",
                        "session=cookie-private; other=second-private"));
        request.setTechnicalError("echo bearer-private cookie-private second-private xml-private");
        request.setRequestBody("<root><password>xml-private</password></root>");
        report.setExecutions(List.of(request));
        var sanitized =
                new SensitiveDataSanitizer().sanitizeReport(report).getExecutions().getFirst();
        assertEquals("echo •••••••• •••••••• •••••••• ••••••••", sanitized.getTechnicalError());
        assertFalse(sanitized.getRequestBody().toString().contains("xml-private"));
    }

    @Test
    void masksNestedKeysHeadersJsonNodesAndCustomFields() throws Exception {
        var s = new SensitiveDataSanitizer(List.of("cpf"));
        var payload =
                Map.of(
                        "nested",
                        List.of(
                                Map.of(
                                        "Client-Secret",
                                        "one",
                                        "access_token",
                                        "two",
                                        "ACCESS-TOKEN",
                                        "three",
                                        "accountId",
                                        42,
                                        "cpf",
                                        "four")),
                        "headers",
                        List.of(Map.of("key", "Authorization", "value", "five")),
                        "Cookie",
                        "six");
        String result =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(s.sanitize(payload));
        for (String secret : List.of("one", "two", "three", "four", "five", "six"))
            assertFalse(result.contains(secret));
        assertTrue(result.contains("42"));
        assertTrue(result.contains(SensitiveDataSanitizer.MASK));
        assertEquals(
                Map.of("password", SensitiveDataSanitizer.MASK),
                s.sanitize(
                        new com.fasterxml.jackson.databind.ObjectMapper()
                                .readTree("{\"password\":\"hidden\"}")));
    }

    @Test
    void urlsTextAndHeaderLines() {
        var s = new SensitiveDataSanitizer();
        assertEquals(
                "https://example.test?q=ok&access_token=••••••••",
                s.sanitizeText("https://example.test?q=ok&access_token=hidden"));
        assertFalse(s.sanitizeText("https://user:secret@example.test/").contains("user:secret"));
        assertFalse(
                s.sanitizeText("Authorization: Bearer hidden\nX-API-Key: other")
                        .contains("hidden"));
        assertEquals(
                "<password>••••••••</password>", s.sanitizeText("<password>hidden</password>"));
        assertEquals(
                "password=••••••••&normal=value", s.sanitizeText("password=hidden&normal=value"));
    }

    @Test
    void prettyJsonAndPlainText() {
        var f = new JsonPayloadFormatter();
        assertEquals("", f.format(null));
        assertEquals("<xml>test</xml>", f.format("<xml>test</xml>"));
        assertTrue(f.format(Map.of("one", 1)).contains("\n"));
        assertTrue(f.format(List.of(1, 2)).contains("1"));
        assertTrue(f.format("{\"password\":\"hidden\"}").contains("••••••••"));
        assertFalse(f.format("{\"password\":\"hidden\"}").contains("hidden"));
    }

    @Test
    void htmlSanitizesEntireReportAndEscapesMarkup() throws Exception {
        var report =
                new BrunoResultParser()
                        .parse(
                                Path.of(getClass().getResource("/results/bruno.json").toURI())
                                        .toString());
        var meta = new ReportMetadata();
        meta.setCollectionName("<script>alert(1)</script>");
        meta.setCompanyName("Example");
        report.setMetadata(meta);
        report.getExecutions()
                .getFirst()
                .getAssertions()
                .getFirst()
                .setErrorMessage("echo fictional-secret");
        String html = new HtmlReportGenerator().generate(report);
        assertFalse(html.contains("fictional-secret"));
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("Evidência de Teste de API"));
        assertTrue(html.contains("Rastreabilidade"));
        assertTrue(html.contains("Payload Recebido"));
        assertTrue(report.getExecutions().getFirst().getUrl().contains("fictional-secret"));
    }
}
