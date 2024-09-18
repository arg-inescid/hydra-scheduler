package org.graalvm.argo.dataset.execution.mw.memory;

import org.graalvm.argo.dataset.execution.Environment;
import org.graalvm.argo.dataset.execution.mw.entity.Function;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingleFunctionMemoryManager implements AbstractMemoryManager {

    private final Map<String, Function> functions;

    public SingleFunctionMemoryManager() {
        functions = new ConcurrentHashMap<>();
    }

    @Override
    public void startRequest(String ownerName, String functionName, int memory) {
        functions.computeIfAbsent(functionName, k -> new Function(functionName, memory));
        functions.get(functionName).addInvocation();
    }

    @Override
    public void finishRequest(String ownerName, String functionName) {
        functions.get(functionName).removeInvocation();
    }

    @Override
    public boolean canAccommodateRequest(String ownerName, String functionName, int memory) {
        Function f = functions.get(functionName);
        if (f == null || f.getInvocations() == 0) {
            // This function has never been invoked OR currently it doesn't have active invocations
            // -> it doesn't have active VMs for collocation.
            return getCurrentMemoryConsumption() + Environment.VM_MEMORY <= Environment.MAX_MEMORY_PER_WORKER_MB;
        } else {
            // This function has active invocations -> try to collocate.
            int lastVMOccupancy = (f.memory * f.getInvocations()) % Environment.VM_MEMORY;
            if (lastVMOccupancy == 0 || lastVMOccupancy + memory > Environment.VM_MEMORY) {
                // All workers are at max occupancy OR we overshoot max occupancy by adding this request.
                return getCurrentMemoryConsumption() + Environment.VM_MEMORY <= Environment.MAX_MEMORY_PER_WORKER_MB;
            }
            // We can collocate -> no need to allocate new VM.
            return true;
        }
    }

    @Override
    public int getCurrentMemoryConsumption() {
        int totalVMs = 0;
        for (Function f : functions.values()) {
            int cinv = f.getInvocations();
            if (cinv > 0) {
                // Adapted from https://stackoverflow.com/a/21830188
                totalVMs += ((f.memory * cinv) + Environment.VM_MEMORY - 1) / Environment.VM_MEMORY;
            }
        }
        return totalVMs * Environment.VM_MEMORY;
    }
}
