package br.com.clinton.executor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

class RunnerOutputDiagnosticsTest {
    @Test
    void classifiesAcrossChunksWithoutRetainingSecrets() throws Exception {
        var diagnostics = new RunnerOutputDiagnostics();
        String secret = "SECRET_TOKEN_123";
        String output =
                "x".repeat(2040)
                        + "unknown option "
                        + secret
                        + "x".repeat(20000)
                        + " certificate "
                        + secret;
        diagnostics.drain(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, diagnostics.hints().size());
        assertTrue(diagnostics.hints().getFirst().contains("argumento desconhecido"));
        assertFalse(diagnostics.hints().toString().contains(secret));
    }

    @Test
    void arbitraryOutputProducesNoDiagnosticLeak() throws Exception {
        var diagnostics = new RunnerOutputDiagnostics();
        diagnostics.drain(
                new ByteArrayInputStream(
                        "Authorization: Bearer secret".getBytes(StandardCharsets.UTF_8)));
        assertTrue(diagnostics.hints().isEmpty());
    }
}
