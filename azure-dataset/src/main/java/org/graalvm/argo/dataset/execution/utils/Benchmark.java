package org.graalvm.argo.dataset.execution.utils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Benchmark {
    public String language;
    public String benchmarkName;
    public String code;
    public String entryPoint;
    public String payload;
    public int memory;
    public int duration;
    public String gvSandbox;
    public String svmId;

    @Override
    public String toString() {
        return "Benchmark{" +
                "language='" + language + '\'' +
                ", benchmarkName='" + benchmarkName + '\'' +
                ", code='" + code + '\'' +
                ", entryPoint='" + entryPoint + '\'' +
                ", payload='" + payload + '\'' +
                ", memory=" + memory +
                ", duration=" + duration +
                ", gvSandbox='" + gvSandbox + '\'' +
                ", svmId='" + svmId + '\'' +
                '}';
    }
}
