package org.fakeworker.utils;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SocketConnection implements Closeable {

    private final SocketChannel channel;
    private ByteBuffer buffer;
    private final Selector selector;
    private final AtomicInteger requestId;
    private final Map<Integer, Consumer<String>> callbackBacklog;
    private boolean isClosed;

    /**
     * This set contains IDs of the requests that received their responses as we read them
     * with readAvailableMessagesImpl. Used to wait until we get a response for a particular
     * request in sendMessage.
     * <p>
     * This set contains this state only between two invocations of readAvailableMessagesImpl.
     * Every invocation of this method empties and potentially updates the contents of this set.
     * Can only be used under assumption of a single-threaded execution.
     */
    private final Set<Integer> returnedIds;

    public SocketConnection(String host, int port) {
        buffer = ByteBuffer.allocate(512);
        requestId = new AtomicInteger(0);
        callbackBacklog = new HashMap<>();
        returnedIds = new HashSet<>();
        isClosed = false;
        try {
            selector = Selector.open();
            channel = SocketChannel.open(new InetSocketAddress(host, port));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String msg, Consumer<String> callback) throws IOException {
        // Make sure we receive a response from this request with the current request ID.
        int currentRequestId = sendMessageImpl(msg, callback);
        while (!returnedIds.contains(currentRequestId) && !isClosed) {
            readAvailableMessagesImpl(true, NetworkUtils.SYNC_REQUEST_TIMEOUT);
        }
    }

    public void sendMessageAsync(String msg, Consumer<String> callback) throws IOException {
        // Just send a message, response will be received when reading responses explicitly.
        sendMessageImpl(msg, callback);
    }

    private int sendMessageImpl(String msg, Consumer<String> callback) throws IOException {
        int currentRequestId = requestId.getAndIncrement();
        callbackBacklog.put(currentRequestId, callback);
        // Write msg to buffer.
        buffer.clear();
        byte[] msgBytes = msg.getBytes();
        buffer.putInt(currentRequestId);
        buffer.putInt(msgBytes.length);
        buffer.put(msgBytes);
        buffer.flip();
        // Write buffer contents to client.
        channel.write(buffer);
        buffer.clear();
        return currentRequestId;
    }

    public void readMessages() throws IOException {
        readAvailableMessagesImpl(false, 0);
    }

    private void readAvailableMessagesImpl(boolean blocking, long timeout) throws IOException {
        if (blocking) {
            selector.select(timeout);
        } else {
            selector.selectNow();
        }
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iter = selectedKeys.iterator();
        returnedIds.clear();
        while (iter.hasNext()) {
            SelectionKey key = iter.next();
            if (key.isReadable()) {
                read();
            }
            iter.remove();
        }
    }

    private void read() throws IOException {
        if (channel.read(buffer) != -1) {
            // Prepare buffer.
            buffer.flip();
            // Read all responses.
            while (buffer.hasRemaining()) {
                int requestId = buffer.getInt();
                int payloadLength = buffer.getInt();
                if (buffer.remaining() < payloadLength) {
                    throw new RuntimeException("Didn't read the entire message (implement safe read).");
                }
                byte[] payloadBytes = new byte[payloadLength];
                buffer.get(payloadBytes);
                String payload = new String(payloadBytes);
                returnedIds.add(requestId);
                callbackBacklog.get(requestId).accept(payload);
            }
            buffer.clear();
        } else {
            // TODO: should this be close()? Risk of closing twice.
            close();
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
        System.out.println("Connection closed!");
        buffer = null;
        isClosed = true;
    }
}
