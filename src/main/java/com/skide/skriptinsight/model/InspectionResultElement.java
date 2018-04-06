package com.skide.skriptinsight.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InspectionResultElement{
    private long targetLine;
    
    private String inspectionClass;
    
    private String fixedInspection;

    @JsonProperty("TargetLine")
    public long getTargetLine(){ 
        return targetLine; 
    }
    
    @JsonProperty("TargetLine")
    public void setTargetLine(long value){
        this.targetLine = value;
    }

    @JsonProperty("InspectionClass")
    public String getInspectionClass(){
        return inspectionClass;
    }

    @JsonProperty("InspectionClass")
    public void setInspectionClass(String value){
        this.inspectionClass = value;
    }

    @JsonProperty("FixedInspection")
    public String getFixedInspection(){
        return fixedInspection;
    }

    @JsonProperty("FixedInspection")
    public void setFixedInspection(String value){
        this.fixedInspection = value;
    }
}