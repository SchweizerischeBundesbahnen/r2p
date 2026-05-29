package ch.rail.r2p.web;

import ch.rail.r2p.persistence.impl.PayloadDeserializationException;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller used only for testing exception handlers.
 * This controller is only available in the test context.
 */
@RestController
@Validated
public class TestConstraintController {

    @GetMapping("/test/constraint")
    public ResponseEntity<String> testConstraint(
        @RequestParam @Size(min = 1, max = 10) String value
    ) {
        return ResponseEntity.ok("Valid: " + value);
    }

    @GetMapping("/test/illegal-state")
    public ResponseEntity<String> testIllegalState() {
        throw new IllegalStateException("Test illegal state exception - data integrity issue");
    }

    @GetMapping("/test/merge-conflict")
    public ResponseEntity<String> testMergeConflict() {
        throw new MergeConflictException("Test merge conflict - reviewed version is stale");
    }

    @GetMapping("/test/invalid-branch-state")
    public ResponseEntity<String> testInvalidBranchState() {
        throw new InvalidBranchStateException("Test invalid branch state - branch already merged");
    }

    @GetMapping("/test/payload-deserialization")
    public ResponseEntity<String> testPayloadDeserialization() {
        throw new PayloadDeserializationException("Test payload deserialization error",
            new RuntimeException("Unexpected value 'tttreftrain'"));
    }
}

