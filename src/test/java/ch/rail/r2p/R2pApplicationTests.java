package ch.rail.r2p;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class R2pApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void should_serve_index_html_at_root() throws Exception {
		// Home page now forwards to static index.html (security is temporarily disabled)
		mockMvc.perform(get("/"))
				.andExpect(status().isOk());
	}

	// TODO: re-enable these tests when security is restored
	// @Test
	// @WithMockUser(authorities = {"SCOPE_r2p"})
	// void should_return_403_missing_role() throws Exception { ... }

	// @Test
	// @WithMockUser(authorities = {"ROLE_r2p_user"})
	// void should_return_403_missing_scope() throws Exception { ... }

	// @Test
	// void should_return_401_without_authentication() throws Exception { ... }
}
