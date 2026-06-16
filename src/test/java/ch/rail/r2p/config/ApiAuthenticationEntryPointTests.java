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
 * Verifies that unauthenticated requests to the REST API ({@code /v1/**}) are answered with
 * 401 Unauthorized and never with a 302 redirect to the IdP login page.
 * <p>
 * This guards the {@code defaultAuthenticationEntryPointFor} configuration in
 * {@link SecurityConfig}: without it, oauth2Login's {@code LoginUrlAuthenticationEntryPoint}
 * would issue a 302 to {@code login.microsoftonline.com}, which browsers block with a CORS error.
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
}
