package org.graalvm.argo.dataset.utils.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HttpClientNetworkUtils {

    private static final Map<String, HttpClient> CLIENTS = new HashMap<>();

    public static void sendPost(String address, String path, String contentType, byte[] content, String[] headers, boolean async, Consumer<String> asyncConsumer) {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create("http://" + address + path))
                .timeout(Duration.ofSeconds(300))
                .header("Content-Type", contentType)
                .header("accept", "application/json; charset=UTF-8");

        if (headers != null && headers.length != 0 && headers.length % 2 == 0) {
            // Read custom headers.
            requestBuilder.headers(headers);
        }
        HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(content)).build();

        HttpClient client = getClient(address);
        if (async) {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(asyncConsumer);
        } else {
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendPost(String address, String path, String contentType, byte[] content, boolean async) {
        sendPost(address, path, contentType, content, null, async, System.out::println);
    }

    private static HttpClient getClient(String address) {
        CLIENTS.computeIfAbsent(address, k -> HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(300)).build());
        return CLIENTS.get(address);
    }

}
