package br.com.clinton.auditor;

import com.microsoft.playwright.*;

import java.nio.file.Paths;

public class AuditReportGenerator {
    public static void generatePdfFromHtml(String htmlContent, String outPutPath){
        try (Playwright playwright = Playwright.create()){

            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            page.setContent(htmlContent);
            page.pdf(new Page.PdfOptions()
                    .setPath(Paths.get(outPutPath))
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new com.microsoft.playwright.options.Margin()
                            .setTop("15mm")
                            .setBottom("15mm")
                            .setLeft("10mm")
                            .setRight("10mm")));

            browser.close();
            System.out.println("Relatorio PDF gerado com sucesso em: " + outPutPath);
        }
    }

}
