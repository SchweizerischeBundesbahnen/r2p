package ch.rail.r2p.web;

import static ch.rail.r2p.Actor.PREFIXED_MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.rail.r2p.Actor;
import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.persistence.TrainPersistence;
import ch.rail.r2p.persistence.impl.BranchSpringDataRepository;
import java.util.NoSuchElementException;
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
    private Actor actor;

    @MockitoSpyBean
    private TrainPersistence trainPersistence;

    @Autowired
    private BranchSpringDataRepository branchSpringDataRepository;

    @BeforeEach
    void setUp() {
        actor.setup();
    }

    @AfterEach
    void tearDown() {
        actor.cleanup();
    }

    @Test
    void should_create_branch() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        var result = trainController.getTrainById(SBB, train.getTrainId());
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
    }

    @Test
    void should_flag_main_branch_correctly() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        var result = trainController.getTrainById(SBB, train.getTrainId());
        assertThat(result.getBody()).isNotNull();
        var branches = result.getBody().getBranches();
        assertThat(branches).hasSize(2);
        assertThat(branches.stream().filter(b -> b.getBranchName().equals(PREFIXED_MAIN_BRANCH_NAME)))
            .allMatch(b -> Boolean.TRUE.equals(b.getTechnical().getMainBranch()), "main branch has mainBranch=true");
        assertThat(branches.stream().filter(b -> !b.getBranchName().equals(PREFIXED_MAIN_BRANCH_NAME)))
            .allMatch(b -> Boolean.FALSE.equals(b.getTechnical().getMainBranch()), "non-main branches have mainBranch=false");
    }

    @Test
    void should_not_create_branch_with_same_name() {
        String branchName = "myBranch";
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        actor.createBranch(SOB, train.getTrainId(), branchName, "test");
        assertThrows(DuplicateKeyException.class, () -> trainController.createTrainBranch(SOB, train.getTrainId(), branchName, "test", actor.loadSampleTrain()));
        assertThrows(DuplicateKeyException.class, () -> trainController.createTrainBranch(SBB, train.getTrainId(), branchName, "test", actor.loadSampleTrain()));
    }

    @Test
    void should_delete_branch() {
        String branchName = "myBranch";
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        Branch branch = actor.createBranch(SOB, train.getTrainId(), branchName, "test");
        trainController.deleteTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "delete branch");
        var result = trainController.getTrainById(SBB, train.getTrainId());
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
        var deletedBranch = result.getBody().getBranches().stream().filter(b -> b.getBranchName().equals(Actor.getPrefixedBranchName(SOB, branchName))).findAny();
        assertThat(deletedBranch).isPresent();
        assertThat(deletedBranch.get().getTechnical().getBranchState()).isEqualTo(BranchState.DELETED);
    }

    @Test
    void should_not_delete_branch_of_other_partner() {
        String branchName = "myBranch";
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        Branch branch = actor.createBranch(SOB, train.getTrainId(), branchName, "test");
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.deleteTrainBranch(BLS, train.getTrainId(), branch.getBranchId(), "delete branch"));
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.deleteTrainBranch(SBB, train.getTrainId(), branch.getBranchId(), "delete branch"));
    }

    @Test
    void should_delete_all_branches_when_deleting_main_branch() {
        String branchName = "myBranch";
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.createBranch(SBB, train.getTrainId(), branchName, "test");
        trainController.deleteTrainBranch(SBB, train.getTrainId(), train.getBranches().getFirst().getBranchId(), "delete main branch");

        var branches = branchSpringDataRepository.findTrainBranchByTrainId(train.getTrainId());
        assertThat(branches).hasSize(2);
        assertThat(branches).allMatch(b -> b.getState() == BranchState.DELETED, "all branches should be DELETED");

        assertThrows(NoSuchElementException.class,
            () -> trainController.getTrainById(SBB, train.getTrainId()));
    }

    @Test
    void should_not_update_deleted_branch() {
        String branchName = "myBranch";
        TrainListElement train = actor.createTrain(SBB, "new train");
        Branch branch = actor.createBranch(SBB, train.getTrainId(), branchName, "test");
        trainController.deleteTrainBranch(SBB, train.getTrainId(), branch.getBranchId(), "delete branch");
        assertThrows(InvalidBranchStateException.class, () -> trainController.updateTrainBranch(SBB, train.getTrainId(), branch.getBranchId(), "update after delete", actor.loadSampleTrain()));
    }

    @Test
    void should_not_create_branch_when_main_branch_was_deleted() {
        String branchName = "myBranch";
        TrainListElement train = actor.createTrain(SBB, "new train");
        trainController.deleteTrainBranch(SBB, train.getTrainId(), train.getBranches().getFirst().getBranchId(), "delete main branch");
        assertThrows(InvalidBranchStateException.class, () -> trainController.createTrainBranch(SBB, train.getTrainId(), branchName, "test", actor.loadSampleTrain()));
    }

    @Test
    void should_update_main_branch() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        trainController.updateTrainBranch(SBB, train.getTrainId(), train.getBranches().getFirst().getBranchId(), "test", actor.loadSampleTrain());
        var result = trainController.getTrainById(SBB, train.getTrainId());
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(1);
        assertThat(result.getBody().getBranches().getFirst().getTechnical().getVersion())
            .isNotEqualTo(train.getBranches().getFirst().getTechnical().getVersion());
    }

    @Test
    void should_not_update_main_branch_by_other_partner() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.updateTrainBranch(SOB, train.getTrainId(), train.getBranches().getFirst().getBranchId(), "test", actor.loadSampleTrain()));
    }

    @Test
    void should_not_get_nonexistent_train_branch() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        UUID nonExistentBranchId = UUID.randomUUID();

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> trainController.getTrainBranch(SBB, train.getTrainId(), nonExistentBranchId));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_not_get_train_branch_with_nonexistent_train_id() {
        UUID nonExistentTrainId = UUID.randomUUID();
        UUID nonExistentBranchId = UUID.randomUUID();

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> trainController.getTrainBranch(SBB, nonExistentTrainId, nonExistentBranchId));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_not_delete_nonexistent_train_branch() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        UUID nonExistentBranchId = UUID.randomUUID();

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> trainController.deleteTrainBranch(SBB, train.getTrainId(), nonExistentBranchId, "delete non-existent branch"));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_not_delete_branch_with_nonexistent_train_id() {
        UUID nonExistentTrainId = UUID.randomUUID();
        UUID nonExistentBranchId = UUID.randomUUID();

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> trainController.deleteTrainBranch(SBB, nonExistentTrainId, nonExistentBranchId, "delete non-existent train"));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_not_create_branch_for_nonexistent_train() {
        UUID nonExistentTrainId = UUID.randomUUID();

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> trainController.createTrainBranch(SBB, nonExistentTrainId, "myBranch", "test", actor.loadSampleTrain()));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_not_update_nonexistent_train_branch() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        UUID nonExistentBranchId = UUID.randomUUID();

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> trainController.updateTrainBranch(SBB, train.getTrainId(), nonExistentBranchId, "update non-existent branch", actor.loadSampleTrain()));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_not_update_branch_with_nonexistent_train_id() {
        UUID nonExistentTrainId = UUID.randomUUID();
        UUID nonExistentBranchId = UUID.randomUUID();

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> trainController.updateTrainBranch(SBB, nonExistentTrainId, nonExistentBranchId, "update non-existent train", actor.loadSampleTrain()));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }
}
