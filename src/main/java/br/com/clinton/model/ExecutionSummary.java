package br.com.clinton.model;

public class ExecutionSummary {

    private int totalRequests;
    private int successfulRequests;
    private int failedRequests;

    private int totalAssertions;
    private int successfulAssertions;
    private int failedAssertions;

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getSuccessfulRequests() {
        return successfulRequests;
    }

    public void setSuccessfulRequests(int successfulRequests) {
        this.successfulRequests = successfulRequests;
    }

    public int getFailedRequests() {
        return failedRequests;
    }

    public void setFailedRequests(int failedRequests) {
        this.failedRequests = failedRequests;
    }

    public int getTotalAssertions() {
        return totalAssertions;
    }

    public void setTotalAssertions(int totalAssertions) {
        this.totalAssertions = totalAssertions;
    }

    public int getSuccessfulAssertions() {
        return successfulAssertions;
    }

    public void setSuccessfulAssertions(int successfulAssertions) {
        this.successfulAssertions = successfulAssertions;
    }

    public int getFailedAssertions() {
        return failedAssertions;
    }

    public void setFailedAssertions(int failedAssertions) {
        this.failedAssertions = failedAssertions;
    }
}