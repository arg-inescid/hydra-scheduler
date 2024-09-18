package org.graalvm.argo.dataset.multilang;

public class FunctionRecord {
    final FunctionLanguage language;
    final int functionId;

    public FunctionRecord(FunctionLanguage language, int functionId) {
        this.language = language;
        this.functionId = functionId;
    }

    @Override
    public String toString() {
        return language.toString() + "," + functionId;
    }
}
