package io.github.rnetai.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RNetAi {
    private static final Logger logger = LoggerFactory.getLogger(RNetAi.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RNetConfig config;
    private final HttpClient httpClient;

    public RNetAi(RNetConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public Map<String, Object> chat(Object body, String accessToken, String model) throws IOException, InterruptedException {
        if (accessToken == null) throw new IllegalArgumentException("accessToken is required");
        if (model == null) throw new IllegalArgumentException("model is required");

        String url = config.getAiProvider() + "/ai?access_token=" + urlEncode(accessToken) + "&model=" + urlEncode(model);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    public java.io.InputStream chatStream(Object body, String accessToken, String model) throws IOException, InterruptedException {
        if (accessToken == null) throw new IllegalArgumentException("accessToken is required");
        if (model == null) throw new IllegalArgumentException("model is required");

        String url = config.getAiProvider() + "/ai/stream?access_token=" + urlEncode(accessToken) + "&model=" + urlEncode(model);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            try (java.io.InputStream is = response.body()) {
                String errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String errorMessage = "";
                try {
                    Map<String, Object> errorData = objectMapper.readValue(errorBody, Map.class);
                    errorMessage = (String) errorData.getOrDefault("error", "");
                } catch (Exception e) {
                    errorMessage = errorBody;
                }
                throw new IOException("AI stream request failed: " + response.statusCode() + " - " + errorMessage);
            }
        }
        return response.body();
    }

    private Map<String, Object> handleResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 400) {
            logger.error("Request failed: {} {}", response.statusCode(), response.body());
            throw new IOException("Request failed: " + response.statusCode() + " " + response.body());
        }
        return objectMapper.readValue(response.body(), Map.class);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
