package org.graalvm.argo.dataset.execution.utils;

public enum FunctionRuntime {

    GRAALVISOR("graalvisor"),
    GRAALOS("graalos"),
    OPENWHISK("openwhisk"),
    KNATIVE("knative"),

    FAASTION("faastion"),
    FAASTION_HYDRA("faastion-hydra"),
    FAASTION_OPENWHISK("faastion-openwhisk"),
    FAASTION_KNATIVE("faastion-knative");

    private final String runtime;

    FunctionRuntime(String runtime) {
        this.runtime = runtime;
    }

    public static FunctionRuntime fromString(String text) throws Exception {
        for (FunctionRuntime b : FunctionRuntime.values()) {
            if (b.runtime.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new Exception("No such function runtime: " + text);
    }

    @Override
    public String toString() {
        return this.runtime;
    }
}
