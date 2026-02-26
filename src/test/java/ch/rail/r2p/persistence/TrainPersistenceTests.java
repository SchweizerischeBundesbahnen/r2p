package ch.rail.r2p.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.Branch;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.impl.BranchSpringDataRepository;
import ch.rail.r2p.persistence.impl.ChangelogSpringDataRepository;
import ch.rail.r2p.persistence.impl.TrainSpringDataRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrainPersistenceTests {

    @Autowired
    private TrainPersistence trainPersistence;

    @Autowired
    private TrainSpringDataRepository trainSpringDataRepository;

    @Autowired
    private BranchSpringDataRepository branchSpringDataRepository;

    @Autowired
    private ChangelogSpringDataRepository changelogSpringDataRepository;

    @AfterEach
    void tearDown() {
        changelogSpringDataRepository.deleteAll();
        trainSpringDataRepository.deleteAll();
        branchSpringDataRepository.deleteAll();
    }

    @Test
    void should_delete_train_completely_with_all_associated_data() {
        // Given a train with multiple branches, partners and changelog entries
        UUID trainId = UUID.randomUUID();
        UUID mainBranchId = UUID.randomUUID();
        UUID secondBranchId = UUID.randomUUID();
        UUID mainVersionId = UUID.randomUUID();
        UUID secondVersionId = UUID.randomUUID();

        createTrainVersionAndBranch(trainId, mainBranchId, mainVersionId);
        createTrainVersionAndBranch(trainId, secondBranchId, secondVersionId);

        trainSpringDataRepository.addPartner(trainId, "SBB");
        trainSpringDataRepository.addPartner(trainId, "SOB");
        trainSpringDataRepository.addPartner(trainId, "BLS");

        createChangelogEntry(trainId, "CREATED", "SBB");
        createChangelogEntry(trainId, "UPDATED", "SBB");
        createChangelogEntry(trainId, "SHARED", "SOB");

        assertThat(trainSpringDataRepository.existsById(mainVersionId)).isTrue();
        assertThat(trainSpringDataRepository.existsById(secondVersionId)).isTrue();
        assertThat(branchSpringDataRepository.findTrainBranchByTrainId(trainId)).hasSize(2);
        assertThat(trainSpringDataRepository.getPartners(trainId)).hasSize(3);
        assertThat(changelogSpringDataRepository.findByTrainId(trainId)).hasSize(3);
        assertThat(trainPersistence.existsByTrainId(trainId)).isTrue();

        // When deleting the train
        trainPersistence.deleteTrains(List.of(trainId));

        // Then ensure all train versions, branches, sharing and changelogs are deleted
        assertThat(trainSpringDataRepository.existsById(mainVersionId)).isFalse();
        assertThat(trainSpringDataRepository.existsById(secondVersionId)).isFalse();
        assertThat(trainPersistence.existsByTrainId(trainId)).isFalse();

        assertThat(branchSpringDataRepository.findTrainBranchByTrainId(trainId)).isEmpty();
        assertThat(branchSpringDataRepository.findById(mainBranchId)).isEmpty();
        assertThat(branchSpringDataRepository.findById(secondBranchId)).isEmpty();

        assertThat(trainSpringDataRepository.getPartners(trainId)).isEmpty();

        assertThat(changelogSpringDataRepository.findByTrainId(trainId)).isEmpty();
    }


    private void createTrainVersionAndBranch(UUID trainId, UUID branchId, UUID versionId) {
        String uniqueBranchName = "branch-" + branchId;

        Branch branch = new Branch(branchId, trainId, uniqueBranchName, "SBB", ch.rail.r2p.api.dto.BranchState.OPEN);
        branchSpringDataRepository.insert(branch);

        Train samplePayload = new Train();
        samplePayload.setId("TEST-TRAIN-" + branchId.toString().substring(0, 4));
        samplePayload.setStartdate(LocalDate.now());

        TrainVersion trainVersion = new TrainVersion(versionId, trainId, samplePayload.getId(), LocalDate.now(), branchId, "Test train", OffsetDateTime.now(), samplePayload, true);
        trainSpringDataRepository.insert(trainVersion);
    }

    private void createChangelogEntry(UUID trainId, String action, String partner) {
        Changelog changelog = Changelog.builder()
            .id(UUID.randomUUID())
            .action(ch.rail.r2p.api.dto.Action.valueOf(action))
            .partner(partner)
            .trainId(trainId)
            .timestamp(OffsetDateTime.now())
            .comment("Test changelog entry")
            .build();
        changelogSpringDataRepository.insert(changelog);
    }
}






