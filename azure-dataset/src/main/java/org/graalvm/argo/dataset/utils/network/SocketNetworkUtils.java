package org.graalvm.argo.dataset.utils.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SocketNetworkUtils {

    public static final long SYNC_REQUEST_TIMEOUT = 10000;

    private static Map<String, SocketConnection> CONNECTIONS = new HashMap<>();

    /**
     * Send a raw message (request ID will be appended automatically).
     */
    public static void send(String address, String msg, boolean async, Consumer<String> callback) {
        SocketConnection conn = getConnection(address);
        try {
            if (async) {
                conn.sendMessageAsync(msg, callback);
            } else {
                conn.sendMessage(msg, callback);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void readAllAvailable() {
//        CONNECTIONS.keySet().forEach(SocketNetworkUtils::readAvailable);
        try {
            for (SocketConnection c : CONNECTIONS.values()) {
                c.readMessages();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void readAvailable(String address) {
        try {
            getConnection(address).readMessages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeConnection(String address) {
        try {
            getConnection(address).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void waitForResponses(long milliseconds) {
        long begin = System.currentTimeMillis();
        long current = System.currentTimeMillis();
        while (current != begin + milliseconds) {
            SocketNetworkUtils.readAllAvailable();
            current = System.currentTimeMillis();
        }
    }

    private static SocketConnection getConnection(String address) {
        String[] splitAddress = address.split(":");
        String host = splitAddress[0];
        int port = Integer.parseInt(splitAddress[1]);
        CONNECTIONS.computeIfAbsent(address, k -> new SocketConnection(host, port));
        return CONNECTIONS.get(address);
    }
}
