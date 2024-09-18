package org.fakeworker.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkUtils {

    public static final String CLOSE_CONNECTION_MESSAGE = "CLOSE_CONNECTION";
    public static final long SYNC_REQUEST_TIMEOUT = 10000;

    private static Map<String, SocketConnection> CONNECTIONS = new HashMap<>();

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

    private static SocketConnection getConnection(String address) {
        String[] splitAddress = address.split(":");
        String host = splitAddress[0];
        int port = Integer.parseInt(splitAddress[1]);
        CONNECTIONS.computeIfAbsent(address, k -> new SocketConnection(host, port));
        return CONNECTIONS.get(address);
    }

}
