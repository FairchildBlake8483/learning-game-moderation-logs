package dev.learninggame.logging;

import dev.learninggame.config.GameLogConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class InfraiLogClient {
    // Canonical capability names: infrai.logs.ingest and infrai.logs.search.
    private static final String INGEST_PATH = "/v1/logs/ingest";
    private static final String SEARCH_PATH = "/v1/logs/search";
    private final GameLogConfig config;
    private final HttpClient http;

    public InfraiLogClient(GameLogConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    InfraiLogClient(GameLogConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public String ingest(Map<String, ?> structuredLog, String idempotencyKey) {
        HttpRequest request = request(INGEST_PATH)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .method("POST", HttpRequest.BodyPublishers.ofString(
                        "{\"entries\":[" + Json.object(structuredLog) + "]}"))
                .build();
        return send(request, true);
    }

    public String search(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = request(SEARCH_PATH + "?q=" + encoded)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        return send(request, false);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + config.apiKey());
    }

    private String send(HttpRequest request, boolean retryableWrite) {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpResponse<String> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException exception) {
                throw new InfraiTransportException("Could not reach the logging service", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new InfraiTransportException("Log request was interrupted", exception);
            }

            String envelope = response.body();
            boolean ok;
            try {
                ok = Json.booleanField(envelope, "ok");
            } catch (IllegalArgumentException exception) {
                throw new InfraiTransportException("Logging response was not an envelope", exception);
            }
            if (ok) return envelope;

            if (response.statusCode() == 429 && (!retryableWrite || request.headers().firstValue("Idempotency-Key").isPresent())
                    && attempt < 3) {
                pause(retryDelayMillis(response, attempt));
                continue;
            }
            String code = Json.stringField(envelope, "code");
            throw new InfraiException(code == null ? "request rejected" : code, envelope, response.statusCode());
        }
        throw new IllegalStateException("Retry loop ended unexpectedly");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> {
                    try { return Long.parseLong(value) * 1_000L; }
                    catch (NumberFormatException ignored) { return 500L * (1L << attempt); }
                })
                .orElse(500L * (1L << attempt));
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new InfraiTransportException("Retry wait was interrupted", exception);
        }
    }

    public static final class InfraiException extends RuntimeException {
        private final int statusCode;
        private final String envelope;

        InfraiException(String code, String envelope, int statusCode) {
            super(code);
            this.envelope = envelope;
            this.statusCode = statusCode;
        }

        public int statusCode() { return statusCode; }
        public String envelope() { return envelope; }
    }

    public static final class InfraiTransportException extends RuntimeException {
        InfraiTransportException(String message, Throwable cause) { super(message, cause); }
    }
}
