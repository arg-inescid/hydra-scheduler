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
            /* Timestamp used to understand whether the executor is too slow or too fast compared to the trace. */
            long beginningTimestamp = System.currentTimeMillis();
            /* Used to avoid waiting on the same period multiple times. */
            int checkedTimestamp = 0;
            while ((line = br.readLine()) != null) {
                splitRow = line.split(InvocationTraceGenerator.DELIMITER);
                String owner = splitRow[0];
                int duration = Integer.parseInt(splitRow[3]);
                int timestamp = Integer.parseInt(splitRow[4]);
                FunctionLanguage language = FunctionLanguage.fromString(splitRow[5]);
                int functionId = Integer.parseInt(splitRow[6]);
                String function = config.getFunctionConfiguration(language, functionId).functionName;

                /* Periodically check if we need to slow down the executor. */
                if (timestamp != checkedTimestamp && timestamp % Environment.WAIT_PERIOD_MS == 0) {
                    waitForInvocation(timestamp, System.currentTimeMillis() - beginningTimestamp);
                    checkedTimestamp = timestamp;
                }

                invokeFunction(config.getLambdaManagerAddress(), owner, function, timestamp, duration, language, functionId, System.out::println);
                SocketNetworkUtils.readAllAvailable();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        SocketNetworkUtils.waitForResponses(15000);
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
        ExecutorConfiguration.FunctionConfiguration functionConfig = config.getFunctionConfiguration(language, functionId);
        // Hydra Python/JavaScript benchmarks have Java wrappers.
        FunctionLanguage actualLanguage = Environment.HYDRA_RUNTIME.equals(config.functionRuntime) ? FunctionLanguage.JAVA : language;
        String message = "u username=" + owner + " function_name=" + function +
                " function_language=" + actualLanguage + " function_entry_point=" + functionConfig.entryPoint +
                " function_memory=" + functionConfig.memory + " function_runtime=" + config.functionRuntime +
                " function_isolation=" + config.functionIsolation + " invocation_collocation=" + config.invocationCollocation;
        if (functionConfig.gvSandbox != null) {
            message = message + " gv_sandbox=" + functionConfig.gvSandbox;
            // For Python/JS functions that need sandbox snapshotting.
            if ("context-snapshot".equals(functionConfig.gvSandbox) && functionConfig.svmId != null) {
                message = message + " svm_id=" + functionConfig.svmId;
            }
        }
        // Append path to the function as payload in ''.
        message = message + " '" + functionConfig.code + "'";
        if (!config.isDebugMode()) {
            SocketNetworkUtils.send(address, message, false, (s) -> {});
        }
    }

    public void invokeFunction(String address, String owner, String function, int timestamp, int duration, FunctionLanguage language, int functionId, Consumer<String> asyncConsumer) {
        if (config.isDebugMode()) {
            System.out.println("Sending request with timestamp: " + timestamp);
        } else {
            ExecutorConfiguration.FunctionConfiguration functionConfig = config.getFunctionConfiguration(language, functionId);
            String message = "i username=" + owner + " function_name=" + function +
                    " request_duration=" + duration +
                    " '" + functionConfig.payload + "'"; // Append invocation parameters as payload in ''.
            SocketNetworkUtils.send(address, message, true, asyncConsumer);
        }
    }
}
