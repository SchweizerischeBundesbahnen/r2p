package ch.rail.r2p;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@NoAuthSpringBootTest
@AutoConfigureMockMvc
class R2pApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void should_serve_index_html_at_root() throws Exception {
		// Home page now forwards to static index.html
		mockMvc.perform(get("/"))
				.andExpect(status().isOk());
	}

    @Test
    @WithMockUser(authorities = {"SCOPE_r2p"})
    void should_return_403_missing_role() throws Exception {
        mockMvc.perform(get("/v1/trains")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_r2p_user"})
    void should_allow_session_user_with_only_role() throws Exception {
        // A session-based request (no Bearer header) needs only ROLE_r2p_user –
        // Azure AD ID tokens do not carry an 'scp' claim.
        mockMvc.perform(get("/v1/trains")
                .header("token", "SBB")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void should_return_401_without_authentication() throws Exception {
        mockMvc.perform(get("/helloworld")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
