package org.graalvm.argo.dataset.multilang;

public class FunctionRecord {
    final FunctionLanguage language;
    final String benchmarkName;

    public FunctionRecord(FunctionLanguage language, String benchmarkName) {
        this.language = language;
        this.benchmarkName = benchmarkName;
    }

    @Override
    public String toString() {
        return language.toString() + "," + benchmarkName;
    }
}
