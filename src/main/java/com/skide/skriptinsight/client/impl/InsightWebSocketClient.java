package com.skide.skriptinsight.client.impl;

import com.skide.skriptinsight.client.SkriptInsightClient;
import com.skide.skriptinsight.model.Converter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;

import static java.lang.System.out;

public class InsightWebSocketClient extends WebSocketClient {
    private final SkriptInsightClient client;
    private final InsightRequestType requestType;

    public InsightWebSocketClient(SkriptInsightClient client, InsightRequestType type, String host, int port, String location) {
        super(URI.create(String.format("ws://%s:%d/%s", host, port, location)));
        this.client = client;
        this.requestType = type;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        if (requestType == InsightRequestType.INSPECTIONS_REQUEST) {
            send("");
        }
    }

    @Override
    public void onMessage(String message) {
        switch (requestType) {
            case INSPECTIONS_REQUEST:
                try {
                    Converter.InspectionFromJsonString(message);
                } catch (IOException e) {
                    out.println("An error occurred whilst retrieving all inspections registered to SkriptInsight");
                    e.printStackTrace();
                }
                break;
            case INSPECT_SCRIPT_REQUEST:
                break;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }
}
