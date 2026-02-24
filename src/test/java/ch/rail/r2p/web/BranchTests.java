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
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authorization.AuthorizationDeniedException;

@SpringBootTest
class BranchTests {
    @Autowired
    private TrainController trainController;

    @Autowired
    private Actor actor;

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
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), "myBranch");
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assert result.getBody() != null;
        assertThat(result.getBody().getBranches()).hasSize(2);
    }

    @Test
    void should_not_create_branch_with_same_name() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName);
        assertThrows(DuplicateKeyException.class, () -> trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName));
        assertThrows(DuplicateKeyException.class, () -> trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName));
    }

    @Test
    void should_delete_branch() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        var branch = trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName);
        assert branch.getBody() != null;
        trainController.deleteTrainBranch(SOB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch");
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assert result.getBody() != null;
        assertThat(result.getBody().getBranches()).hasSize(2);
        var deletedBranch = result.getBody().getBranches().stream().filter(b -> b.getBranchName().equals(Actor.getPrefixedBranchName(SOB, branchName))).findAny();
        assertThat(deletedBranch).isPresent();
        assertThat(deletedBranch.get().getTechnical().getBranchState()).isEqualTo(BranchState.DELETED);
    }

    @Test
    void should_not_delete_branch_of_other_partner() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        var branch = trainController.createTrainBranch(SOB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName);
        assertThrows(AuthorizationDeniedException.class, () -> {
            assert branch.getBody() != null;
            trainController.deleteTrainBranch(BLS, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch");
        });
        assertThrows(AuthorizationDeniedException.class, () -> {
            assert branch.getBody() != null;
            trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch");
        });
    }

    @Test
    void should_delete_all_branches_when_deleting_main_branch() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName);
        trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "delete main branch");
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assert result.getBody() != null;
        assertThat(result.getBody().getBranches().stream().map(Branch::getBranchName)).containsExactlyInAnyOrder(PREFIXED_MAIN_BRANCH_NAME, Actor.getPrefixedBranchName(SBB, branchName));
        assertThat(result.getBody().getBranches()).allMatch(b -> b.getTechnical().getBranchState() == BranchState.DELETED, "branch is deleted");
    }

    @Test
    void should_not_update_deleted_branch() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        var branch = trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName);
        assert branch.getBody() != null;
        trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "delete branch");
        assertThrows(IllegalStateException.class, () -> trainController.updateTrainBranch(SBB, train.getBody().getTrainId(), branch.getBody().getBranchId(), "update after delete", actor.loadSampleTrain()));
    }

    @Test
    void should_not_create_branch_when_main_branch_was_deleted() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "delete main branch");
        assertThrows(IllegalStateException.class, () -> trainController.createTrainBranch(SBB, train.getBody().getTrainId(), "test", actor.loadSampleTrain(), branchName));
    }

    @Test
    void should_update_main_branch() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.updateTrainBranch(SBB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "test", actor.loadSampleTrain());
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assert result.getBody() != null;
        assertThat(result.getBody().getBranches()).hasSize(1);
        assertThat(result.getBody().getBranches().getFirst().getTechnical().getVersion())
            .isNotEqualTo(train.getBody().getBranches().getFirst().getTechnical().getVersion());
    }

    @Test
    void should_not_update_main_branch_by_other_partner() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain());
        assert train.getBody() != null;
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.updateTrainBranch(SOB, train.getBody().getTrainId(), train.getBody().getBranches().getFirst().getBranchId(), "test", actor.loadSampleTrain()));
    }

    @Test
    void should_not_get_nonexistent_train_branch() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        UUID trainId = train.getTrainId();

        assertThrows(NoSuchElementException.class, () -> trainController.getTrainBranch(SBB, trainId, UUID.randomUUID()));
    }
}
