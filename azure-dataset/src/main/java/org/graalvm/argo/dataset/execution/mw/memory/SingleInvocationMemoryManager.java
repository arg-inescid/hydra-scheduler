package org.graalvm.argo.dataset.execution.mw.memory;

import org.graalvm.argo.dataset.execution.Environment;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleInvocationMemoryManager implements AbstractMemoryManager {

    private final AtomicInteger concurrentInvocations;

    public SingleInvocationMemoryManager() {
        this.concurrentInvocations = new AtomicInteger(0);
    }

    @Override
    public void startRequest(String ownerName, String functionName, int memory) {
        concurrentInvocations.incrementAndGet();
    }

    @Override
    public void finishRequest(String ownerName, String functionName) {
        concurrentInvocations.decrementAndGet();
    }

    @Override
    public boolean canAccommodateRequest(String ownerName, String functionName, int memory) {
        return getCurrentMemoryConsumption() + Environment.VM_MEMORY <= Environment.MAX_MEMORY_PER_WORKER_MB;
    }

    @Override
    public int getCurrentMemoryConsumption() {
        return concurrentInvocations.get() * Environment.VM_MEMORY;
    }
}
