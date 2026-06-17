package ch.rail.r2p.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.rail.r2p.NoAuthSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that unauthenticated requests to protected resources ({@code /v1/**} and
 * {@code /internal/**}) are answered with 401 Unauthorized and never with a 302 redirect to the
 * IdP login page.
 * <p>
 * This guards the {@code authenticationEntryPoint} configuration in {@link SecurityConfig}:
 * without it, oauth2Login's {@code LoginUrlAuthenticationEntryPoint} would issue a 302 to
 * {@code /oauth2/authorization/azure -> login.microsoftonline.com}, which an AJAX fetch follows
 * cross-origin and browsers block with a CORS error. The integrated UI relies on the 401 to
 * trigger the login redirect itself (see {@code r2p-api.js} / {@code router.js}).
 * <p>
 * Note: deliberately <em>not</em> annotated with {@code @WithMockR2PUser} so requests are
 * unauthenticated and actually exercise the authentication entry point.
 */
@NoAuthSpringBootTest
@AutoConfigureMockMvc
class ApiAuthenticationEntryPointTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticated_api_request_returns_401_and_no_redirect() throws Exception {
        mockMvc.perform(get("/v1/trains")
                .accept(MediaType.APPLICATION_JSON))
            // Must be 401, never a 302 redirect to the IdP login page.
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void unauthenticated_api_request_to_single_resource_returns_401_and_no_redirect() throws Exception {
        mockMvc.perform(get("/v1/trains/00000000-0000-0000-0000-000000000000")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void unauthenticated_internal_request_returns_401_and_no_redirect() throws Exception {
        // /internal/** is also consumed by the UI via AJAX (e.g. fetchStats -> /internal/stats)
        // and must therefore return 401 rather than a redirect to the IdP login page.
        mockMvc.perform(get("/internal/stats")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Location"));
    }
}
