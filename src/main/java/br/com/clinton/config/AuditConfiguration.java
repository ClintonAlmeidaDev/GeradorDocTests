package br.com.clinton.config;

public class AuditConfiguration {

    private final String collectionPath;
    private final String jsonOutputPath;
    private final String pdfOutputPath;

    private final String collectionName;
    private final String environment;
    private final String executor;
    private final String companyName;

    public AuditConfiguration(
            String collectionPath,
            String jsonOutputPath,
            String pdfOutputPath,
            String collectionName,
            String environment,
            String executor,
            String companyName
    ) {
        this.collectionPath = collectionPath;
        this.jsonOutputPath = jsonOutputPath;
        this.pdfOutputPath = pdfOutputPath;
        this.collectionName = collectionName;
        this.environment = environment;
        this.executor = executor;
        this.companyName = companyName;
    }

    public String getCollectionPath() {
        return collectionPath;
    }

    public String getJsonOutputPath() {
        return jsonOutputPath;
    }

    public String getPdfOutputPath() {
        return pdfOutputPath;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getExecutor() {
        return executor;
    }

    public String getCompanyName() {
        return companyName;
    }
}