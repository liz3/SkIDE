package net.nickac.skriptinsight.model;

import java.util.Map;
import com.fasterxml.jackson.annotation.*;

public class InspectionResult {
    private InspectionResultElement[] inspectionResults;
    private String allFixedTokens;

    @JsonProperty("InspectionResults")
    public InspectionResultElement[] getInspectionResults() { return inspectionResults; }
    @JsonProperty("InspectionResults")
    public void setInspectionResults(InspectionResultElement[] value) { this.inspectionResults = value; }

    @JsonProperty("AllFixedTokens")
    public String getAllFixedTokens() { return allFixedTokens; }
    @JsonProperty("AllFixedTokens")
    public void setAllFixedTokens(String value) { this.allFixedTokens = value; }
}