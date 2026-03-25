package ch.rail.r2p.web;

import static ch.rail.r2p.domain.Branch.MAIN_BRANCH_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.Journey;
import ch.rail.r2p.api.dto.Journeylocation;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.Branch;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.impl.BranchSpringDataRepository;
import ch.rail.r2p.persistence.impl.ChangelogSpringDataRepository;
import ch.rail.r2p.persistence.impl.TrainSpringDataRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ChangelogTests {

    // This class tests querying the changelog.
    // In order to have test data with defined timestamps, changelogs are directly inserted via the SpringData Repository.

    @Autowired
    private TrainController trainController;

    @Autowired
    private ChangelogSpringDataRepository changelogSpringDataRepository;
    @Autowired
    private TrainSpringDataRepository trainSpringDataRepository;
    @Autowired
    private BranchSpringDataRepository branchSpringDataRepository;

    @BeforeEach
    void setUp() {
        cleanAll();
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        // Delete changelog first, including entries with null branch_id (SHARED/UNSHARED) that are not cascade-deleted with branch
        changelogSpringDataRepository.deleteAll();
        trainSpringDataRepository.deleteAllTrainPartners();
        branchSpringDataRepository.deleteAll(); // cascades to train_version via FK ON DELETE CASCADE
    }

    @Test
    void should_list_all() {
        UUID trainId1 = UUID.randomUUID();
        UUID trainId2 = UUID.randomUUID();
        createMinimalTrain(trainId1, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, MAIN_BRANCH_NAME);
        generateChangelog(trainId1, "SBB", MAIN_BRANCH_NAME, 4, OffsetDateTime.parse("2026-01-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SBB", MAIN_BRANCH_NAME, 5, OffsetDateTime.parse("2026-02-01T12:00+01:00"), Duration.ofDays(1));
        trainSpringDataRepository.addPartner(trainId1, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SBB");

        assertThat(trainController.listTrainChanges(PartnerToken.SBB, OffsetDateTime.MIN, OffsetDateTime.MAX).getBody()).hasSize(5 + 6);
    }

    @Test
    void should_list_all_according_to_interval() {
        UUID trainId1 = UUID.randomUUID();
        UUID trainId2 = UUID.randomUUID();
        createMinimalTrain(trainId1, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, MAIN_BRANCH_NAME);
        generateChangelog(trainId1, "SBB", MAIN_BRANCH_NAME, 4, OffsetDateTime.parse("2026-01-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SBB", MAIN_BRANCH_NAME, 5, OffsetDateTime.parse("2026-02-01T12:00+01:00"), Duration.ofDays(1));
        trainSpringDataRepository.addPartner(trainId1, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SBB");

        assertThat(
            trainController.listTrainChanges(PartnerToken.SBB, OffsetDateTime.parse("2026-01-01T12:00+01:00"), OffsetDateTime.parse("2026-02-06T12:00+01:00")).getBody())
            .hasSize(5 + 6);
        assertThat(
            trainController.listTrainChanges(PartnerToken.SBB, OffsetDateTime.parse("2026-01-01T13:00+01:00"), OffsetDateTime.parse("2026-02-06T11:00+01:00")).getBody())
            .hasSize(4 + 5);
    }

    @Test
    void should_list_all_according_to_sharing() {
        UUID trainId1 = UUID.randomUUID();
        UUID trainId2 = UUID.randomUUID();
        createMinimalTrain(trainId1, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, "SOB_Branch");
        generateChangelog(trainId1, "SBB", MAIN_BRANCH_NAME, 4, OffsetDateTime.parse("2026-01-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SBB", MAIN_BRANCH_NAME, 5, OffsetDateTime.parse("2026-02-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SOB", "SOB_Branch", 3, OffsetDateTime.parse("2026-02-01T13:00+01:00"), Duration.ofDays(1));
        trainSpringDataRepository.addPartner(trainId1, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SOB");

        assertThat(
            trainController.listTrainChanges(PartnerToken.SBB, OffsetDateTime.MIN, OffsetDateTime.MAX).getBody())
            .hasSize(5 + 10);
        assertThat(
            trainController.listTrainChanges(PartnerToken.SOB, OffsetDateTime.MIN, OffsetDateTime.MAX).getBody())
            .hasSize(10);
    }

    @Test
    void should_list_train() {
        UUID trainId1 = UUID.randomUUID();
        UUID trainId2 = UUID.randomUUID();
        createMinimalTrain(trainId1, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, MAIN_BRANCH_NAME);
        generateChangelog(trainId1, "SBB", MAIN_BRANCH_NAME, 4, OffsetDateTime.parse("2026-01-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SBB", MAIN_BRANCH_NAME, 5, OffsetDateTime.parse("2026-01-01T13:00+01:00"), Duration.ofDays(1));
        trainSpringDataRepository.addPartner(trainId1, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SBB");

        assertThat(trainController.getTrainChangesById(PartnerToken.SBB, trainId1, null, null).getBody()).hasSize(5);
        assertThat(trainController.getTrainChangesById(PartnerToken.SBB, trainId2, null, null).getBody()).hasSize(6);
    }

    @Test
    void should_list_train_according_to_interval() {
        UUID trainId1 = UUID.randomUUID();
        UUID trainId2 = UUID.randomUUID();
        createMinimalTrain(trainId1, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, MAIN_BRANCH_NAME);
        generateChangelog(trainId1, "SBB", MAIN_BRANCH_NAME, 4, OffsetDateTime.parse("2026-01-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SBB", MAIN_BRANCH_NAME, 5, OffsetDateTime.parse("2026-01-01T13:00+01:00"), Duration.ofDays(1));
        trainSpringDataRepository.addPartner(trainId1, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SBB");

        assertThat(
            trainController.getTrainChangesById(PartnerToken.SBB, trainId1, OffsetDateTime.parse("2026-01-01T12:00+01:00"), OffsetDateTime.parse("2026-01-05T12:00+01:00")).getBody())
            .hasSize(5);
        assertThat(
            trainController.getTrainChangesById(PartnerToken.SBB, trainId1, OffsetDateTime.parse("2026-01-01T13:00+01:00"), OffsetDateTime.parse("2026-01-05T11:00+01:00")).getBody())
            .hasSize(3);
    }

    @Test
    void should_list_train_according_to_sharing() {
        UUID trainId1 = UUID.randomUUID();
        UUID trainId2 = UUID.randomUUID();
        createMinimalTrain(trainId1, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, MAIN_BRANCH_NAME);
        createMinimalTrain(trainId2, "SOB_Branch");
        generateChangelog(trainId1, "SBB", MAIN_BRANCH_NAME, 4, OffsetDateTime.parse("2026-01-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SBB", MAIN_BRANCH_NAME, 5, OffsetDateTime.parse("2026-02-01T12:00+01:00"), Duration.ofDays(1));
        generateChangelog(trainId2, "SOB", "SOB_Branch", 3, OffsetDateTime.parse("2026-02-01T13:00+01:00"), Duration.ofDays(1));
        trainSpringDataRepository.addPartner(trainId1, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SBB");
        trainSpringDataRepository.addPartner(trainId2, "SOB");

        assertThat(trainController.getTrainChangesById(PartnerToken.SBB, trainId1, null, null).getBody()).hasSize(5);
        assertThat(trainController.getTrainChangesById(PartnerToken.SBB, trainId2, null, null).getBody()).hasSize(10);
        assertThat(trainController.getTrainChangesById(PartnerToken.SOB, trainId1, null, null).getBody()).hasSize(0);
        assertThat(trainController.getTrainChangesById(PartnerToken.SOB, trainId2, null, null).getBody()).hasSize(10);
    }

    @Test
    void should_not_get_changelog_for_nonexistent_train() {
        UUID nonExistentTrainId = UUID.randomUUID();

        var exception = assertThrows(NoSuchElementException.class,
            () -> trainController.getTrainChangesById(PartnerToken.SBB, nonExistentTrainId, null, null));
        assertThat(exception.getMessage()).isEqualTo("Train not found");
    }

    private void createMinimalTrain(UUID trainId, String branchName) {
        // Create a minimal train version entry to satisfy the train existence check
        UUID branchId = UUID.nameUUIDFromBytes(branchName.getBytes(StandardCharsets.UTF_8));

        // Create the branch first to satisfy the foreign key constraint
        if (branchSpringDataRepository.findById(branchId).isEmpty()) {
            branchSpringDataRepository.insert(Branch.builder()
                .branchId(branchId)
                .trainId(trainId)
                .name(branchName)
                .owner("SBB")
                .state(ch.rail.r2p.api.dto.BranchState.OPEN)
                .build());
        }

        // Create minimal Train payload with required fields
        String trainNumber = "TEST-" + trainId.toString().substring(0, 8);
        LocalDate operationalDay = LocalDate.of(2026, 1, 1);

        Train payload = new Train(
            trainNumber,  // id
            java.util.List.of(),  // references
            java.util.List.of(),  // links
            "Test Train", // name
            operationalDay,  // startdate
            OffsetDateTime.now(),  // timestamp
            new Journey(java.util.List.of(
                new Journeylocation(),  // minimal first location
                new Journeylocation()   // minimal second location (min 2 required)
            ))
        );

        TrainVersion trainVersion = TrainVersion.builder()
            .version(UUID.randomUUID())
            .trainId(trainId)
            .trainNumber(trainNumber)
            .operationalDay(operationalDay)
            .branchId(branchId)
            .comment("Test train")
            .timestamp(OffsetDateTime.now())
            .payload(payload)
            .head(true)
            .build();
        trainSpringDataRepository.insert(trainVersion);
    }

    private void generateChangelog(UUID trainId, String partner, String branchName, int updates, OffsetDateTime startTime, Duration interval) {
        // Create the branch first to satisfy the foreign key constraint
        UUID branchId = UUID.nameUUIDFromBytes(branchName.getBytes(StandardCharsets.UTF_8));

        // Only create branch if it doesn't exist yet
        if (branchSpringDataRepository.findById(branchId).isEmpty()) {
            branchSpringDataRepository.insert(Branch.builder()
                .branchId(branchId)
                .trainId(trainId)
                .name(branchName)
                .owner(partner)
                .state(ch.rail.r2p.api.dto.BranchState.OPEN)
                .build());
        }

        OffsetDateTime time = startTime;
        changelogSpringDataRepository.insert(new Changelog(UUID.randomUUID(), Action.CREATED, partner, null, trainId, branchId, branchName, UUID.randomUUID(), time,
            "Create %s/%s".formatted(trainId, branchName)));
        for (int i = 0; i < updates; i++) {
            time = time.plus(interval);
            changelogSpringDataRepository.insert(
                new Changelog(UUID.randomUUID(), Action.UPDATED, partner, null, trainId, branchId, branchName, UUID.randomUUID(), time,
                    "Update %s/%s".formatted(trainId, branchName)));
        }
    }

}
