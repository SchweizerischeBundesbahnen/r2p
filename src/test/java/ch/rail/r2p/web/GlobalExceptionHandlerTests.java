package ch.rail.r2p.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.rail.r2p.WithMockR2PUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for GlobalExceptionHandler using MockMvc to verify exception handling through the WebMvc layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockR2PUser
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_handle_no_such_element_exception() throws Exception {
        // Given: a non-existent train ID
        UUID nonExistentTrainId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // When: requesting a non-existent train
        mockMvc.perform(get("/v1/trains/" + nonExistentTrainId)
                .header("token", "SBB")
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 404 with ProblemDetail
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void should_handle_method_argument_not_valid_exception() throws Exception {
        // Given: an invalid train JSON (missing required fields like trainNumber)
        String invalidTrainJson = """
            {
              "id": "invalid-train",
              "leadRu": "SBB"
            }
            """;

        // When: attempting to create a train with invalid data
        mockMvc.perform(post("/v1/trains")
                .header("token", "SBB")
                .param("comment", "test comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidTrainJson)
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 400 with "Validation failed" message
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value(containsString("Validation failed")));
    }

    @Test
    void should_handle_method_argument_type_mismatch_exception() throws Exception {
        // When: requesting with an invalid UUID format in path variable
        mockMvc.perform(get("/v1/trains/not-a-valid-uuid")
                .header("token", "SBB")
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 400 with custom error message from our handler
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value(containsString("Invalid value")))
            .andExpect(jsonPath("$.detail").value(containsString("trainId")));
    }

    @Test
    void should_handle_constraint_violation_exception() throws Exception {
        // Given: a request parameter that violates the @Size(max = 10) constraint
        String tooLongValue = "this-value-is-longer-than-10-characters";

        // When: calling the test endpoint with a constraint violation
        mockMvc.perform(get("/test/constraint")
                .param("value", tooLongValue)
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 400 with constraint violation message
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value(containsString("size must be between 1 and 10")));
    }
}