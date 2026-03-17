package ch.rail.r2p.web;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/test-data")
@ConditionalOnProperty(name = "r2p.test-data.enabled", havingValue = "true")
public class TestDataController {

    private final TestDataService testDataService;

    public TestDataController(TestDataService testDataService) {
        this.testDataService = testDataService;
    }

    /**
     * Generates test data based on the given parameters.
     * Only available when {@code r2p.test-data.enabled=true}.
     */
    @PostMapping("/generate")
    public ResponseEntity<TestDataResult> generate(@RequestBody @Valid TestDataRequest request) {
        TestDataResult result = testDataService.generate(request);
        return ResponseEntity.ok(result);
    }
}

