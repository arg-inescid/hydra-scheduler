package org.graalvm.argo.dataset.execution.mw;

import org.graalvm.argo.dataset.execution.mw.memory.AbstractMemoryManager;
import org.graalvm.argo.dataset.multilang.FunctionLanguage;

import java.util.function.Consumer;

public class PhysicalFakeWorker extends AbstractWorker {

    private final MultiWorkerInvocationTraceExecutor executor;
    private final String address;

    protected PhysicalFakeWorker(AbstractMemoryManager memoryManager, MultiWorkerInvocationTraceExecutor executor, String address) {
        super(memoryManager);
        this.executor = executor;
        this.address = address;
    }

    @Override
    public void ensureUploaded(String owner, String function, String benchmarkName) {
        if (!functions.contains(owner + "_" + function)) {
            executor.uploadFunction(address, owner, function, benchmarkName);
            owners.add(owner);
            functions.add(owner + "_" + function);
        }
    }

    @Override
    public void acceptFunctionInvocation(String owner, String function, int functionMemory, int duration, int timestamp, String benchmarkName) {
        memoryManager.startRequest(owner, function, functionMemory);
        executor.invokeFunction(address, owner, function, timestamp, duration, benchmarkName, new InvocationCallback(this, owner, function, duration));
        ++totalRequests;
    }

    private static class InvocationCallback implements Consumer<String> {

        private final AbstractWorker worker;
        private final String owner;
        private final String function;
        private final long duration;
        private final long startTimestamp;

        private InvocationCallback(AbstractWorker worker, String owner, String function, int duration) {
            this.worker = worker;
            this.owner = owner;
            this.function = function;
            this.duration = duration;
            this.startTimestamp = System.currentTimeMillis();
        }

        @Override
        public void accept(String response) {
            long actualEndTimestamp = System.currentTimeMillis();
            String[] ts = response.split(" ");
            long msgReceivedTs = Long.parseLong(ts[0]);
            long msgRespondedTs = Long.parseLong(ts[1]);
            worker.memoryManager.finishRequest(owner, function);
            long actualDuration = actualEndTimestamp - startTimestamp;
            MultiWorkerInvocationTraceExecutor.differences.add(actualDuration - duration);
            MultiWorkerInvocationTraceExecutor.ratios.add((double) actualDuration / (duration == 0 ? 0.1 : (double) duration));
            MultiWorkerInvocationTraceExecutor.networkSend.add(msgReceivedTs - startTimestamp);
            MultiWorkerInvocationTraceExecutor.workerProcess.add(msgRespondedTs - msgReceivedTs);
            MultiWorkerInvocationTraceExecutor.traceDurations.add(duration);
            MultiWorkerInvocationTraceExecutor.networkRecv.add(actualEndTimestamp - msgRespondedTs);
        }
    }
}
