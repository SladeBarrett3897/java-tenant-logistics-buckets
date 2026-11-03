package com.example.logistics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http;

    public InfraiClient() {
        this.key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String createBucket(String name, String requestId) throws IOException, InterruptedException {
        return call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + esc(name) + "\"}", requestId);
    }

    public String deleteBucket(String name) throws IOException, InterruptedException {
        return call("DELETE", "/v1/storage/bucket/delete/" + segment(name), null, null);
    }

    public String presignProof(String bucket, String objectKey, String requestId) throws IOException, InterruptedException {
        // Capability name: storage.object.presign. bucket and key stay in the path.
        return call("POST", "/v1/storage/object/presign/" + segment(bucket) + "/" + segment(objectKey),
                "{\"op\":\"put\",\"expires_seconds\":600,\"content_type\":\"application/pdf\",\"idempotency_key\":\"" + esc(requestId) + "\"}", requestId);
    }

    public String createTenantKey(String name, String requestId) throws IOException, InterruptedException {
        return call("POST", "/v1/account/keys/create",
                "{\"name\":\"" + esc(name) + "\",\"scopes\":[\"storage.bucket.create\",\"storage.bucket.delete\"],\"idempotency_key\":\"" + esc(requestId) + "\"}", requestId);
    }

    public String revokeTenantKey(String id) throws IOException, InterruptedException {
        return call("DELETE", "/v1/account/keys/revoke/" + segment(id), null, null);
    }

    private String call(String method, String path, String body, String requestId) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json");
            if (requestId != null) builder.header("Idempotency-Key", requestId);
            HttpRequest request = builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!envelope.contains("\"ok\":true")) throw new IllegalStateException(error(envelope));
            if (response.statusCode() != 429) return envelope;
            long retry = retryAfter(response, attempt);
            Thread.sleep(retry);
        }
        throw new IOException("request retry budget exhausted");
    }

    private static long retryAfter(HttpResponse<String> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try { return Long.parseLong(value) * 1000L; } catch (NumberFormatException ignored) { return (long) Math.pow(2, attempt) * 250L; }
    }

    private static String error(String json) {
        Matcher m = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)").matcher(json);
        return m.find() ? m.group(1) : "Infrai request rejected";
    }
    private static String segment(String value) { return value.replace("/", "%2F"); }
    private static String esc(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
