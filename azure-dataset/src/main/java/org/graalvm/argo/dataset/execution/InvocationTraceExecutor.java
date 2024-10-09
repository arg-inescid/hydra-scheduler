package org.graalvm.argo.dataset.execution;

import org.graalvm.argo.dataset.generator.InvocationTraceGenerator;
import org.graalvm.argo.dataset.multilang.FunctionLanguage;
import org.graalvm.argo.dataset.utils.network.SocketNetworkUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class InvocationTraceExecutor {

    protected final ExecutorConfiguration config;


    public InvocationTraceExecutor(ExecutorConfiguration config) {
        this.config = config;
    }

    // HashOwner,HashFunction,AverageAllocatedMb,AverageDuration,Timestamp
    public void execute(String invocationsFilePath) {
        uploadFunctions(invocationsFilePath);
        try (BufferedReader br = new BufferedReader(new FileReader(invocationsFilePath))) {
            String line;
            String[] splitRow;
            br.readLine(); // To skip the header
            int currentTimestamp = 0;
            while ((line = br.readLine()) != null) {
                splitRow = line.split(InvocationTraceGenerator.DELIMITER);
                String owner = splitRow[0];
                int duration = Integer.parseInt(splitRow[3]);
                int timestamp = Integer.parseInt(splitRow[4]);
                FunctionLanguage language = FunctionLanguage.fromString(splitRow[5]);
                int functionId = Integer.parseInt(splitRow[6]);
                String function = config.getFunctionConfiguration(language, functionId).functionName;

                waitForInvocation(currentTimestamp, timestamp);
                currentTimestamp = timestamp;

                invokeFunction(config.getLambdaManagerAddress(), owner, function, timestamp, duration, language, functionId, (s) -> {});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadFunctions(String invocationsFilePath) {
        Set<String> uploadedFunctions = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(invocationsFilePath))) {
            String line;
            String[] splitRow;
            br.readLine(); // To skip the header
            while ((line = br.readLine()) != null) {
                splitRow = line.split(InvocationTraceGenerator.DELIMITER);
                String owner = splitRow[0];
                FunctionLanguage language = FunctionLanguage.fromString(splitRow[5]);
                int functionId = Integer.parseInt(splitRow[6]);
                String function = config.getFunctionConfiguration(language, functionId).functionName;
                ensureUploaded(uploadedFunctions, owner, function, language, functionId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void ensureUploaded(Set<String> uploadedFunctions, String owner, String function, FunctionLanguage language, int functionId) {
        if (!uploadedFunctions.contains(owner + "_" + function)) {
            uploadFunction(config.getLambdaManagerAddress(), owner, function, language, functionId);
            uploadedFunctions.add(owner + "_" + function);
        }
    }

    protected void waitForInvocation(int currentTimestamp, int invocationTimestamp) {
        int timeToSleep = invocationTimestamp - currentTimestamp;
        if (timeToSleep != 0) {
            try {
                Thread.sleep(timeToSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void waitForInvocation(int traceTimestamp, long realTimestamp) {
        long timeDifference = traceTimestamp - realTimestamp;
        if (timeDifference > 0) {
            try {
                System.out.println("Sleeping (ms) " + timeDifference);
                Thread.sleep(timeDifference);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Warning: executor lags behind the trace by (ms) " + timeDifference);
        }
    }

    public void uploadFunction(String address, String owner, String function, FunctionLanguage language, int functionId) {
        uploadFunction(address, owner, function, language, functionId, true);
    }

    public void uploadFunction(String address, String owner, String function, FunctionLanguage language, int functionId, boolean includeBody) {
        ExecutorConfiguration.FunctionConfiguration functionConfig = config.getFunctionConfiguration(language, functionId);
        // Graalvisor Python/JavaScript benchmarks have Java wrappers.
        FunctionLanguage actualLanguage = Environment.GRAALVISOR_RUNTIME.equals(config.functionRuntime) ? FunctionLanguage.JAVA : language;
        String queryParameters = "username=" + owner + "&function_name=" + function +
                "&function_language=" + actualLanguage + "&function_entry_point=" + functionConfig.entryPoint +
                "&function_memory=" + functionConfig.memory + "&function_runtime=" + config.functionRuntime +
                "&function_isolation=" + config.functionIsolation + "&invocation_collocation=" + config.invocationCollocation;
        if (functionConfig.gvSandbox != null) {
            queryParameters = queryParameters + "&gv_sandbox=" + functionConfig.gvSandbox;
            // For Python/JS functions that need sandbox snapshotting.
            if ("context-snapshot".equals(functionConfig.gvSandbox) && functionConfig.svmId != null) {
                queryParameters = queryParameters + "&svm_id=" + functionConfig.svmId;
            }
        }
        if (!config.isDebugMode()) {
            String payload = includeBody ? functionConfig.code : "";
            SocketNetworkUtils.send(address, "u", false, (s) -> {});
        }
    }

    public void invokeFunction(String address, String owner, String function, int timestamp, int duration, FunctionLanguage language, int functionId, Consumer<String> asyncConsumer) {
        invokeFunction(address, owner, function, timestamp, duration, language, functionId, asyncConsumer, true);
    }

    public void invokeFunction(String address, String owner, String function, int timestamp, int duration, FunctionLanguage language, int functionId, Consumer<String> asyncConsumer, boolean includeBody) {
        if (config.isDebugMode()) {
            System.out.println("Sending request with timestamp: " + timestamp);
        } else {
            ExecutorConfiguration.FunctionConfiguration functionConfig = config.getFunctionConfiguration(language, functionId);
            String payload = includeBody ? functionConfig.payload : "";
            SocketNetworkUtils.send(address, "i " + duration, true, asyncConsumer);
        }
    }
}
