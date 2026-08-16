package br.com.clinton.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HtmlReportGenerator {

    public String generate(String jsonPath) throws IOException {
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

        ClassLoaderTemplateResolver resolver =
                new ClassLoaderTemplateResolver();

        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        return templateEngine.process(
                "report-template",
                context
        );
    }
}