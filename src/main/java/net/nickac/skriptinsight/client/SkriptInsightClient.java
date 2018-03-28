package net.nickac.skriptinsight.client;

import net.nickac.skriptinsight.model.Inspection;
import net.nickac.skriptinsight.model.InspectionResult;

import java.util.concurrent.*;

public class SkriptInsightClient {

    protected void initEngine() {
        /*
           Here's how we need to do this:
           SkriptInsight is made in C#. You may think that C# isn't cross-platform, but you're wrong.
           With the power of .NET Core, we can package the runtime and run the app easily.
           In theory it's simple: Find the executable file and run it. But it's harder than you may think.
         */
    }

    protected void stopEngine() {
        //This should always be run before the IDE is closed.
        //Otherwise, we will leave the process opened and cause some trouble.
    }


    protected Inspection[] getRegisteredInspections() {
        //Request all registered inspections or get from cached data.
        return null;
    }

    protected InspectionResult inspectScript(String script) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<InspectionResult> task = new Callable<InspectionResult>() {
            @Override
            public InspectionResult call() throws Exception {
                //Request inspection by websocket
                return null;
            }
        };
        Future<InspectionResult> future = executorService.submit(task);

        return future.get();
    }


}
