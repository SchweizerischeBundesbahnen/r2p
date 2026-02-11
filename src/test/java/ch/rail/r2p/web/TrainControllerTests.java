package ch.rail.r2p.web;

import static ch.rail.r2p.Constants.MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.ChangelogPersistence;
import ch.rail.r2p.persistence.TrainPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class TrainControllerTests {

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

    private final static String PREFIXED_MAIN_BRANCH_NAME = "%s-%s".formatted(SBB, MAIN_BRANCH_NAME);

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
    void should_not_get_not_shared_train_by_id() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainById(SOB, train.getBody().getTrainId()));
    }

    @Test
    void should_share_train() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        var result = trainController.getTrainById(SOB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
    }

    @Test
    void should_create_branch() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", loadSampleTrain(), "myBranch");
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
    }

    @Test
    void should_not_create_branch_with_same_name() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName);
        assertThrows(DuplicateKeyException.class, () -> trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName));
        assertThrows(DuplicateKeyException.class, () -> trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName));
    }

    @Test
    void should_delete_branch() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        var branch = trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName);
        trainController.deleteTrainBranch(SOB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch");
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
        var deletedBranch = result.getBody().getBranches().stream().filter(b -> b.getBranchName().equals("SOB-" + branchName)).findAny();
        assertThat(deletedBranch).isPresent();
        assertThat(deletedBranch.get().getTechnical().getBranchState()).isEqualTo(BranchState.DELETED);
    }

    @Test
    void should_not_delete_branch_of_other_partner() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        var branch = trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName);
        assertThrows(AuthorizationDeniedException.class, () -> trainController.deleteTrainBranch(BLS, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch"));
        assertThrows(AuthorizationDeniedException.class, () -> trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch"));
    }

    @Test
    void should_delete_all_branches_when_deleting_main_branch() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName);
        trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "delete main branch");
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches().stream().map(Branch::getBranchName)).containsExactlyInAnyOrder(PREFIXED_MAIN_BRANCH_NAME, "SBB-" + branchName);
        assertThat(result.getBody().getBranches()).allMatch(b -> b.getTechnical().getBranchState() == BranchState.DELETED, "branch is deleted");
    }

    @Test
    void should_not_update_deleted_branch() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        var branch = trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName);
        trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch");
        assertThrows(IllegalStateException.class, () -> trainController.updateTrainBranch(SBB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "update after delete", loadSampleTrain()));
    }

    @Test
    void should_not_create_branch_when_main_branch_was_deleted() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "delete main branch");
        assertThrows(IllegalStateException.class, () -> trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", loadSampleTrain(), branchName));
    }

    @Test
    void should_update_main_branch() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.updateTrainBranch(SBB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "test", loadSampleTrain());
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(1);
        assertThat(result.getBody().getBranches().getFirst().getTechnical().getVersion())
            .isNotEqualTo(train.getBody().getBranches().getFirst().getTechnical().getVersion());
    }

    @Test
    void should_unshare_train() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        var sharedTrains = trainController.listTrains(SOB, null, null);
        assertThat(sharedTrains).isNotNull();
        assertThat(sharedTrains.getBody()).hasSize(1);

        trainController.unshareTrain(SBB, train.getBody().getTrainId(), "SOB");

        var result = trainController.listTrains(SOB, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isEmpty();
        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainById(SOB, train.getBody().getTrainId()));
    }

    @Test
    void should_delete_owned_branches_when_unsharing_train() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", loadSampleTrain(), "myBranch");

        trainController.unshareTrain(SBB, train.getBody().getTrainId(), "SOB");

        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
        var sbbBranch = result.getBody().getBranches().stream().filter(b -> b.getTechnical().getOwner().equals("SBB")).findAny().orElseThrow();
        var sobBranch = result.getBody().getBranches().stream().filter(b -> b.getTechnical().getOwner().equals("SOB")).findAny().orElseThrow();
        assertThat(sbbBranch.getTechnical().getBranchState()).isEqualTo(BranchState.OPEN);
        assertThat(sobBranch.getTechnical().getBranchState()).isEqualTo(BranchState.DELETED);
    }

    @Test
    void should_not_allow_unsharing_for_other_partners() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        trainController.shareTrain(SOB, train.getBody().getTrainId(), "BLS");

        assertThrows(AuthorizationDeniedException.class, () -> trainController.unshareTrain(SOB, train.getBody().getTrainId(), "BLS"));
    }

    @Test
    void should_not_update_main_branch_by_other_partner() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.updateTrainBranch(SOB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "test", loadSampleTrain()));
    }

    @Test
    void should_write_changelog() {
        String branchName = "myBranch";
        String prefixedBranchName = "SOB-myBranch";
        OffsetDateTime startTime = OffsetDateTime.now();
        TrainListElement train = trainController.createTrain(SBB, "initial creation", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();
        UUID mainBranchId = train.getBranches().getFirst().getBranchId();
        trainController.shareTrain(SBB, trainId, "SOB");
        trainController.updateTrainBranch(SBB, trainId, mainBranchId, "update main branch", loadSampleTrain());
        UUID branchId = trainController.createTrainBranch(SOB, trainId, "create branch", loadSampleTrain(), branchName).getBody().getBranchId();
        trainController.updateTrainBranch(SOB, trainId, branchId, "update branch", loadSampleTrain());
        trainController.deleteTrainBranch(SOB, trainId, branchId, "delete branch");
        OffsetDateTime endTime = OffsetDateTime.now();
        var result = trainController.getTrainChangesById(SBB, trainId, null, null).getBody();
        assertThat(result).hasSize(6);
        assertThat(result.stream().map(Change::getTimestamp)).allMatch(timestamp -> timestamp.isAfter(startTime) && timestamp.isBefore(endTime));
        assertThat(result.stream().map(Change::getTrainId)).allMatch(trainId::equals);
        assertThat(result.stream().map(Change::getBranchName)).containsExactly(PREFIXED_MAIN_BRANCH_NAME, null, PREFIXED_MAIN_BRANCH_NAME, prefixedBranchName, prefixedBranchName,
            prefixedBranchName);
        assertThat(result.stream().map(Change::getAction)).containsExactly(Action.CREATED, Action.SHARED, Action.UPDATED, Action.CREATED, Action.UPDATED, Action.DELETED);
        assertThat(result.stream().map(Change::getPartner)).containsExactly("SBB", "SBB", "SBB", "SOB", "SOB", "SOB");
        assertThat(result.stream().map(Change::getComment)).containsExactly("initial creation", "Train shared", "update main branch", "create branch", "update branch", "delete branch");
    }

    @Test
    void should_write_changelog_for_unsharing() {
        String branchName = "myBranch";
        String prefixedBranchName = "SOB-myBranch";
        TrainListElement train = trainController.createTrain(SBB, "initial creation", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();
        UUID mainBranchId = train.getBranches().getFirst().getBranchId();
        trainController.shareTrain(SBB, trainId, "SOB");
        trainController.updateTrainBranch(SBB, trainId, mainBranchId, "update main branch", loadSampleTrain());
        UUID branchId = trainController.createTrainBranch(SOB, trainId, "create branch", loadSampleTrain(), branchName).getBody().getBranchId();
        trainController.updateTrainBranch(SOB, trainId, branchId, "update branch", loadSampleTrain());
        trainController.unshareTrain(SBB, trainId, "SOB");

        var sobChangelog = trainController.getTrainChangesById(SOB, trainId, null, null).getBody();
        assertThat(sobChangelog).hasSize(2);
        assertThat(sobChangelog.stream().map(Change::getTrainId)).allMatch(trainId::equals);
        assertThat(sobChangelog.stream().map(Change::getAction)).containsExactly(Action.SHARED, Action.UNSHARED);
        assertThat(sobChangelog.stream().map(Change::getConcernedPartner)).containsExactly("SOB", "SOB");

        var sbbChangelog = trainController.getTrainChangesById(SBB, trainId, null, null).getBody();
        assertThat(sbbChangelog).hasSize(7);
        assertThat(sbbChangelog.stream().map(Change::getTrainId)).allMatch(trainId::equals);
        assertThat(sbbChangelog.stream().map(Change::getBranchName)).containsExactly(PREFIXED_MAIN_BRANCH_NAME, null, PREFIXED_MAIN_BRANCH_NAME, prefixedBranchName, prefixedBranchName, null,
            prefixedBranchName);
        assertThat(sbbChangelog.stream().map(Change::getAction)).containsExactly(Action.CREATED, Action.SHARED, Action.UPDATED, Action.CREATED, Action.UPDATED, Action.UNSHARED, Action.DELETED);
        assertThat(sbbChangelog.stream().map(Change::getPartner)).containsExactly("SBB", "SBB", "SBB", "SOB", "SOB", "SBB", "SBB");
        assertThat(sbbChangelog.stream().map(Change::getComment)).containsExactly("initial creation", "Train shared", "update main branch", "create branch", "update branch", "Train unshared",
            "Branch deleted due to unsharing of train");
    }
}
