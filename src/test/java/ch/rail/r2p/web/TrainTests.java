package ch.rail.r2p.web;

import static ch.rail.r2p.Actor.PREFIXED_MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.rail.r2p.Actor;
import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.persistence.TrainPersistence;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Objects;
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
    private Actor actor;

    @MockitoSpyBean
    private TrainPersistence trainPersistence;

    @BeforeEach
    void setUp() {
        actor.setup();
    }

    @AfterEach
    void tearDown() {
        actor.cleanup();
    }

    @Test
    void should_create_train_for_instance_owner() {
        Train sampleTrain = actor.loadSampleTrain();
        var train = trainController.createTrain(SBB, "new train", sampleTrain);
        assertThat(train.getBody()).isNotNull();
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(1);
        assertThat(result.getBody().getBranches().getFirst().getBusiness().getTrainNumber()).isEqualTo(sampleTrain.getId());
    }

    @Test
    void should_not_create_train_for_non_instance_owner() {
        assertThrows(AuthorizationDeniedException.class, () -> trainController.createTrain(SOB, "new train", actor.loadSampleTrain()));
    }

    @Test
    void should_list_trains_according_to_operating_day() {
        trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
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
    void should_throw_exception_when_to_date_is_before_from_date() {
        LocalDate from = LocalDate.parse("2027-01-01");
        LocalDate to = LocalDate.parse("2024-01-01");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> trainController.listTrains(SBB, from, to));

        assertThat(exception.getMessage()).isEqualTo("Parameter 'to' must be after 'from'");
    }

    @Test
    void should_get_train_by_id() {
        String comment = "Train creation comment";
        var train = trainController.createTrain(SBB, comment, actor.loadSampleTrain());
        assert train.getBody() != null;
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assert result.getBody() != null;
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
        String prefixedBranchName = Actor.getPrefixedBranchName(SOB, branchName);
        // truncating timestamps to millis to avoid precision issues when comparing in memory timestamps with database timestamps, which might have different precision
        OffsetDateTime startTime = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        TrainListElement train = trainController.createTrain(SBB, "initial creation", actor.loadSampleTrain()).getBody();
        assert train != null;
        UUID trainId = train.getTrainId();
        UUID mainBranchId = train.getBranches().getFirst().getBranchId();
        trainController.shareTrain(SBB, trainId, "SOB");
        trainController.updateTrainBranch(SBB, trainId, mainBranchId, "update main branch", actor.loadSampleTrain());
        UUID branchId = Objects.requireNonNull(trainController.createTrainBranch(SOB, trainId, branchName, "create branch", actor.loadSampleTrain()).getBody()).getBranchId();
        trainController.updateTrainBranch(SOB, trainId, branchId, "update branch", actor.loadSampleTrain());
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
        trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        var result = trainController.listTrains(SBB, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void should_not_list_not_shared_trains() {
        trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        var result = trainController.listTrains(BLS, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isEmpty();
    }

    @Test
    void should_not_get_not_shared_train_by_id() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assertThrows(AuthorizationDeniedException.class, () -> {
            assert train.getBody() != null;
            trainController.getTrainById(SOB, train.getBody().getTrainId());
        });
    }

    @Test
    void should_get_train_branch_by_lead_ru() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
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
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
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
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        UUID trainId = train.getTrainId();
        UUID branchId = train.getBranches().getFirst().getBranchId();

        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainBranch(SOB, trainId, branchId));
    }

    @Test
    void should_get_train_version_by_lead_ru() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        UUID versionId = train.getBranches().getFirst().getTechnical().getVersion();

        var result = trainController.getTrainVersion(SBB, versionId);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(actor.loadSampleTrain().getId());
    }

    @Test
    void should_get_train_version_by_shared_partner() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        UUID versionId = train.getBranches().getFirst().getTechnical().getVersion();
        trainController.shareTrain(SBB, train.getTrainId(), "SOB");

        var result = trainController.getTrainVersion(SOB, versionId);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(actor.loadSampleTrain().getId());
    }

    @Test
    void should_get_historical_train_version() {
        var train = trainController.createTrain(SBB, "initial version", actor.loadSampleTrain()).getBody();
        assert train != null;
        UUID trainId = train.getTrainId();
        UUID branchId = train.getBranches().getFirst().getBranchId();
        UUID firstVersionId = train.getBranches().getFirst().getTechnical().getVersion();

        trainController.updateTrainBranch(SBB, trainId, branchId, "second version", actor.loadSampleTrain());
        trainController.updateTrainBranch(SBB, trainId, branchId, "third version", actor.loadSampleTrain());

        var firstVersionResult = trainController.getTrainVersion(SBB, firstVersionId);
        assertThat(firstVersionResult).isNotNull();
        assertThat(firstVersionResult.getBody()).isNotNull();
    }

    @Test
    void should_not_get_train_version_if_not_shared() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        UUID versionId = train.getBranches().getFirst().getTechnical().getVersion();

        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainVersion(SOB, versionId));
    }

    @Test
    void should_not_get_nonexistent_train_version() {
        assertThrows(NoSuchElementException.class, () -> trainController.getTrainVersion(SBB, UUID.randomUUID()));
    }
}
