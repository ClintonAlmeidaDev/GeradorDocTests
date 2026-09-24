package br.com.clinton.model;

public class ReportMetadata {
    private java.util.Map<String, String> traceability = new java.util.LinkedHashMap<>();

    public java.util.Map<String, String> getTraceability() {
        return traceability;
    }

    public void setTraceability(java.util.Map<String, String> value) {
        this.traceability = value;
    }

    private String collectionName;
    private String executionDate;
    private String environment;
    private String executor;
    private String companyName;

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(String executionDate) {
        this.executionDate = executionDate;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
