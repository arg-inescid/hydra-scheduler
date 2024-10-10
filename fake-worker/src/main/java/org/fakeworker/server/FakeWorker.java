package org.fakeworker.server;

import java.nio.channels.SocketChannel;
import java.util.PriorityQueue;

public class FakeWorker {

    private static final String TIMESTAMPS_RESPONSE_TEMPLATE = "%d %d";
    private static final String UNKNOWN_RESPONSE_TEMPLATE = "Unknown request type!";

    private static final String REQUEST_DURATION_PARAMETER = "request_duration=";

    private static final PriorityQueue<InvocationCallback> callbacks = new PriorityQueue<>();


    public static void processPayload(int requestId, String payload, SocketChannel client, long readTimestamp) {
        if (payload.startsWith("i ")) {
            // i ... request_duration=X ...
            // Return in a callback later.
            int beginningIndex = payload.indexOf(REQUEST_DURATION_PARAMETER) + REQUEST_DURATION_PARAMETER.length();
            int endIndex = payload.indexOf(" ", beginningIndex);
            endIndex = endIndex == -1 ? payload.length() : endIndex;
            int duration = Integer.parseInt(payload.substring(beginningIndex, endIndex));
            callbacks.offer(new InvocationCallback(requestId, System.currentTimeMillis() + duration, client, readTimestamp));
        } else if (payload.startsWith("u ")) {
            // u ...
            // Return immediately.
            String response = String.format(TIMESTAMPS_RESPONSE_TEMPLATE, readTimestamp, System.currentTimeMillis());
            Server.writeResponse(requestId, response.getBytes(), client);
        } else {
            System.out.println("Warning: unknown request type. Payload:\n" + payload);
            Server.writeResponse(requestId, UNKNOWN_RESPONSE_TEMPLATE.getBytes(), client);
        }
    }

    public static int processCallbacks() {
        int processed = 0;
        long currentTimestamp = System.currentTimeMillis();
        InvocationCallback cb;
        while ((cb = callbacks.peek()) != null && cb.callbackTimestamp < currentTimestamp) {
            callbacks.remove();
            cb.run();
            ++processed;
        }
        return processed;
    }

    static class InvocationCallback implements Runnable, Comparable<InvocationCallback> {

        private final int requestId;
        private final long callbackTimestamp;
        private final SocketChannel client;
        private final long readTimestamp;

        private InvocationCallback(int requestId, long callbackTimestamp, SocketChannel client, long readTimestamp) {
            this.requestId = requestId;
            this.callbackTimestamp = callbackTimestamp;
            this.client = client;
            this.readTimestamp = readTimestamp;
        }

        public int getEstimatedTime() {
            return (int) (this.callbackTimestamp - System.currentTimeMillis());
        }

        @Override
        public void run() {
            String response = String.format(TIMESTAMPS_RESPONSE_TEMPLATE, readTimestamp, System.currentTimeMillis());
            Server.writeResponse(requestId, response.getBytes(), client);
        }

        @Override
        public int compareTo(InvocationCallback other) {
            return Long.compare(callbackTimestamp, other.callbackTimestamp);
        }

        @Override
        public String toString() {
            return "InvocationCallback [callbackTimestamp=" + callbackTimestamp + "]";
        }
    }
}
