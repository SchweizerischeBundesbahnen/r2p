package ch.rail.r2p.config;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationConverterTests {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void should_map_scope_and_roles_to_authorities_and_use_preferred_username() {
        Jwt jwt = Jwt.withTokenValue("test-token")
            .headers(h -> h.put("alg", "none"))
            .claim("scp", "r2p extra")
            .claim("roles", List.of("r2p_user", "admin"))
            .claim("preferred_username", "user@example.org")
            .subject("subject-value")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        AbstractAuthenticationToken auth = securityConfig.jwtAuthenticationConverter().convert(jwt);
        assertNotNull(auth);
        assertEquals("user@example.org", auth.getName(), "Principal should be preferred_username");

        Set<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertTrue(authorities.contains("SCOPE_r2p"));
        assertTrue(authorities.contains("SCOPE_extra"));
        assertTrue(authorities.contains("ROLE_r2p_user"));
        assertTrue(authorities.contains("ROLE_admin"));
        assertEquals(4, authorities.size(), "Exactly 4 authorities expected");
    }

    @Test
    void should_fallback_to_subject_when_preferred_username_missing() {
        Jwt jwt = Jwt.withTokenValue("test-token")
            .headers(h -> h.put("alg", "none"))
            .claim("scp", "r2p")
            .claim("roles", List.of("r2p_user"))
            .subject("subject-fallback")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        AbstractAuthenticationToken auth = securityConfig.jwtAuthenticationConverter().convert(jwt);
        Objects.requireNonNull(auth);
        assertEquals("subject-fallback", auth.getName(), "Should use subject as principal when preferred_username absent");
    }

    @Test
    void should_ignore_blank_entries_and_empty_claims() {
        Jwt jwt = Jwt.withTokenValue("test-token")
            .headers(h -> h.put("alg", "none"))
            .claim("scp", "   ") // blank scopes
            .claim("roles", List.of("", "   ")) // blank roles
            .subject("subj")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        AbstractAuthenticationToken auth = securityConfig.jwtAuthenticationConverter().convert(jwt);
        Objects.requireNonNull(auth);
        assertTrue(auth.getAuthorities().isEmpty(), "No authorities should be created from blank claims");
    }

    @Test
    void should_map_authorities_for_example_token() {
        // Provided example token claims (simplified, we ignore cryptographic correctness)
        Jwt jwt = Jwt.withTokenValue("example-token")
            .headers(h -> h.put("alg", "none"))
            .claim("aud", "f5b54c65-e0b1-4233-bc60-bf4c59f399bd")
            .claim("iss", "https://login.microsoftonline.com/2cda5d11-f0ac-46b3-967d-af1b2e1bd01a/v2.0")
            .claim("scp", "r2p")
            .claim("roles", List.of("r2p_user"))
            .claim("preferred_username", "patrick.uhlmann@sbb.ch")
            .claim("sub", "Su2ykyZr6_kVfLZXHPBDB9B76moJwypmIj1h3E6fGrc")
            .subject("Su2ykyZr6_kVfLZXHPBDB9B76moJwypmIj1h3E6fGrc")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        AbstractAuthenticationToken auth = securityConfig.jwtAuthenticationConverter().convert(jwt);
        Objects.requireNonNull(auth);
        assertEquals("patrick.uhlmann@sbb.ch", auth.getName());
        Set<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertEquals(Set.of("SCOPE_r2p", "ROLE_r2p_user"), authorities);
    }
}
