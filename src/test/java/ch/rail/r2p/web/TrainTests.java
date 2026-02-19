package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.domain.Branch.MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.ChangelogPersistence;
import ch.rail.r2p.persistence.TrainPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class TrainTests {

    @Autowired
    private TrainController trainController;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private TrainPersistence trainPersistence;

    @Autowired
    private ChangelogPersistence changelogPersistence;

    private Train sampleTrain;

    Set<UUID> trainIdsForCleanup = new HashSet<>();

    private static final String PREFIXED_MAIN_BRANCH_NAME = getPrefixedBranchName(SBB, MAIN_BRANCH_NAME);

    Train loadSampleTrain() {
        if (sampleTrain == null) {
            try {
                sampleTrain = objectMapper.readValue(
                    this.getClass().getResourceAsStream("/openapi/sampleTrain.json"), Train.class);
                Objects.requireNonNull(sampleTrain);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load sample train", e);
            }
        }
        return sampleTrain;
    }

    static String getPrefixedBranchName(PartnerToken owner, String branchName) {
        return "%s-%s".formatted(owner.name(), branchName);
    }

    @BeforeEach
    void setUp() {
        // Collect all trainIds of trains inserted by trainPersistence.insertVersion
        doAnswer(invocation -> {
            invocation.callRealMethod();
            trainIdsForCleanup.add(((TrainVersion) invocation.getArgument(0)).getTrainId());
            return null;
        }).when(trainPersistence).insertVersion(any());
    }

    @AfterEach
    void tearDown() {
        trainPersistence.deleteTrains(trainIdsForCleanup);
        changelogPersistence.deleteByTrainId(trainIdsForCleanup);
        trainIdsForCleanup.clear();
    }

    @Test
    void should_create_train_for_instance_owner() {
        Train sampleTrain = loadSampleTrain();
        var train = trainController.createTrain(SBB, "new train", sampleTrain);
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(1);
        assertThat(result.getBody().getBranches().getFirst().getBusiness().getTrainNumber()).isEqualTo(sampleTrain.getId());
    }

    @Test
    void should_not_create_train_for_non_instance_owner() {
        assertThrows(AuthorizationDeniedException.class, () -> trainController.createTrain(SOB, "new train", loadSampleTrain()));
    }

    @Test
    void should_list_trains_according_to_operating_day() {
        trainController.createTrain(SBB, "new train", loadSampleTrain());
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2024-01-01"), LocalDate.parse("2027-01-01")).getBody()).hasSize(1);
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2024-01-01"), null).getBody()).hasSize(1);
        assertThat(trainController.listTrains(SBB, null, LocalDate.parse("2027-01-01")).getBody()).hasSize(1);
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2000-01-01"), LocalDate.parse("2000-01-01")).getBody()).isEmpty();
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2100-01-01"), LocalDate.parse("2101-01-01")).getBody()).isEmpty();
        assertThat(trainController.listTrains(SBB, null, LocalDate.parse("2000-01-01")).getBody()).isEmpty();
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2100-01-01"), null).getBody()).isEmpty();
        assertThat(trainController.listTrains(SBB, null, null).getBody()).isNotEmpty();
    }

    @Test
    void should_get_train_by_id() {
        String comment = "Train creation comment";
        var train = trainController.createTrain(SBB, comment, loadSampleTrain());
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(1);
        assertThat(result.getBody().getBranches().getFirst().getBranchName()).isEqualTo(PREFIXED_MAIN_BRANCH_NAME);
        assertThat(result.getBody().getBranches().getFirst().getTechnical().getComment()).isEqualTo(comment);
    }

    @Test
    void should_not_get_nonexistent_train_by_id() {
        assertThrows(NoSuchElementException.class, () -> trainController.getTrainById(SOB, UUID.randomUUID()));
    }

    @Test
    void should_write_changelog() {
        String branchName = "myBranch";
        String prefixedBranchName = getPrefixedBranchName(SOB, branchName);
        // truncating timestamps to millis to avoid precision issues when comparing in memory timestamps with database timestamps, which might have different precision
        OffsetDateTime startTime = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        TrainListElement train = trainController.createTrain(SBB, "initial creation", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();
        UUID mainBranchId = train.getBranches().getFirst().getBranchId();
        trainController.shareTrain(SBB, trainId, "SOB");
        trainController.updateTrainBranch(SBB, trainId, mainBranchId, "update main branch", loadSampleTrain());
        UUID branchId = trainController.createTrainBranch(SOB, trainId, "create branch", loadSampleTrain(), branchName).getBody().getBranchId();
        trainController.updateTrainBranch(SOB, trainId, branchId, "update branch", loadSampleTrain());
        trainController.deleteTrainBranch(SOB, trainId, branchId, "delete branch");
        OffsetDateTime endTime = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        var result = trainController.getTrainChangesById(SBB, trainId, null, null).getBody();
        assertThat(result).hasSize(6);
        assertThat(result.stream().map(c -> c.getTimestamp().truncatedTo(ChronoUnit.MILLIS)))
            .as("all timestamps should be within startTime %s and endTime %s".formatted(startTime, endTime))
            .allMatch(timestamp -> !timestamp.isBefore(startTime) && !timestamp.isAfter(endTime));
        assertThat(result.stream().map(Change::getTrainId)).allMatch(trainId::equals);
        assertThat(result.stream().map(Change::getBranchName)).containsExactly(PREFIXED_MAIN_BRANCH_NAME, null, PREFIXED_MAIN_BRANCH_NAME, prefixedBranchName, prefixedBranchName,
            prefixedBranchName);
        assertThat(result.stream().map(Change::getAction)).containsExactly(Action.CREATED, Action.SHARED, Action.UPDATED, Action.CREATED, Action.UPDATED, Action.DELETED);
        assertThat(result.stream().map(Change::getPartner)).containsExactly("SBB", "SBB", "SBB", "SOB", "SOB", "SOB");
        assertThat(result.stream().map(Change::getComment)).containsExactly("initial creation", "Train shared", "update main branch", "create branch", "update branch", "delete branch");
    }

    @Test
    void should_list_shared_trains() {
        trainController.createTrain(SBB, "new train", loadSampleTrain());
        var result = trainController.listTrains(SBB, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void should_not_list_not_shared_trains() {
        trainController.createTrain(SBB, "new train", loadSampleTrain());
        var result = trainController.listTrains(BLS, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isEmpty();
    }

    @Test
    void should_not_get_not_shared_train_by_id() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainById(SOB, train.getBody().getTrainId()));
    }

    @Test
    void should_get_train_branch_by_lead_ru() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();
        UUID branchId = train.getBranches().getFirst().getBranchId();

        var result = trainController.getTrainBranch(SBB, trainId, branchId);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranchName()).isEqualTo(PREFIXED_MAIN_BRANCH_NAME);
        assertThat(result.getBody().getTechnical().getOwner()).isEqualTo("SBB");
        assertThat(result.getBody().getTechnical().getComment()).isEqualTo("new train");
    }

    @Test
    void should_get_train_branch_by_shared_partner() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();
        UUID branchId = train.getBranches().getFirst().getBranchId();
        trainController.shareTrain(SBB, trainId, "SOB");

        var result = trainController.getTrainBranch(SOB, trainId, branchId);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranchName()).isEqualTo(PREFIXED_MAIN_BRANCH_NAME);
        assertThat(result.getBody().getTechnical().getOwner()).isEqualTo("SBB");
    }

    @Test
    void should_not_get_train_branch_if_not_shared() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();
        UUID branchId = train.getBranches().getFirst().getBranchId();

        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainBranch(SOB, trainId, branchId));
    }

    @Test
    void should_get_train_version_by_lead_ru() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain()).getBody();
        UUID versionId = train.getBranches().getFirst().getTechnical().getVersion();

        var result = trainController.getTrainVersion(SBB, versionId);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(loadSampleTrain().getId());
    }

    @Test
    void should_get_train_version_by_shared_partner() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain()).getBody();
        UUID versionId = train.getBranches().getFirst().getTechnical().getVersion();
        trainController.shareTrain(SBB, train.getTrainId(), "SOB");

        var result = trainController.getTrainVersion(SOB, versionId);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(loadSampleTrain().getId());
    }

    @Test
    void should_get_historical_train_version() {
        var train = trainController.createTrain(SBB, "initial version", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();
        UUID branchId = train.getBranches().getFirst().getBranchId();
        UUID firstVersionId = train.getBranches().getFirst().getTechnical().getVersion();

        trainController.updateTrainBranch(SBB, trainId, branchId, "second version", loadSampleTrain());
        trainController.updateTrainBranch(SBB, trainId, branchId, "third version", loadSampleTrain());

        var firstVersionResult = trainController.getTrainVersion(SBB, firstVersionId);
        assertThat(firstVersionResult).isNotNull();
        assertThat(firstVersionResult.getBody()).isNotNull();
    }

    @Test
    void should_not_get_train_version_if_not_shared() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain()).getBody();
        UUID versionId = train.getBranches().getFirst().getTechnical().getVersion();

        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainVersion(SOB, versionId));
    }

    @Test
    void should_not_get_nonexistent_train_version() {
        assertThrows(NoSuchElementException.class, () -> trainController.getTrainVersion(SBB, UUID.randomUUID()));
    }
}
