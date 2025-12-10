package ch.rail.r2p;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class R2pApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void should_redirect_to_swagger_ui() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/swagger-ui/index.html"));
	}

	@Test
	@WithMockUser(authorities = {"SCOPE_r2p"})
	void should_return_403_missing_role() throws Exception {
		mockMvc.perform(get("/helloworld")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = {"ROLE_r2p_user"})
	void should_return_403_missing_scope() throws Exception {
		mockMvc.perform(get("/helloworld")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	void should_return_401_without_authentication() throws Exception {
		mockMvc.perform(get("/helloworld")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
}
