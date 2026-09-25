package br.com.clinton.runner;

import br.com.clinton.config.CliOptions;
import br.com.clinton.context.ExecutionContextCollector;
import br.com.clinton.history.RunManifest;
import br.com.clinton.openapi.OpenApiBootstrap;

import org.slf4j.*;

import java.nio.file.Path;
import java.util.*;

public final class AuditCli {
    private static final Logger LOG = LoggerFactory.getLogger(AuditCli.class);

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String... args) {
        try {
            if (args.length > 0 && args[0].equals("compare")) {
                if (args.length != 3)
                    throw new IllegalArgumentException(
                            "Uso: compare runA.audit-run.json runB.audit-run.json");
                System.out.print(RunManifest.compare(Path.of(args[1]), Path.of(args[2])));
                return 0;
            }
            CliOptions o = CliOptions.parse(args, System.getenv());
            if (o.flag("help")) {
                System.out.println(HELP);
                return 0;
            }
            if (o.flag("version")) {
                System.out.println("GeradorDocsTests " + ExecutionContextCollector.version());
                return 0;
            }
            int code;
            if (o.get("openapi") != null) {
                new OpenApiBootstrap()
                        .generate(
                                Path.of(o.get("openapi")),
                                o.get("generate", "postman"),
                                o.outputDir());
                code = 0;
            } else code = new AuditService().execute(o);
            LOG.info("Exit code: {}", code);
            return code;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Parser and browser exception messages can contain unmasked input. Only curated errors
            // are printed.
            String message =
                    e.getClass() == IllegalArgumentException.class
                                    || e.getClass() == IllegalStateException.class
                            ? e.getMessage()
                            : "Erro técnico ("
                                    + e.getClass().getSimpleName()
                                    + "). Consulte docs/TROUBLESHOOTING.md e valide os"
                                    + " pré-requisitos.";
            LOG.error(
                    "{} Exit code: 2",
                    new br.com.clinton.sanitizer.SensitiveDataSanitizer().sanitizeText(message));
            return 2;
        }
    }

    private static final String HELP =
            """
            GeradorDocsTests - evidência de testes de API
            java -jar gerador-docs-tests.jar --collection PATH [opções]
            --runner auto|bruno|postman       Detecção automática por padrão
            --environment HML               Rótulo do relatório (default LOCAL)
            --folder PATH                   Pasta da collection (ex.: HOMOLOGACAO)
            --bruno-env NAME                 Ambiente executado pelo Bruno
            --environment-file PATH         Environment Newman/Bruno
            --company NAME --executor NAME   Empresa e responsável
            --output-dir PATH               Default audit-output
            --pdf-output PATH               Caminho PDF explícito, sem sobrescrever
            --json-output PATH              JSON normalizado SANITIZADO
            --keep-raw-results               Mantém JSON bruto sensível para diagnóstico
            --mask-key FIELD                Repetível; normaliza hífens e underscores
            --config PATH                   Arquivo .properties UTF-8
            --timeout-seconds N             Timeout do processo (default 1800)
            --tool-arg=ARG                   Um argumento literal, repetível
            -- [args...]                    Argumentos adicionais Bruno/Newman
            --openapi FILE --generate bruno|postman --output-dir DIR
            compare runA.audit-run.json runB.audit-run.json
            --help --version
            Exit codes: 0 PASS; 1 FAIL com PDF; 2 erro técnico/configuração.
            """;
}
