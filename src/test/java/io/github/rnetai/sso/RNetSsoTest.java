package io.github.rnetai.sso;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RNetSsoTest {

    @Test
    public void testPKCEGeneration() {
        PKCEUtil.PKCE pkce = PKCEUtil.generate();
        assertNotNull(pkce.getVerifier());
        assertNotNull(pkce.getChallenge());
        assertNotEquals(pkce.getVerifier(), pkce.getChallenge());
    }

    @Test
    public void testLoginUrlGeneration() {
        RNetConfig config = new RNetConfig("test-client", "test-secret", "http://localhost/callback");
        RNetAuth sso = new RNetAuth(config);
        
        String url = sso.getAuthorizationUrl("test-challenge");
        assertTrue(url.contains("client_id=test-client"));
        assertTrue(url.contains("code_challenge=test-challenge"));
        assertTrue(url.contains("scope=openid+profile"));
    }
}
