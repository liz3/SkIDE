package com.skide.skriptinsight.model;

import com.fasterxml.jackson.annotation.*;

public class Inspection{
    private String typeName;

    private String description;

    @JsonProperty("TypeName")
    public String getTypeName(){
        return typeName;
    }

    @JsonProperty("TypeName")
    public void setTypeName(String value){
        this.typeName = value;
    }

    @JsonProperty("Description")
    public String getDescription(){
        return description;
    }

    @JsonProperty("Description")
    public void setDescription(String value){
        this.description = value;
    }
}