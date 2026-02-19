package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static ch.rail.r2p.domain.Branch.MAIN_BRANCH_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.ChangelogPersistence;
import ch.rail.r2p.persistence.TrainPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class BranchTests {

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
        var deletedBranch = result.getBody().getBranches().stream().filter(b -> b.getBranchName().equals(getPrefixedBranchName(SOB, branchName))).findAny();
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
        assertThat(result.getBody().getBranches().stream().map(Branch::getBranchName)).containsExactlyInAnyOrder(PREFIXED_MAIN_BRANCH_NAME, getPrefixedBranchName(SBB, branchName));
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
    void should_not_update_main_branch_by_other_partner() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.updateTrainBranch(SOB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "test", loadSampleTrain()));
    }

    @Test
    void should_not_get_nonexistent_train_branch() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();

        assertThrows(NoSuchElementException.class, () -> trainController.getTrainBranch(SBB, trainId, UUID.randomUUID()));
    }
}
