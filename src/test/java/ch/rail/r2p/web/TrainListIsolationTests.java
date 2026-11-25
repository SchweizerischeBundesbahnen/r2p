package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.Actor;
import ch.rail.r2p.NoAuthSpringBootTest;
import ch.rail.r2p.api.dto.TrainListElement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests that a single train with a corrupt (undeserializable) payload is skipped during listTrains
 * and does not prevent valid trains from being returned.
 */
@NoAuthSpringBootTest
class TrainListIsolationTests {

    @Autowired
    private TrainController trainController;

    @Autowired
    private Actor actor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        actor.setup();
    }

    @AfterEach
    void tearDown() {
        actor.cleanup();
    }

    @Test
    void should_skip_train_with_corrupt_payload_and_return_valid_trains() {
        // Given: one valid train
        TrainListElement validTrain = actor.createTrain(SBB, "valid train");

        // And: a second train whose payload is then corrupted in the DB to contain an unknown enum value
        TrainListElement corruptTrain = actor.createTrain(SBB, "corrupt train");
        UUID corruptVersionId = corruptTrain.getBranches().getFirst().getTechnical().getVersion();
        corruptPayloadInDb(corruptVersionId);

        // When: listing all trains
        List<TrainListElement> result = trainController.listTrains(SBB, null, null).getBody();

        // Then: only the valid train is returned; the corrupt one is silently skipped
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTrainId()).isEqualTo(validTrain.getTrainId());
    }

    @Test
    void should_return_empty_list_when_all_trains_have_corrupt_payloads() {
        // Given: two trains both with corrupt payloads
        TrainListElement train1 = actor.createTrain(SBB, "corrupt train 1");
        TrainListElement train2 = actor.createTrain(SBB, "corrupt train 2");
        corruptPayloadInDb(train1.getBranches().getFirst().getTechnical().getVersion());
        corruptPayloadInDb(train2.getBranches().getFirst().getTechnical().getVersion());

        // When
        List<TrainListElement> result = trainController.listTrains(SBB, null, null).getBody();

        // Then: empty list — no exception thrown
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void should_return_all_trains_when_no_payloads_are_corrupt() {
        // Given: two valid trains
        actor.createTrain(SBB, "train one");
        actor.createTrain(SBB, "train two");

        // When
        List<TrainListElement> result = trainController.listTrains(SBB,
            LocalDate.parse("2024-01-01"), LocalDate.parse("2030-01-01")).getBody();

        // Then: both trains are returned
        assertThat(result).isNotNull().hasSize(2);
    }

    /**
     * Directly overwrites the payload column of the given version with JSON that contains
     * an unknown enum value, simulating legacy data incompatible with the current schema.
     */
    private void corruptPayloadInDb(UUID versionId) {
        byte[] corruptPayload = """
            {"references":[{"type":"tttreftrain","value":"corrupt-value"}]}
            """.getBytes();
        jdbcTemplate.update(
            "UPDATE train_version SET payload = ? WHERE version = ?",
            corruptPayload, versionId
        );
    }
}

