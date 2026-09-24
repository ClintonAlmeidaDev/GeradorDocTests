package br.com.clinton.report;

import br.com.clinton.model.AuditReport;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class HtmlReportGenerator {

    public String generate(AuditReport auditReport) {

        auditReport =
                new br.com.clinton.sanitizer.SensitiveDataSanitizer().sanitizeReport(auditReport);
        Context context = new Context();
        context.setVariable("executionFailed", auditReport.isExecutionFailed());
        JsonPayloadFormatter jsonFormatter = new JsonPayloadFormatter();

        context.setVariable("results", auditReport.getExecutions());
        context.setVariable("summary", auditReport.getSummary());
        context.setVariable("metadata", auditReport.getMetadata());
        context.setVariable("jsonFormatter", jsonFormatter);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();

        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        return templateEngine.process("report-template", context);
    }
}
