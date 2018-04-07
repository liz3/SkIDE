package com.skide.skriptinsight.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.*;

public class InspectionRequest {
    private String requestID;
    private String scriptContent;

    @JsonProperty("RequestId")
    public String getRequestID() {
        return requestID;
    }

    @JsonProperty("RequestId")
    public void setRequestID(String value) {
        this.requestID = value;
    }

    @JsonProperty("ScriptContent")
    public String getScriptContent() {
        return scriptContent;
    }

    @JsonProperty("ScriptContent")
    public void setScriptContent(String value) {
        this.scriptContent = value;
    }
}