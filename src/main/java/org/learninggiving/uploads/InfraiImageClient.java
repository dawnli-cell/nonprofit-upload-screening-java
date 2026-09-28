package org.learninggiving.uploads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Component
public final class InfraiImageClient implements ImagePublisher {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json;
    private final UploadScreeningProperties properties;

    public InfraiImageClient(ObjectMapper json, UploadScreeningProperties properties) {
        this.json = json;
        this.properties = properties;
    }

    @Override
    public Object resizeForPublication(byte[] image, String filename, String idempotencyKey) {
        var payload = json.createObjectNode();
        payload.putObject("image").put("base64", Base64.getEncoder().encodeToString(image));
        payload.put("width", properties.publishWidth());
        payload.put("height", properties.publishHeight());
        payload.put("fit", "cover");
        payload.put("enlarge", false);
        payload.put("format", "webp");
        payload.put("idempotency_key", idempotencyKey);
        payload.put("store", true);
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/v1/image/resize"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429) {
                    pause(response, attempt);
                    continue;
                }
                if (!envelope.path("ok").asBoolean()) {
                    JsonNode error = envelope.path("error");
                    throw new InfraiRequestException(error.path("code").asText("REQUEST_REJECTED"),
                            error.path("message").asText("Request rejected"), response.statusCode());
                }
                if (response.statusCode() >= 500) {
                    throw new InfraiRequestException("TRANSPORT_ERROR", "Upstream transport error", response.statusCode());
                }
                return json.convertValue(envelope.path("data"), Object.class);
            } catch (IOException e) {
                throw new InfraiRequestException("INVALID_RESPONSE", e.getMessage(), 502);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfraiRequestException("INTERRUPTED", "Request interrupted", 503);
            }
        }
        throw new InfraiRequestException("RATE_LIMITED", "Retry window exhausted", 429);
    }

    private static void pause(HttpResponse<?> response, int attempt) throws InterruptedException {
        long seconds = response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value.trim()))
                .orElse(1L << attempt);
        Thread.sleep(Math.min(seconds, 30) * 1000L);
    }

}
