package ch.rail.r2p.config;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.rail.r2p.NoAuthSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the authentication entry point behaviour configured in {@link SecurityConfig} for
 * unauthenticated requests to protected resources ({@code /v1/**}, {@code /internal/**}).
 * <p>
 * Two paths must be distinguished (see {@code SecurityConfig#apiAuthenticationEntryPoint}):
 * <ul>
 *   <li><b>Session cookie / browser (no Bearer header):</b> plain {@code 401}, no
 *       {@code WWW-Authenticate} challenge and crucially no {@code 302} redirect to the IdP login
 *       page. An AJAX fetch follows a 302 cross-origin and the browser blocks it with a CORS error;
 *       the frontend instead turns the 401 into a login redirect itself (see {@code r2p-api.js} /
 *       {@code router.js}).</li>
 *   <li><b>Bearer token (REST API):</b> {@code 401} plus the RFC 6750
 *       {@code WWW-Authenticate: Bearer ...} challenge, so compliant clients still learn why the
 *       token was rejected.</li>
 * </ul>
 * Deliberately <em>not</em> annotated with {@code @WithMockR2PUser} so requests are unauthenticated
 * and actually exercise the entry point.
 */
@NoAuthSpringBootTest
@AutoConfigureMockMvc
class ApiAuthenticationEntryPointTests {

    @Autowired
    private MockMvc mockMvc;

    // Replaces the issuer-uri-backed JwtDecoder so a Bearer request fails locally (no network)
    // and routes through the BearerTokenAuthenticationEntryPoint.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void unauthenticated_session_api_request_returns_401_without_redirect_or_bearer_challenge() throws Exception {
        mockMvc.perform(get("/v1/trains")
                .accept(MediaType.APPLICATION_JSON))
            // Must be 401, never a 302 redirect to the IdP login page.
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
            // No Bearer header present -> plain 401, no Bearer challenge.
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void unauthenticated_session_api_request_to_single_resource_returns_401_and_no_redirect() throws Exception {
        mockMvc.perform(get("/v1/trains/00000000-0000-0000-0000-000000000000")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void unauthenticated_session_internal_request_returns_401_and_no_redirect() throws Exception {
        // /internal/** is also consumed by the UI via AJAX (e.g. fetchStats -> /internal/stats)
        // and must therefore return 401 rather than a redirect to the IdP login page.
        mockMvc.perform(get("/internal/stats")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void invalid_bearer_token_request_returns_401_with_www_authenticate_challenge() throws Exception {
        given(jwtDecoder.decode(anyString())).willThrow(new BadJwtException("invalid token"));

        mockMvc.perform(get("/v1/trains")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            // Still no redirect for API clients ...
            .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
            // ... but the RFC 6750 Bearer challenge must be preserved.
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")));
    }
}
