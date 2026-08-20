package br.com.clinton.runner;

import br.com.clinton.parser.ResultParser;

import br.com.clinton.model.AuditReport;
import br.com.clinton.model.RequestExecution;
import br.com.clinton.parser.BrunoResultParser;
import br.com.clinton.config.AuditConfiguration;

import br.com.clinton.executor.BrunoCliExecutor;
import br.com.clinton.report.HtmlReportGenerator;

import br.com.clinton.auditor.AuditReportGenerator;
import br.com.clinton.executor.BrunoExecutionResult;

import br.com.clinton.model.ReportMetadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import br.com.clinton.config.AuditConfigurationLoader;

public class BrunoAuditRunner {



    public static void main(String[] args) {
        AuditConfiguration configuration =
                AuditConfigurationLoader.load(args);

        try {
            BrunoCliExecutor brunoCliExecutor = new BrunoCliExecutor();

            BrunoExecutionResult executionResult =
                    brunoCliExecutor.execute(
                            configuration.getCollectionPath(),
                            configuration.getJsonOutputPath()
                    );
            System.out.println(
                    "Bruno exit code: "
                            + executionResult.getExitCode()
            );

            System.out.println(
                    "Relatório JSON gerado: "
                            + executionResult.isReportGenerated()
            );
            ResultParser resultParser =
                    new BrunoResultParser();

            AuditReport auditReport =
                    resultParser.parse(
                            configuration.getJsonOutputPath()
                    );

            ReportMetadata metadata =
                    new ReportMetadata();

            metadata.setCollectionName(
                    configuration.getCollectionName()
            );

            metadata.setExecutionDate(
                    LocalDateTime.now()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "dd/MM/yyyy HH:mm:ss"
                                    )
                            )
            );

            metadata.setEnvironment(
                    configuration.getEnvironment()
            );

            metadata.setExecutor(
                    configuration.getExecutor()
            );

            metadata.setCompanyName(
                    configuration.getCompanyName()
            );

            auditReport.setMetadata(metadata);
            System.out.println(
                    "Requests normalizadas: "
                            + auditReport.getSummary().getTotalRequests()
            );

            System.out.println(
                    "Requests com sucesso: "
                            + auditReport.getSummary().getSuccessfulRequests()
            );

            System.out.println(
                    "Requests com falha: "
                            + auditReport.getSummary().getFailedRequests()
            );

            System.out.println(
                    "Assertions: "
                            + auditReport
                            .getSummary()
                            .getTotalAssertions()
            );

            System.out.println(
                    "Assertions aprovadas: "
                            + auditReport
                            .getSummary()
                            .getSuccessfulAssertions()
            );

            System.out.println(
                    "Assertions falhas: "
                            + auditReport
                            .getSummary()
                            .getFailedAssertions()
            );

            if (!auditReport.getExecutions().isEmpty()) {

                RequestExecution first =
                        auditReport.getExecutions().get(0);

                System.out.println(
                        "Primeira execução normalizada: "
                                + first.getMethod()
                                + " "
                                + first.getUrl()
                                + " -> HTTP "
                                + first.getHttpStatus()
                );
            }

            HtmlReportGenerator htmlReportGenerator =
                    new HtmlReportGenerator();

            String htmlContent =
                    htmlReportGenerator.generate(auditReport);

            System.out.println("Renderizando PDF de alta resolução com Playwright...");
            AuditReportGenerator.generatePdfFromHtml(
                    htmlContent,
                    configuration.getPdfOutputPath()
            );
            System.out.println("Processo concluído com sucesso! PDF gerado em: " + configuration.getPdfOutputPath());

        } catch (Exception e) {
            System.err.println("Falha na geração do relatório de auditoria.");
            e.printStackTrace();
        }
    }

}