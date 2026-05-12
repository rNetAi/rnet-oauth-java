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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RNetAuth {
    private static final Logger logger = LoggerFactory.getLogger(RNetAuth.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RNetConfig config;
    private final HttpClient httpClient;

    public RNetAuth(RNetConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public PKCEUtil.PKCE generatePKCE() {
        return PKCEUtil.generate();
    }

    public String getAuthorizationUrl(String challenge) {
        return getAuthorizationUrl(challenge, null);
    }

    public String getAuthorizationUrl(String challenge, String state) {
        StringBuilder url = new StringBuilder(config.getIssuer())
                .append("/oauth2/authorize?")
                .append("response_type=code")
                .append("&client_id=").append(urlEncode(config.getClientId()))
                .append("&redirect_uri=").append(urlEncode(config.getRedirectUri()))
                .append("&scope=").append(urlEncode("openid profile email"));

        if (challenge != null) {
            url.append("&code_challenge=").append(urlEncode(challenge));
            url.append("&code_challenge_method=S256");
        }

        if (state != null) {
            url.append("&state=").append(urlEncode(state));
        }

        return url.toString();
    }

    public Map<String, Object> exchangeCodeForToken(String code, String codeVerifier) throws IOException, InterruptedException {
        String tokenEndpoint = config.getIssuer() + "/oauth2/token";
        
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", config.getRedirectUri());
        
        if (codeVerifier != null) {
            params.put("code_verifier", codeVerifier);
        }

        return postForm(tokenEndpoint, params, true);
    }

    public Map<String, Object> refreshAccessToken(String refreshToken) throws IOException, InterruptedException {
        String tokenEndpoint = config.getIssuer() + "/oauth2/token";
        
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        return postForm(tokenEndpoint, params, true);
    }

    private Map<String, Object> postForm(String url, Map<String, String> params, boolean useBasicAuth) throws IOException, InterruptedException {
        String formBody = params.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody));

        if (useBasicAuth) {
            String auth = config.getClientId() + ":" + config.getClientSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encodedAuth);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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
