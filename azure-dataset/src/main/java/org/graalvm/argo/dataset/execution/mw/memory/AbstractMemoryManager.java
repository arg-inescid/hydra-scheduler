package org.graalvm.argo.dataset.execution.mw.memory;

public interface AbstractMemoryManager {
    void startRequest(String ownerName, String functionName, int memory);
    void finishRequest(String ownerName, String functionName);
    boolean canAccommodateRequest(String ownerName, String functionName, int memory);
    int getCurrentMemoryConsumption();
}
