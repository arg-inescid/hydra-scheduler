package org.graalvm.argo.dataset.execution.mw.memory;

import org.graalvm.argo.dataset.execution.Environment;
import org.graalvm.argo.dataset.execution.mw.entity.Function;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OwnerCollocationMemoryManager implements AbstractMemoryManager {

    private final Map<String, Map<String, Function>> ownersFunctions;

    public OwnerCollocationMemoryManager() {
        ownersFunctions = new ConcurrentHashMap<>();
    }

    @Override
    public void startRequest(String ownerName, String functionName, int memory) {
        Map<String, Function> functions = ownersFunctions.computeIfAbsent(ownerName, k -> new ConcurrentHashMap<>());
        functions.computeIfAbsent(functionName, k -> new Function(functionName, memory));
        functions.get(functionName).addInvocation();
    }

    @Override
    public void finishRequest(String ownerName, String functionName) {
        ownersFunctions.get(ownerName).get(functionName).removeInvocation();
    }

    @Override
    public boolean canAccommodateRequest(String ownerName, String functionName, int memory) {
        Map<String, Function> functions = ownersFunctions.get(ownerName);
        if (functions == null) {
            // This owner has never had his functions invoked -> no active VMs for collocation.
            return getCurrentMemoryConsumption() + Environment.VM_MEMORY <= Environment.MAX_MEMORY_PER_WORKER_MB;
        }
        int ownerInvocationsMemory = 0;
        for (Function f : functions.values()) {
            ownerInvocationsMemory += f.memory * f.getInvocations();
        }
        int lastVMOccupancy = ownerInvocationsMemory % Environment.VM_MEMORY;
        if (ownerInvocationsMemory == 0 || lastVMOccupancy == 0 || lastVMOccupancy + memory > Environment.VM_MEMORY) {
            // This owner has no active invocations and VMs OR all workers are at max occupancy OR we overshoot
            // max occupancy by adding this request.
            return getCurrentMemoryConsumption() + Environment.VM_MEMORY <= Environment.MAX_MEMORY_PER_WORKER_MB;
        }
        // We can collocate -> no need to allocate new VM.
        return true;
    }

    @Override
    public int getCurrentMemoryConsumption() {
        int totalVMs = 0;
        for (Map<String, Function> functions : ownersFunctions.values()) {
            int ownerMemory = 0;
            for (Function f : functions.values()) {
                ownerMemory += f.memory * f.getInvocations();
            }
            if (ownerMemory != 0) {
                // Adapted from https://stackoverflow.com/a/21830188
                totalVMs += (ownerMemory + Environment.VM_MEMORY - 1) / Environment.VM_MEMORY;
            }
        }
        return totalVMs * Environment.VM_MEMORY;
    }
}
