package com.skide.skriptinsight.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InspectionResult {
    private String requestID;
    private InspectionResultElement[] inspectionResults;

    @JsonProperty("RequestId")
    public String getRequestID() { return requestID; }
    @JsonProperty("RequestId")
    public void setRequestID(String value) { this.requestID = value; }

    @JsonProperty("InspectionResults")
    public InspectionResultElement[] getInspectionResults() { return inspectionResults; }
    @JsonProperty("InspectionResults")
    public void setInspectionResults(InspectionResultElement[] value) { this.inspectionResults = value; }
}