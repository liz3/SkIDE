package com.skide.skriptinsight.client;

import com.skide.CoreManager;
import com.skide.core.code.CodeManager;
import com.skide.skriptinsight.client.impl.InsightFutureWebSocketClient;
import com.skide.skriptinsight.client.impl.InsightRequestType;
import com.skide.skriptinsight.client.utils.InsightConstants;
import com.skide.skriptinsight.model.*;
import javafx.application.Platform;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkriptInsightClient {

    private CoreManager coreManager;
    private InsightFutureWebSocketClient inspectionsWebSocketClient;
    private Inspection[] registeredInspections;

    public SkriptInsightClient(CoreManager coreManager) {

        this.coreManager = coreManager;
    }

    public void initEngine() {
        /*
           Here's how we need to do this:
           SkriptInsight is made in C#. You may think that C# isn't cross-platform, but you're wrong.
           With the power of .NET Core, we can package the runtime and run the app easily.
           In theory it's simple: Find the executable file and run it. But it's harder than you may think.
         */
        Thread th = new Thread(() -> {
            int nrInspections = getRegisteredInspections().length;
            System.out.printf("SkriptInsight: Loaded %d inspection %s%n", nrInspections, nrInspections != 1 ? "s" : "");

            inspectionsWebSocketClient = new InsightFutureWebSocketClient(
                    this,
                    InsightRequestType.INSPECT_SCRIPT_REQUEST,
                    "127.0.0.1",
                    InsightConstants.Misc.SERVER_PORT,
                    InsightConstants.Paths.INSPECT_PATH
            );
            inspectionsWebSocketClient.connect();

        });
        th.start();
    }

    public void stopEngine() {
        //This should always be run before the IDE is closed.
        //Otherwise, we will leave the process opened and cause some trouble.
        new Thread(() ->
        {
            if (inspectionsWebSocketClient != null) inspectionsWebSocketClient.close();
        }).start();
    }


    private Inspection[] getRegisteredInspections() {
        //Request all registered inspections or get from cached data.
        if (registeredInspections == null) {
            InsightFutureWebSocketClient client = new InsightFutureWebSocketClient(
                    this,
                    InsightRequestType.INSPECTIONS_REQUEST,
                    "127.0.0.1",
                    InsightConstants.Misc.SERVER_PORT,
                    InsightConstants.Paths.INSPECTIONS_PATH
            );
            client.connect();
            try {
                registeredInspections = Converter.InspectionFromJsonString(client.getReturnedValue());
            } catch (IOException e) {
                System.out.println("An error occurred whilst trying to parse response from client.");
                e.printStackTrace();
            } finally {
                client.close();
            }
        }
        return registeredInspections;
    }

    private InspectionResult inspectScript(String script) {
        if (inspectionsWebSocketClient != null) {
            try {
                InspectionRequest request = new InspectionRequest();
                request.setRequestID(UUID.randomUUID().toString());
                request.setScriptContent(script);

                inspectionsWebSocketClient.send(Converter.InspectionRequestToJsonString(request));
                return Converter.InspectionResultFromJsonString(inspectionsWebSocketClient.getReturnedValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new InspectionResult();
    }

    public void inspectScriptInAnotherThread(String script, CodeManager manager) {
        new Thread(() -> {
            InspectionResult result = coreManager.insightClient.inspectScript(script);

            coreManager.insightClient.handleSkriptInspections(manager, result);
        }).start();
    }

    public Inspection GetInspectionFromClass(String className) {
        for (Inspection inspection : getRegisteredInspections()) {
            if (inspection.getTypeName().equals(className))
                return inspection;
        }
        return null;
    }

    private void handleSkriptInspections(CodeManager manager, InspectionResult result) {
        ConcurrentHashMap<Integer, InspectionResultElement> marked = manager.getMarked();
        marked.clear();

        if (result == null || result.getInspectionResults() == null) return;
        for (InspectionResultElement resultElement : result.getInspectionResults()) {
            marked.putIfAbsent((int) resultElement.getTargetLine() - 1, resultElement);
        }
        Platform.runLater(() -> manager.highlighter.runHighlighting());
    }

}
