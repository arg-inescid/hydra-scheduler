package org.graalvm.argo.dataset.execution.mw.memory;

import org.graalvm.argo.dataset.execution.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleInvocationMemoryManager implements AbstractMemoryManager {

    private final AtomicInteger memoryConsumption;
    private final Map<String, Integer> functionMemory;

    public SingleInvocationMemoryManager() {
        this.memoryConsumption = new AtomicInteger(0);
        this.functionMemory = new HashMap<>();
    }

    @Override
    public void startRequest(String ownerName, String functionName, int memory) {
        functionMemory.putIfAbsent(ownerName + "_" + functionName, memory);
        memoryConsumption.set(memoryConsumption.get() + memory);
    }

    @Override
    public void finishRequest(String ownerName, String functionName) {
        memoryConsumption.set(memoryConsumption.get() - functionMemory.get(ownerName + "_" + functionName));
    }

    @Override
    public boolean canAccommodateRequest(String ownerName, String functionName, int memory) {
        return getCurrentMemoryConsumption() + memory <= Environment.MAX_MEMORY_PER_WORKER_MB;
    }

    @Override
    public int getCurrentMemoryConsumption() {
        return memoryConsumption.get();
    }
}
