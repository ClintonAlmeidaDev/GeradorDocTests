package br.com.clinton.parser;

import br.com.clinton.model.AuditReport;

import java.io.IOException;

public interface ResultParser {

    AuditReport parse(String resultPath) throws IOException;
}
