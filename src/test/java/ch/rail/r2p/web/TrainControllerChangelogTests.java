package ch.rail.r2p.web;

import static ch.rail.r2p.Constants.MAIN_BRANCH_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.persistence.impl.ChangelogSpringDataRepository;
import ch.rail.r2p.persistence.impl.TrainSpringDataRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TrainControllerChangelogTests {

    // This class tests querying the changelog.
    // In order to have test data with defined timestamps, changelogs are direclty inserted via the SpringData Repository.

    @Autowired
    private TrainController trainController;

    @Autowired
    private ChangelogSpringDataRepository changelogSpringDataRepository;
    @Autowired
    private TrainSpringDataRepository trainSpringDataRepository;

    @AfterEach
    void tearDown() {
        changelogSpringDataRepository.deleteAll();
    }

    @Test
    void should_list_all() {
        UUID trainId1 = UUID.randomUUID();
        UUID trainId2 = UUID.randomUUID();
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

    private void generateChangelog(UUID trainId, String partner, String branchName, int updates, OffsetDateTime startTime, Duration interval) {
        OffsetDateTime time = startTime;
        changelogSpringDataRepository.insert(new Changelog(UUID.randomUUID(), Action.CREATED, partner, null, trainId, UUID.nameUUIDFromBytes(branchName.getBytes()), branchName, UUID.randomUUID(), time,
            "Create %s/%s".formatted(trainId, branchName)));
        for (int i = 0; i < updates; i++) {
            time = time.plus(interval);
            changelogSpringDataRepository.insert(
                new Changelog(UUID.randomUUID(), Action.UPDATED, partner, null, trainId, UUID.nameUUIDFromBytes(branchName.getBytes()), branchName, UUID.randomUUID(), time,
                    "Update %s/%s".formatted(trainId, branchName)));
        }
    }

}
