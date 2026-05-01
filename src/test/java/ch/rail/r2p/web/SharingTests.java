package ch.rail.r2p.web;

import static ch.rail.r2p.Actor.PREFIXED_MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.rail.r2p.Actor;
import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.persistence.TrainPersistence;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class SharingTests {

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
    void should_share_train() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        var result = trainController.getTrainById(SOB, train.getTrainId());
        assertThat(result).isNotNull();
    }

    @Test
    void should_unshare_train() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        var sharedTrains = trainController.listTrains(SOB, null, null);
        assertThat(sharedTrains.getBody()).hasSize(1);

        trainController.unshareTrain(SBB, train.getTrainId(), "SOB", "revoking access");

        assertThat(trainController.listTrains(SOB, null, null).getBody()).isEmpty();
        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainById(SOB, train.getTrainId()));
    }

    @Test
    void should_delete_owned_branches_when_unsharing_train() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        trainController.unshareTrain(SBB, train.getTrainId(), "SOB", "revoking access");

        var result = trainController.getTrainById(SBB, train.getTrainId());
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
        var sbbBranch = result.getBody().getBranches().stream()
            .filter(b -> "SBB".equals(b.getTechnical().getOwner())).findAny().orElseThrow();
        var sobBranch = result.getBody().getBranches().stream()
            .filter(b -> "SOB".equals(b.getTechnical().getOwner())).findAny().orElseThrow();
        assertThat(sbbBranch.getTechnical().getBranchState()).isEqualTo(BranchState.OPEN);
        assertThat(sobBranch.getTechnical().getBranchState()).isEqualTo(BranchState.DELETED);
    }

    @Test
    void should_not_allow_unsharing_for_other_partners() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        trainController.shareTrain(SOB, train.getTrainId(), "BLS", "sharing with BLS");

        assertThrows(AuthorizationDeniedException.class, () -> trainController.unshareTrain(SOB, train.getTrainId(), "BLS", "not allowed"));
    }

    @Test
    void should_not_allow_unsharing_partner_that_was_never_shared() {
        TrainListElement train = actor.createTrain(SBB, "new train");

        var exception = assertThrows(AuthorizationDeniedException.class,
            () -> trainController.unshareTrain(SBB, train.getTrainId(), "SOB", "revoking"));
        assertThat(exception.getMessage()).contains("Train is not shared with partner 'SOB'");
    }

    @Test
    void should_write_changelog_for_unsharing() {
        String branchName = "myBranch";
        String prefixedBranchName = Actor.getPrefixedBranchName(SOB, branchName);
        TrainListElement train = actor.createTrain(SBB, "initial creation");
        UUID trainId = train.getTrainId();
        UUID mainBranchId = train.getBranches().getFirst().getBranchId();
        actor.shareTrain(SBB, trainId, SOB, "Train shared");
        trainController.updateTrainBranch(SBB, trainId, mainBranchId, "update main branch", actor.loadSampleTrain());
        UUID branchId = actor.createBranch(SOB, trainId, branchName, "create branch").getBranchId();
        trainController.updateTrainBranch(SOB, trainId, branchId, "update branch", actor.loadSampleTrain());
        trainController.unshareTrain(SBB, trainId, "SOB", "Train unshared");

        var sobChangelog = trainController.getTrainChangesById(SOB, trainId, null, null).getBody();
        assertThat(sobChangelog).hasSize(2);
        assertThat(sobChangelog.stream().map(Change::getTrainId)).allMatch(trainId::equals);
        assertThat(sobChangelog.stream().map(Change::getAction)).containsExactly(Action.SHARED, Action.UNSHARED);
        assertThat(sobChangelog.stream().map(Change::getAffectedPartner)).containsExactly("SOB", "SOB");

        var sbbChangelog = trainController.getTrainChangesById(SBB, trainId, null, null).getBody();
        assertThat(sbbChangelog).hasSize(7);
        assertThat(sbbChangelog.stream().map(Change::getTrainId)).allMatch(trainId::equals);
        assertThat(sbbChangelog.stream().map(Change::getBranchName)).containsExactly(PREFIXED_MAIN_BRANCH_NAME, null, PREFIXED_MAIN_BRANCH_NAME, prefixedBranchName, prefixedBranchName, null, prefixedBranchName);
        assertThat(sbbChangelog.stream().map(Change::getAction)).containsExactly(Action.CREATED, Action.SHARED, Action.UPDATED, Action.CREATED, Action.UPDATED, Action.UNSHARED, Action.DELETED);
        assertThat(sbbChangelog.stream().map(Change::getPartner)).containsExactly("SBB", "SBB", "SBB", "SOB", "SOB", "SBB", "SBB");
        assertThat(sbbChangelog.stream().map(Change::getComment)).containsExactly("initial creation", "Train shared", "update main branch", "create branch", "update branch", "Train unshared", "Branch deleted due to unsharing of train");
    }

    @Test
    void should_not_share_nonexistent_train() {
        UUID nonExistentTrainId = UUID.randomUUID();

        var exception = assertThrows(NoSuchElementException.class,
            () -> trainController.shareTrain(SBB, nonExistentTrainId, "SOB", "sharing"));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_not_unshare_nonexistent_train() {
        UUID nonExistentTrainId = UUID.randomUUID();

        var exception = assertThrows(NoSuchElementException.class,
            () -> trainController.unshareTrain(SBB, nonExistentTrainId, "SOB", "unsharing"));
        assertThat(exception.getMessage()).isEqualTo("Train or Branch not found");
    }

    @Test
    void should_implicitly_reject_merge_request_when_unsharing_train() {
        TrainListElement train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        // Unsharing should implicitly reject the open merge request before deleting the branch
        trainController.unshareTrain(SBB, train.getTrainId(), SOB.toString(), "revoking access");

        var changes = trainController.getTrainChangesById(SBB, train.getTrainId(), null, null).getBody();
        assertThat(changes).isNotNull();
        List<Action> actions = changes.stream().map(Change::getAction).toList();
        assertThat(actions).contains(Action.MERGE_REJECTED);
        assertThat(actions).contains(Action.DELETED);
        int rejectedIdx = actions.indexOf(Action.MERGE_REJECTED);
        int deletedIdx  = actions.lastIndexOf(Action.DELETED);
        assertThat(rejectedIdx).isLessThan(deletedIdx);
    }
}

