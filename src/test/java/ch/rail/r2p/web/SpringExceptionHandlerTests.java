package ch.rail.r2p.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.rail.r2p.WithMockR2PUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockR2PUser
class SpringExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_handle_method_not_allowed() throws Exception {
        // When: attempting to use POST on an existing endpoint that does not allow POST
        // note: this is handled by Spring's default handler, not GlobalExceptionHandler
        mockMvc.perform(post("/v1/trains/" + UUID.randomUUID() + "/branches/" + UUID.randomUUID())
                .header("token", "SBB")
                .param("comment", "test comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 405 Method Not Allowed
            .andExpect(status().isMethodNotAllowed());
    }
}