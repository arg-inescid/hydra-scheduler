package org.fakeworker.server;

import org.fakeworker.utils.NetworkUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class Server {

    // We need separate read/write buffers as we can write responses while reading requests.
    private static final ByteBuffer readBuffer = ByteBuffer.allocate(512);
    private static final ByteBuffer writeBuffer = ByteBuffer.allocate(512);

    public static void main(String[] args) throws IOException {
        int port = 5454;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("localhost", port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            long before = System.currentTimeMillis();
            selector.select(5); // TODO: maybe selectNow()?
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isAcceptable()) {
                    register(selector, serverSocket);
                }
                if (key.isReadable()) {
                    while (!read(key, before)) {}
                }
                iter.remove();
            }
            FakeWorker.processCallbacks();
        }
    }

    private static boolean read(SelectionKey key, long readTimestamp) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        if (client.read(readBuffer) != -1) {
            // Prepare buffer.
            readBuffer.flip();
            // Real all requests.
            while (readBuffer.hasRemaining()) {
                // Making sure we have enough room to read the mandatory bytes.
                if (readBuffer.remaining() < 8) {
                    readBuffer.compact();
                    return false;
                }
                int requestId = readBuffer.getInt();
                int payloadLength = readBuffer.getInt();
                if (readBuffer.remaining() < payloadLength) {
                    // Roll back two integers that were already read.
                    readBuffer.position(readBuffer.position() - 4*2);
                    // Copy all unread bytes including two integers to the beginning, set position to continue writing.
                    readBuffer.compact();
                    return false;
                }
                byte[] payloadBytes = new byte[payloadLength];
                readBuffer.get(payloadBytes);
                String payload = new String(payloadBytes);
                if (NetworkUtils.CLOSE_CONNECTION_MESSAGE.equals(payload)) {
                    closeClientConnection(client);
                } else {
                    FakeWorker.processPayload(requestId, payload, client, readTimestamp);
                }
            }
            readBuffer.clear();
        } else {
            closeClientConnection(client);
        }
        return true;
    }

    private static void closeClientConnection(SocketChannel client) throws IOException {
        System.out.println("End-of-stream, closing connection...");
        client.close();
    }

    private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    public static void writeResponse(int requestId, byte[] responsePayload, SocketChannel client) {
        try {
            writeBuffer.clear();
            writeBuffer.putInt(requestId);
            writeBuffer.putInt(responsePayload.length);
            writeBuffer.put(responsePayload);
            writeBuffer.flip();
            client.write(writeBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeBuffer.clear();
        }
    }
}
