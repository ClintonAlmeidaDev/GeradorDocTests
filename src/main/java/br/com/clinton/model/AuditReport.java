package br.com.clinton.model;

import java.util.List;

public class AuditReport {

    private List<RequestExecution> executions;
    private ExecutionSummary summary;

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