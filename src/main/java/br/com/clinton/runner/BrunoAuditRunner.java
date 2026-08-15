package br.com.clinton.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import br.com.clinton.executor.BrunoCliExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class BrunoAuditRunner {

    private static final String BRUNO_COLLECTION_DIR = "./src/test/resources/bruno-collection";
    private static final String JSON_OUTPUT_PATH = "./target/bruno-results.json";
    private static final String PDF_OUTPUT_PATH = "./Relatorio_Auditoria_Testes.pdf";

    public static void main(String[] args) {
        try {
            BrunoCliExecutor brunoCliExecutor = new BrunoCliExecutor();

            brunoCliExecutor.execute(
                    BRUNO_COLLECTION_DIR,
                    JSON_OUTPUT_PATH
            );
            System.out.println("Formatando template HTML do relatório...");
            String htmlContent = generateHtmlReport(JSON_OUTPUT_PATH);

            System.out.println("Renderizando PDF de alta resolução com Playwright...");
            generatePdfFromHtml(htmlContent, PDF_OUTPUT_PATH);

            System.out.println("Processo concluído com sucesso! PDF gerado em: " + PDF_OUTPUT_PATH);

        } catch (Exception e) {
            System.err.println("Falha na geração do relatório de auditoria.");
            e.printStackTrace();
        }
    }

    private static String generateHtmlReport(String jsonPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode rootNode = mapper.readTree(new File(jsonPath));

        if (!rootNode.isArray() || rootNode.isEmpty()) {
            throw new IllegalStateException(
                    "JSON do Bruno não possui execuções."
            );
        }

        JsonNode executionNode = rootNode.get(0);

        List<Map<String, Object>> results = mapper.convertValue(
                executionNode.path("results"),
                new TypeReference<List<Map<String, Object>>>() {}
        );

        Map<String, Object> summary = mapper.convertValue(
                executionNode.path("summary"),
                new TypeReference<Map<String, Object>>() {}
        );

        Context context = new Context();

        context.setVariable("results", results);
        context.setVariable("summary", summary);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        return templateEngine.process("report-template", context);
    }

    private static void generatePdfFromHtml(String htmlContent, String outputPath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            page.setContent(htmlContent, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            page.pdf(new Page.PdfOptions()
                    .setPath(Paths.get(outputPath))
                    .setPrintBackground(true)
            );

            browser.close();
        }
    }
}