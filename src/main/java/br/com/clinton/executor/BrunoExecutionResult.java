package br.com.clinton.executor;

public class BrunoExecutionResult {

    private final int exitCode;
    private final boolean reportGenerated;

    public BrunoExecutionResult(
            int exitCode,
            boolean reportGenerated
    ) {
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