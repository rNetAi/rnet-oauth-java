# RNet OAuth Java Library

A lightweight Java library for integrating **RNet OAuth** and **AI Provider** services. This library allows users to authenticate via RNet OAuth and pay for AI model token costs directly using their RNet account.

## Features

- **OAuth2 PKCE Support**: Secure authorization code flow with automatic code verifier and challenge generation.
- **Token Management**: Exchange authorization codes for tokens and refresh expired tokens.
- **UserInfo Endpoint**: Fetch the authenticated user's RNet profile with an access token.
- **AI Integration**: Easy methods to chat with AI models using standard or streaming responses.
- **Lightweight**: Minimal dependencies (Jackson for JSON, SLF4J for logging).
- **Modern Java**: Built for Java 25.

## Installation

### Maven
Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.rnetai</groupId>
    <artifactId>rnet-oauth</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Quick Start

### 1. Initialize the Clients
```java
RNetConfig config = new RNetConfig("client-id", "client-secret", "redirect-uri");
RNetAuth auth = new RNetAuth(config);
RNetAi ai = new RNetAi(config);
```

### 2. Generate Authorization URL (OAuth2 PKCE)
```java
// Generate PKCE values
PKCEUtil.PKCE pkce = auth.generatePKCE();
String codeVerifier = pkce.getVerifier(); // Store this in user session

// Get the URL to redirect the user to
// challenge: PKCE code challenge (optional)
// state: An optional string to maintain state between the request and callback (recommended for security)
String authUrl = auth.getAuthorizationUrl(pkce.getChallenge(), "optional-state");
```

### 3. Exchange Code for Tokens
```java
// After the user redirects back with a ?code=...
Map<String, Object> tokenResponse = auth.exchangeCodeForToken(authCode, codeVerifier);
String accessToken = (String) tokenResponse.get("access_token");
String refreshToken = (String) tokenResponse.get("refresh_token");
```

### 4. Refresh Tokens
```java
Map<String, Object> refreshedTokens = auth.refreshAccessToken(refreshToken);
String newAccessToken = (String) refreshedTokens.get("access_token");
```

### 5. Get User Info
```java
Map<String, Object> userInfo = auth.getUserInfo(accessToken);
System.out.println(userInfo.get("email"));
System.out.println(userInfo.get("name"));
```

The UserInfo response comes from RNet's `/userinfo` endpoint and may include:
`sub`, `email`, `email_verified`, `name`, `preferred_username`, `user_id`, `role`, and `status`.

### 6. Chat with AI
```java
Map<String, Object> payload = Map.of(
    "contents", List.of(
        Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", "Hello!"))
        )
    )
);

Map<String, Object> response = ai.chat(payload, accessToken, "gemini-2.5-flash-lite");
System.out.println(response);
```

### 7. Streaming AI Response (Untested)
```java
InputStream stream = ai.chatStream(payload, accessToken, "gemini-2.5-flash-lite");
// Process the stream...
```

### 8. File Upload (Untested)
```java
import java.nio.file.Files;
import java.nio.file.Path;

byte[] fileBuffer = Files.readAllBytes(Path.of("document.pdf"));

// Upload to Gemini
Map<String, Object> geminiUpload = ai.geminiFileUpload(accessToken, "gemini-2.5-flash-lite", fileBuffer, "application/pdf", "document.pdf");
System.out.println(geminiUpload.get("fileReference")); // Use this in chat payload

// Upload to OpenAI
Map<String, Object> openaiUpload = ai.openAIFileUpload(accessToken, "gpt-4o", fileBuffer, "application/pdf", "document.pdf");
```

### 9. File Deletion (Untested)
```java
// Gemini files auto-delete after 48 hours, so there is no delete method.
// Delete an OpenAI file:
ai.openAIFileDelete(accessToken, "gpt-4o", (String) openaiUpload.get("fileReference"));
```

### 10. AI Chat with File & Tools (Untested)
```java
Map<String, Object> payload = Map.of(
    "contents", List.of(
        Map.of(
            "role", "user",
            "parts", List.of(
                Map.of("text", "Based on this document, what is my name? Also search the web for the current weather in London."),
                Map.of("fileData", Map.of("fileUri", geminiUpload.get("fileReference"), "mimeType", geminiUpload.get("mimeType")))
            )
        )
    ),
    "tools", List.of(
        Map.of("googleSearch", Map.of()) // Enable Google Search tool
    )
);

Map<String, Object> response = ai.chat(payload, accessToken, "gemini-2.5-flash-lite");
System.out.println(response);
```

## License
This project is licensed under the MIT License.
