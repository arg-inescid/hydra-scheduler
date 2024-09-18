package org.graalvm.argo.dataset.execution.mw.entity;

import java.util.concurrent.atomic.AtomicInteger;

public class Function {

    public final String name;
    public final int memory;
    private final AtomicInteger concurrentInvocations;

    public Function(String name, int memory) {
        this.name = name;
        this.memory = memory;
        concurrentInvocations = new AtomicInteger(0);
    }

    public void addInvocation() {
        concurrentInvocations.incrementAndGet();
    }

    public void removeInvocation() {
        concurrentInvocations.decrementAndGet();
    }

    public int getInvocations() {
        return concurrentInvocations.get();
    }
}
