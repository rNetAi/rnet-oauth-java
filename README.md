# RNet Auth Java Library

A lightweight Java library for integrating **RNet Auth** and **AI Provider** services. This library allows users to authenticate via RNet and pay for AI model token costs directly using their RNet account.

## Features

- **OAuth2 PKCE Support**: Secure authorization code flow with automatic code verifier and challenge generation.
- **Token Management**: Exchange authorization codes for tokens and refresh expired tokens.
- **AI Integration**: Easy methods to chat with AI models using standard or streaming responses.
- **Lightweight**: Minimal dependencies (Jackson for JSON, SLF4J for logging).
- **Modern Java**: Built for Java 25.

## Installation

### Maven
Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.rnetai</groupId>
    <artifactId>rnet-sso</artifactId>
    <version>1.0.0</version>
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

### 5. Chat with AI
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

### 6. Streaming AI Response
```java
InputStream stream = ai.chatStream(payload, accessToken, "gemini-2.5-flash-lite");
// Process the stream...
```

## License
This project is licensed under the MIT License.
