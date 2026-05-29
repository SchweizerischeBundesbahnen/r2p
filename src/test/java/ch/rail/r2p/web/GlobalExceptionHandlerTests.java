package ch.rail.r2p.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.rail.r2p.Actor;
import ch.rail.r2p.NoAuthSpringBootTest;
import ch.rail.r2p.WithMockR2PUser;
import ch.rail.r2p.api.dto.Train;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for GlobalExceptionHandler using MockMvc to verify exception handling through the WebMvc layer.
 */
@NoAuthSpringBootTest
@AutoConfigureMockMvc
@WithMockR2PUser
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Actor actor;

    @Autowired
    private ObjectMapper objectMapper;

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
                .with(csrf())
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

    @Test
    void should_handle_authorization_denied_exception() throws Exception {
        // Given: a valid train JSON and a partner that is not the instance owner
        Train sampleTrain = actor.loadSampleTrain();
        String validTrainJson = objectMapper.writeValueAsString(sampleTrain);

        // When: a non-instance owner tries to create a train
        mockMvc.perform(post("/v1/trains")
                .with(csrf())
                .header("token", "SOB")  // SOB is not the instance owner (SBB is)
                .param("comment", "test comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validTrainJson)
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 403 Forbidden with ProblemDetail
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value(containsString("Partner is not instance owner")));
    }

    @Test
    void should_handle_illegal_argument_exception() throws Exception {
        // Given: invalid date parameters where 'to' is before 'from'
        String from = "2027-01-01";
        String to = "2024-01-01";

        // When: requesting train list with invalid date range
        mockMvc.perform(get("/v1/trains")
                .header("token", "SBB")
                .param("operationalDayFrom", from)
                .param("operationalDayTo", to)
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 400 Bad Request with ProblemDetail
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value(containsString("Parameter 'operationalDayTo' (to) must not be before 'operationalDayFrom' (from)")));
    }

    @Test
    void should_handle_illegal_state_exception() throws Exception {
        // When: calling an endpoint that might throw IllegalStateException
        mockMvc.perform(get("/test/illegal-state")
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 500 Internal Server Error with ProblemDetail
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void should_handle_merge_conflict_exception() throws Exception {
        // When: calling an endpoint that throws MergeConflictException
        mockMvc.perform(get("/test/merge-conflict")
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 409 Conflict with ProblemDetail containing the exception message
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.detail").value(containsString("reviewed version is stale")));
    }

    @Test
    void should_handle_invalid_branch_state_exception() throws Exception {
        // When: calling an endpoint that throws InvalidBranchStateException
        mockMvc.perform(get("/test/invalid-branch-state")
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 409 Conflict with ProblemDetail
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.detail").value(containsString("branch already merged")));
    }

    @Test
    void should_handle_payload_deserialization_exception_as_500_with_sanitized_message() throws Exception {
        // When: a stored train payload cannot be deserialized (e.g. unknown enum value 'tttreftrain')
        mockMvc.perform(get("/test/payload-deserialization")
                .accept(MediaType.APPLICATION_JSON))
            // Then: should return 500 (not 400) with a sanitized message that hides Jackson internals
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("A stored train payload could not be deserialized. Please contact support."));
    }
}