package org.graalvm.argo.dataset.execution;

import org.graalvm.argo.dataset.execution.utils.FunctionRuntime;
import org.graalvm.argo.dataset.execution.utils.ConfigUtils;

import org.graalvm.argo.dataset.execution.utils.Benchmark;

import java.util.Map;

public class ExecutorConfiguration {

    final String executionMode;

    final FunctionRuntime functionRuntime;
    public final String invocationCollocation;
    public final String functionIsolation;
    /**
     * If true, then print timestamps instead of issuing requests.
     */
    private final boolean debug;
    private final String lambdaManagerAddress;

    private final Map<String, Benchmark> benchmarks;

    ExecutorConfiguration(String executionMode, boolean debug, String lambdaManagerAddress) {
        this.executionMode = executionMode;
        this.functionRuntime = getFunctionRuntime(executionMode);
        this.invocationCollocation = getInvocationCollocation(executionMode);
        this.functionIsolation = getFunctionIsolation(executionMode);
        this.debug = debug;
        this.lambdaManagerAddress = lambdaManagerAddress;
        this.benchmarks = ConfigUtils.getBenchmarksConfig(getBenchmarkConfigFilename(this.functionRuntime));
        System.out.println("Loaded configs of " + this.benchmarks.size() + " benchmarks");
    }

    public boolean isDebugMode() {
        return debug;
    }

    public String getLambdaManagerAddress() {
        return lambdaManagerAddress;
    }

    public Benchmark getBenchmarkConfiguration(String benchmarkName) {
        return benchmarks.get(benchmarkName);
    }

    private FunctionRuntime getFunctionRuntime(String executionMode) {
        if ("gv".equals(executionMode) || "gv-sf".equals(executionMode) || "gv-si".equals(executionMode) || "gv-fc".equals(executionMode)) {
            return FunctionRuntime.GRAALVISOR;
        } else if ("ow".equals(executionMode)) {
            return FunctionRuntime.OPENWHISK;
        } else if ("gos".equals(executionMode) || "gos-native".equals(executionMode)) {
            return FunctionRuntime.GRAALOS;
        } else if ("kn".equals(executionMode)) {
            return FunctionRuntime.KNATIVE;
        } else if ("faastion".equals(executionMode)) {
            return FunctionRuntime.FAASTION;
        } else if ("faastion-openwhisk".equals(executionMode)) {
            return FunctionRuntime.FAASTION_OPENWHISK;
        } else if ("faastion-knative".equals(executionMode)) {
            return FunctionRuntime.FAASTION_KNATIVE;
        }
        throw new IllegalArgumentException("Unsupported execution mode: " + executionMode);
    }

    private String getInvocationCollocation(String executionMode) {
        if ("gv".equals(executionMode) || "gv-sf".equals(executionMode) || "gv-fc".equals(executionMode) || "kn".equals(executionMode)
        || "faastion".equals(executionMode) || "faastion-knative".equals(executionMode)) {
            return "true";
        }
        return "false";
    }

    private String getFunctionIsolation(String executionMode) {
        if ("gv".equals(executionMode) || "gv-fc".equals(executionMode)) {
            return "false";
        }
        return "true";
    }

    private String getBenchmarkConfigFilename(FunctionRuntime functionRuntime) {
        switch (functionRuntime) {
            case GRAALVISOR:
                return "gv-benchmarks.json";
            case OPENWHISK:
                return "ow-benchmarks.json";
            case GRAALOS:
                return "gos-benchmarks.json";
            case KNATIVE:
                return "kn-benchmarks.json";
            case FAASTION:
                return "faastion-benchmarks.json";
            case FAASTION_OPENWHISK:
                return "faastion-ow-benchmarks.json";
            case FAASTION_KNATIVE:
                return "faastion-kn-benchmarks.json";
        }
        throw new IllegalArgumentException("Unsupported execution mode: " + functionRuntime);
    }
}
