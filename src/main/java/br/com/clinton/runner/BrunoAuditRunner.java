package br.com.clinton.runner;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

import br.com.clinton.executor.BrunoCliExecutor;
import br.com.clinton.report.HtmlReportGenerator;

import br.com.clinton.auditor.AuditReportGenerator;

import java.nio.file.Paths;

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

            HtmlReportGenerator htmlReportGenerator =
                    new HtmlReportGenerator();

            String htmlContent =
                    htmlReportGenerator.generate(JSON_OUTPUT_PATH);

            System.out.println("Renderizando PDF de alta resolução com Playwright...");
            AuditReportGenerator.generatePdfFromHtml(htmlContent, PDF_OUTPUT_PATH);

            System.out.println("Processo concluído com sucesso! PDF gerado em: " + PDF_OUTPUT_PATH);

        } catch (Exception e) {
            System.err.println("Falha na geração do relatório de auditoria.");
            e.printStackTrace();
        }
    }

}