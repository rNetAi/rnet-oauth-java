package io.github.rnetai.oauth;

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
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        this.config = config;
        this.httpClient = HttpClient.newBuilder().build();
    }

    RNetAi(RNetConfig config, HttpClient httpClient) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    public Map<String, Object> chat(Object body, String accessToken, String model)
            throws IOException, InterruptedException {
        if (accessToken == null)
            throw new IllegalArgumentException("accessToken is required");
        if (model == null)
            throw new IllegalArgumentException("model is required");

        String url = config.getAiProvider() + "/ai?access_token=" + urlEncode(accessToken) + "&model="
                + urlEncode(model);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    public java.io.InputStream chatStream(Object body, String accessToken, String model)
            throws IOException, InterruptedException {
        if (accessToken == null)
            throw new IllegalArgumentException("accessToken is required");
        if (model == null)
            throw new IllegalArgumentException("model is required");

        String url = config.getAiProvider() + "/ai/stream?access_token=" + urlEncode(accessToken) + "&model="
                + urlEncode(model);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());
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

    public Map<String, Object> geminiFileUpload(String accessToken, String model, byte[] fileData, String mimeType, String displayName) throws IOException, InterruptedException {
        return uploadFile(accessToken, model, fileData, mimeType, displayName);
    }

    public Map<String, Object> openAIFileUpload(String accessToken, String model, byte[] fileData, String mimeType, String displayName) throws IOException, InterruptedException {
        return uploadFile(accessToken, model, fileData, mimeType, displayName);
    }

    public Map<String, Object> claudeFileUpload(String accessToken, String model, byte[] fileData, String mimeType, String displayName) throws IOException, InterruptedException {
        return uploadFile(accessToken, model, fileData, mimeType, displayName);
    }

    public Map<String, Object> openAIFileDelete(String accessToken, String model, String fileId) throws IOException, InterruptedException {
        return deleteFile(accessToken, model, fileId);
    }

    public Map<String, Object> claudeFileDelete(String accessToken, String model, String fileId) throws IOException, InterruptedException {
        return deleteFile(accessToken, model, fileId);
    }

    private Map<String, Object> uploadFile(String accessToken, String model, byte[] fileData, String mimeType, String displayName) throws IOException, InterruptedException {
        if (accessToken == null) throw new IllegalArgumentException("accessToken is required");
        if (model == null) throw new IllegalArgumentException("model is required");
        if (fileData == null) throw new IllegalArgumentException("fileData is required");
        if (mimeType == null) throw new IllegalArgumentException("mimeType is required");

        String url = config.getAiProvider() + "/ai/upload?access_token=" + urlEncode(accessToken) + "&model=" + urlEncode(model);
        String boundary = java.util.UUID.randomUUID().toString().replace("-", "");

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        // file part
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        String filename = (displayName != null && !displayName.isEmpty()) ? displayName : "file";
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(fileData);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));

        // mimeType part
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"mimeType\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(mimeType.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));

        // displayName part
        if (displayName != null) {
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"displayName\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(displayName.getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }

        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    private Map<String, Object> deleteFile(String accessToken, String model, String fileId) throws IOException, InterruptedException {
        if (accessToken == null) throw new IllegalArgumentException("accessToken is required");
        if (model == null) throw new IllegalArgumentException("model is required");
        if (fileId == null) throw new IllegalArgumentException("fileId is required");

        String url = config.getAiProvider() + "/ai/upload?access_token=" + urlEncode(accessToken) + "&model=" + urlEncode(model) + "&fileId=" + urlEncode(fileId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
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
