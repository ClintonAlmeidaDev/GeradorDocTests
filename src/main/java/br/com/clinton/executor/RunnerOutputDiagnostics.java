package br.com.clinton.executor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Classifies a bounded window of untrusted output. Only fixed messages can leave this class. */
final class RunnerOutputDiagnostics {
    private final Set<String> hints = new LinkedHashSet<>();

    void drain(InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            char[] chunk = new char[2048];
            String tail = "";
            int count;
            while ((count = reader.read(chunk)) != -1) {
                tail = tail + new String(chunk, 0, count);
                if (tail.length() > 8192) tail = tail.substring(tail.length() - 8192);
                classify(tail.toLowerCase(Locale.ROOT));
            }
        }
    }

    private synchronized void classify(String text) {
        if (text.contains("unknown argument")
                || text.contains("unknown option")
                || text.contains("unrecognized option"))
            hints.add(
                    "O runner indicou argumento desconhecido. Confira a versão instalada e os"
                        + " argumentos adicionais.");
        if (text.contains("cannot find module") || text.contains("module_not_found"))
            hints.add(
                    "O Node indicou módulo ausente. Reinstale o pacote npm do runner no prefixo"
                        + " utilizado.");
        if (text.contains("eacces")
                || text.contains("eperm")
                || text.contains("access is denied")
                || text.contains("permission denied"))
            hints.add(
                    "O runner indicou acesso negado. Confira permissões do diretório temporário,"
                        + " antivírus e política corporativa.");
        if (text.contains("environment")
                && (text.contains("not found") || text.contains("does not exist")))
            hints.add(
                    "O runner indicou ambiente não encontrado. Confira --bruno-env ou"
                        + " --environment-file; --environment é apenas um rótulo.");
        if (text.contains("certificate") || text.contains("self-signed"))
            hints.add(
                    "O runner indicou problema de certificado. Configure a CA corporativa e o proxy"
                        + " aprovados pela empresa.");
        if (text.contains("yamlparseerror")
                || text.contains("unexpected scalar")
                || text.contains("parse error"))
            hints.add(
                    "O runner indicou erro de parsing. Confira YAML/JSON da collection e"
                        + " compatibilidade com a versão instalada.");
    }

    synchronized List<String> hints() {
        return List.copyOf(hints);
    }
}
