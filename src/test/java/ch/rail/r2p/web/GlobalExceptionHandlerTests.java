package ch.rail.r2p.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.rail.r2p.WithMockR2PUser;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for GlobalExceptionHandler using MockMvc to verify exception handling through WebMvc layer.
 * Each test uses .andDo(print()) to output the actual response for verification of which handler was called.
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
    void should_handle_method_not_allowed() throws Exception {
        // When: attempting to use POST on an endpoint that doesn't exist
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
    void should_handle_constraint_violation_exception() {
        // Given: a GlobalExceptionHandler instance and a ConstraintViolationException
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ConstraintViolationException exception = new ConstraintViolationException("Constraint violated", Set.of());

        // When: handling the exception
        ProblemDetail problemDetail = handler.handleConstraintViolationException(exception);

        // Then: should return 400 Bad Request with the exception message
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Constraint violated");
        assertThat(problemDetail.getTitle()).isEqualTo("Bad Request");
    }
}