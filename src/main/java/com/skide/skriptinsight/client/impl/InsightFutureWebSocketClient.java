package com.skide.skriptinsight.client.impl;

import com.skide.skriptinsight.client.SkriptInsightClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InsightFutureWebSocketClient extends InsightWebSocketClient{
    private CompletableFuture<String> future;

    public InsightFutureWebSocketClient(SkriptInsightClient client, InsightRequestType type, String host, int port, String location){
        super(client, type, host, port, location);
        
        this.future = new CompletableFuture<>();
    }

    @Override
    public void onMessage(String message){
        future.complete(message);
        
        super.onMessage(message);
    }

    public String getReturnedValue(){
        try{
            return future.get();
        }catch (InterruptedException | ExecutionException e){
            System.out.println("Unable to get the first returned value from websocket!");

            e.printStackTrace();
        }finally{
            //Create a new future.
            future = new CompletableFuture<>();
        }

        return "";
    }




}
