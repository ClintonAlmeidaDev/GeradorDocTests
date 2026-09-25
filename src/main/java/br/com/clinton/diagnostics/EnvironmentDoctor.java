package br.com.clinton.diagnostics;

import br.com.clinton.config.CliOptions;
import br.com.clinton.executor.*;
import br.com.clinton.runner.CollectionDetector;

import com.microsoft.playwright.*;

import org.slf4j.*;

import java.nio.file.*;
import java.util.*;

/** Local installation check: never runs the user's collection or calls its APIs. */
public final class EnvironmentDoctor {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentDoctor.class);

    public int check(CliOptions options) throws Exception {
        LOG.info(
                "Diagnóstico local: Java {}. Nenhuma request da collection será executada.",
                System.getProperty("java.version"));
        String runner = options.get("runner");
        if (options.get("collection") != null)
            runner = CollectionDetector.detect(Path.of(options.get("collection")), runner);
        if (runner.equals("auto"))
            throw new IllegalArgumentException(
                    "Para --doctor, informe --runner=bruno|postman ou --collection para detecção"
                        + " automática.");
        String variable = runner.equals("bruno") ? "BRU_EXECUTABLE" : "NEWMAN_EXECUTABLE";
        var probe =
                RunnerProbe.check(
                        System.getenv()
                                .getOrDefault(variable, runner.equals("bruno") ? "bru" : "newman"),
                        variable);
        LOG.info("Runner {} versão {}: OK ({})", runner, probe.version(), probe.command().mode());
        Files.createDirectories(options.outputDir());
        Path test = Files.createTempFile(options.outputDir(), "doctor-", ".pdf");
        try {
            try (Playwright playwright =
                    Playwright.create(
                            new Playwright.CreateOptions()
                                    .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))) {
                try (Browser browser = playwright.chromium().launch()) {
                    Page page = browser.newPage();
                    page.route("**/*", route -> route.abort());
                    page.setContent("<html><body>GeradorDocsTests - instalação OK</body></html>");
                    page.pdf(new Page.PdfOptions().setPath(test));
                }
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Chromium/PDF indisponível. Execute scripts/setup.ps1 -SkipBuild no Windows"
                            + " ou java -cp <jar> com.microsoft.playwright.CLI install chromium. Em"
                            + " rede corporativa, confira proxy/CA e PLAYWRIGHT_DOWNLOAD_HOST.");
            }
            if (Files.size(test) == 0)
                throw new IllegalStateException("Chromium gerou PDF vazio no diagnóstico.");
            LOG.info(
                    "Diretório de saída gravável e Chromium/PDF: OK. Instalação pronta; valide"
                        + " depois ambiente, pasta e conectividade da API.");
            return 0;
        } finally {
            Files.deleteIfExists(test);
        }
    }
}
