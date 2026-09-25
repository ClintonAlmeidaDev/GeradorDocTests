package br.com.clinton.executor;

public class CollectionExecutionResult {

    private final java.util.List<String> diagnostics;

    public java.util.List<String> getDiagnostics() {
        return diagnostics;
    }

    private final int exitCode;
    private final boolean reportGenerated;

    public CollectionExecutionResult(int exitCode, boolean reportGenerated) {
        this(exitCode, reportGenerated, java.util.List.of());
    }

    public CollectionExecutionResult(
            int exitCode, boolean reportGenerated, java.util.List<String> diagnostics) {
        this.diagnostics = java.util.List.copyOf(diagnostics);
        this.exitCode = exitCode;
        this.reportGenerated = reportGenerated;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isReportGenerated() {
        return reportGenerated;
    }

    public boolean isSuccessful() {
        return exitCode == 0;
    }
}
