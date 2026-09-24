package br.com.clinton.model;

import java.util.List;

public class AuditReport {
    private boolean executionFailed;

    public boolean isExecutionFailed() {
        return executionFailed;
    }

    public void setExecutionFailed(boolean value) {
        this.executionFailed = value;
    }

    private List<RequestExecution> executions;
    private ExecutionSummary summary;
    private ReportMetadata metadata;

    public ReportMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ReportMetadata metadata) {
        this.metadata = metadata;
    }

    public List<RequestExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<RequestExecution> executions) {
        this.executions = executions;
    }

    public ExecutionSummary getSummary() {
        return summary;
    }

    public void setSummary(ExecutionSummary summary) {
        this.summary = summary;
    }
}
