package br.com.clinton.auditor;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Margin;

import java.nio.file.Paths;

public class AuditReportGenerator {

    public static void generatePdfFromHtml(String htmlContent, String outputPath) {

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch();

            Page page = browser.newPage();

            page.route("**/*", route -> route.abort());
            page.setContent(htmlContent);

            String footerTemplate =
                    """
                    <div style="
                        width: 100%;
                        font-size: 8px;
                        color: #64748b;
                        text-align: right;
                        padding-right: 10mm;
                        font-family: Arial, sans-serif;
                    ">
                        Página
                        <span class="pageNumber"></span>
                        de
                        <span class="totalPages"></span>
                    </div>
                    """;

            page.pdf(
                    new Page.PdfOptions()
                            .setPath(Paths.get(outputPath))
                            .setFormat("A4")
                            .setPrintBackground(true)
                            .setDisplayHeaderFooter(true)
                            .setHeaderTemplate("<div></div>")
                            .setFooterTemplate(footerTemplate)
                            .setMargin(
                                    new Margin()
                                            .setTop("15mm")
                                            .setBottom("20mm")
                                            .setLeft("10mm")
                                            .setRight("10mm")));

            browser.close();
        }
    }
}
