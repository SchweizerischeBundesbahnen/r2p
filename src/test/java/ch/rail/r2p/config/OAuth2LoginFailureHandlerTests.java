package ch.rail.r2p.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.rail.r2p.NoAuthSpringBootTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that a failed browser OIDC login ends on the static, generic {@code login-error.html}
 * page instead of Spring's default {@code /login?error} (not served, 404), and that the error
 * details are only logged, not exposed to the user.
 */
@NoAuthSpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class OAuth2LoginFailureHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oauth2_error_is_logged_with_description_and_redirects_to_generic_error_page(CapturedOutput output)
        throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2Error error = new OAuth2Error("invalid_token_response",
            "AADSTS7000215: Invalid client secret provided.", "https://example.org/err");

        SecurityConfig.oauth2LoginFailureHandler().onAuthenticationFailure(
            new MockHttpServletRequest(), response, new OAuth2AuthenticationException(error));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login-error.html");
        assertThat(output)
            .contains("code=invalid_token_response")
            .contains("description=AADSTS7000215: Invalid client secret provided.")
            .contains("uri=https://example.org/err");
    }

    @Test
    void non_oauth2_error_is_logged_and_redirects_to_generic_error_page(CapturedOutput output) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityConfig.oauth2LoginFailureHandler().onAuthenticationFailure(
            new MockHttpServletRequest(), response, new BadCredentialsException("bad credentials"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login-error.html");
        assertThat(output).contains("OAuth2 login failed: bad credentials");
    }

    @Test
    void failed_login_callback_redirects_to_login_error_page(CapturedOutput output) throws Exception {
        // Callback without a stored authorization request -> authorization_request_not_found
        mockMvc.perform(get("/login/oauth2/code/azure").param("code", "x").param("state", "y"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login-error.html"));
        assertThat(output).contains("code=authorization_request_not_found");
    }

    @Test
    void login_error_page_is_public() throws Exception {
        mockMvc.perform(get("/login-error.html")).andExpect(status().isOk());
    }
}
