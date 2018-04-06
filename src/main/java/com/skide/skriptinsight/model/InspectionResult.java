package com.skide.skriptinsight.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InspectionResult{
    private String requestID;

    private InspectionResultElement[] inspectionResults;

    private long timeTakenToParse;

    private long timeTakenToFix;

    @JsonProperty("RequestId")
    public String getRequestID(){
        return requestID;
    }

    @JsonProperty("RequestId")
    public void setRequestID(String value){
        this.requestID = value;
    }

    @JsonProperty("InspectionResults")
    public InspectionResultElement[] getInspectionResults(){
        return inspectionResults;
    }

    @JsonProperty("InspectionResults")
    public void setInspectionResults(InspectionResultElement[] value){
        this.inspectionResults = value;
    }

    @JsonProperty("TimeTakenToParse")
    public long getTimeTakenToParse(){
        return timeTakenToParse;
    }

    @JsonProperty("TimeTakenToParse")
    public void setTimeTakenToParse(long value){
        this.timeTakenToParse = value;
    }

    @JsonProperty("TimeTakenToFix")
    public long getTimeTakenToFix(){
        return timeTakenToFix;
    }

    @JsonProperty("TimeTakenToFix")
    public void setTimeTakenToFix(long value){
        this.timeTakenToFix = value;
    }
}