package org.graalvm.argo.dataset.execution.mw;

import org.graalvm.argo.dataset.execution.mw.entity.Function;
import org.graalvm.argo.dataset.execution.mw.memory.AbstractMemoryManager;
import org.graalvm.argo.dataset.multilang.FunctionLanguage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class FakeWorker extends AbstractWorker {

    private final Map<Integer, Queue<Runnable>> scheduledTasks;

    public FakeWorker(AbstractMemoryManager memoryManager) {
        super(memoryManager);
        scheduledTasks = new HashMap<>();
    }

    @Override
    public void ensureUploaded(String owner, String function, String benchmarkName) {
        if (!functions.contains(owner + "_" + function)) {
            owners.add(owner);
            functions.add(owner + "_" + function);
        }
    }

    @Override
    public void acceptFunctionInvocation(String owner, String function, int functionMemory, int duration, int timestamp, String benchmarkName) {
        memoryManager.startRequest(owner, function, functionMemory);
        int invocationFinishTimestamp = timestamp + duration;
        scheduledTasks.computeIfAbsent(invocationFinishTimestamp, k -> new LinkedList<>());
        scheduledTasks.get(invocationFinishTimestamp).offer(new InvocationFinishCallback(this, owner, function));

        ++totalRequests;
    }

    public void evictInvocations(int timestamp) {
        Queue<Runnable> tasks = scheduledTasks.get(timestamp);
        if (tasks != null) {
            while (!tasks.isEmpty()) {
                tasks.poll().run();
            }
        }
    }

    private static class InvocationFinishCallback implements Runnable {

        private final AbstractWorker worker;
        private final String owner;
        private final String function;

        private InvocationFinishCallback(AbstractWorker worker, String owner, String function) {
            this.worker = worker;
            this.owner = owner;
            this.function = function;
        }

        @Override
        public void run() {
            worker.memoryManager.finishRequest(owner, function);
        }

    }

    private static class InvocationCallback implements Runnable {

        private final AbstractWorker worker;
        private final String owner;
        private final String function;
        private final int duration;

        private InvocationCallback(AbstractWorker worker, String owner, String function, int duration) {
            this.worker = worker;
            this.owner = owner;
            this.function = function;
            this.duration = duration;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
            }
            worker.memoryManager.finishRequest(owner, function);
        }

    }
}
