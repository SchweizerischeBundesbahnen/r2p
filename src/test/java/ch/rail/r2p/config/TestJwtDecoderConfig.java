package ch.rail.r2p.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Provides a stub {@link JwtDecoder} for all {@code @SpringBootTest} tests.
 * <p>
 * The real decoder would try to fetch the OIDC discovery document from the
 * Azure AD {@code issuer-uri} at runtime, which is not available in CI.
 * This stub always rejects tokens (simulating an invalid/expired JWT), which
 * is the correct behaviour tested by
 * {@code R2pApplicationTests.should_return_401_on_invalid_bearer_token}.
 */
@TestConfiguration
public class TestJwtDecoderConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return ignoredToken -> {
            throw new BadJwtException("Invalid or untrusted JWT (test stub)");
        };
    }
}



