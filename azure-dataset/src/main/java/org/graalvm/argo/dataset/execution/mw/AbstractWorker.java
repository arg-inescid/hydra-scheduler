package org.graalvm.argo.dataset.execution.mw;

import org.graalvm.argo.dataset.execution.mw.memory.AbstractMemoryManager;
import org.graalvm.argo.dataset.multilang.FunctionLanguage;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractWorker {

    protected final Set<String> owners;
    protected final Set<String> functions;

    protected int totalRequests = 0;
    protected int maxExperiencedMemoryUtilization = 0;

    protected final AbstractMemoryManager memoryManager;

    protected AbstractWorker(AbstractMemoryManager memoryManager) {
        this.owners = new HashSet<>();
        this.functions = new HashSet<>();
        this.memoryManager = memoryManager;
    }

    public abstract void ensureUploaded(String owner, String function, FunctionLanguage language, int functionId);

    public abstract void acceptFunctionInvocation(String owner, String function, int allocatedMemoryMb, int duration, int timestamp, FunctionLanguage language, int functionId) throws IOException;

    public boolean hasFunctionRegistered(String owner, String function) {
        return functions.contains(owner + "_" + function);
    }

    public boolean hasOwnerRegistered(String owner) {
        return owners.contains(owner);
    }

    public int getCurrentMemoryUtilization() {
        int currentMemoryUtilization = memoryManager.getCurrentMemoryConsumption();
        if (currentMemoryUtilization > maxExperiencedMemoryUtilization) {
            maxExperiencedMemoryUtilization = currentMemoryUtilization;
        }
        return currentMemoryUtilization;
    }

    public boolean canAccommodateRequest(String owner, String function, int memory) {
        return memoryManager.canAccommodateRequest(owner, function, memory);
    }

    public void printStatistics() {
        System.out.println("###################################");
        System.out.println("Registered functions: " + functions.size());
        System.out.println("Registered owners:    " + owners.size());
        System.out.println("Total requests:       " + totalRequests);
        System.out.println("Max memory:           " + maxExperiencedMemoryUtilization);
        System.out.println("###################################");
    }
}
