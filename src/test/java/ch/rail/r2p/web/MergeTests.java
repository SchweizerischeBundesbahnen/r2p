package ch.rail.r2p.web;

import static ch.rail.r2p.Actor.PREFIXED_MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.rail.r2p.Actor;
import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.persistence.TrainPersistence;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class MergeTests {

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
    void should_request_merge() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        var result = trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTechnical().getBranchState()).isEqualTo(BranchState.MERGE_REQUESTED);
    }

    @Test
    void should_record_merge_request_in_changelog() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        var changes = trainController.getTrainChangesById(SBB, train.getTrainId(), null, null).getBody();
        assertThat(changes).isNotNull();
        assertThat(changes.stream().map(Change::getAction)).contains(Action.MERGE_REQUESTED);
    }

    @Test
    void should_not_request_merge_for_other_partners_branch() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.requestMerge(BLS, train.getTrainId(), branch.getBranchId(), "please merge"));
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.requestMerge(SBB, train.getTrainId(), branch.getBranchId(), "please merge"));
    }

    @Test
    void should_not_request_merge_on_already_requested_branch() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        assertThrows(InvalidBranchStateException.class,
            () -> trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge again"));
    }

    @Test
    void should_merge_branch_into_main() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        UUID reviewedVersion = branch.getTechnical().getVersion();
        var result = trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merging");

        assertThat(result.getBody()).isNotNull();
        var mainBranch = result.getBody().getBranches().stream()
            .filter(b -> b.getBranchName().equals(PREFIXED_MAIN_BRANCH_NAME)).findFirst().orElseThrow();
        assertThat(mainBranch.getTechnical().getBranchState()).isEqualTo(BranchState.OPEN);
        assertThat(mainBranch.getTechnical().getVersion()).isNotEqualTo(train.getBranches().getFirst().getTechnical().getVersion());
        var mergedBranch = result.getBody().getBranches().stream()
            .filter(b -> !b.getBranchName().equals(PREFIXED_MAIN_BRANCH_NAME)).findFirst().orElseThrow();
        assertThat(mergedBranch.getTechnical().getBranchState()).isEqualTo(BranchState.MERGED);
    }

    @Test
    void should_merge_without_prior_merge_request_as_lead_ru() {
        // The Lead RU can merge any branch directly, without a prior merge request from the branch owner
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        UUID reviewedVersion = branch.getTechnical().getVersion();
        var result = trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "direct merge");

        assertThat(result.getBody()).isNotNull();
        var mergedBranch = result.getBody().getBranches().stream()
            .filter(b -> !b.getBranchName().equals(PREFIXED_MAIN_BRANCH_NAME)).findFirst().orElseThrow();
        assertThat(mergedBranch.getTechnical().getBranchState()).isEqualTo(BranchState.MERGED);
    }

    @Test
    void should_not_merge_without_prior_merge_request_as_non_lead_ru() {
        // A non-Lead-RU partner cannot call merge at all – only the Lead RU can
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        UUID reviewedVersion = branch.getTechnical().getVersion();
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.merge(SOB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "SOB tries to merge directly"));
    }

    @Test
    void should_record_merge_in_changelog() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        UUID reviewedVersion = branch.getTechnical().getVersion();
        trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merging");

        var changes = trainController.getTrainChangesById(SBB, train.getTrainId(), null, null).getBody();
        assertThat(changes).isNotNull();
        List<Action> actions = changes.stream().map(Change::getAction).toList();
        assertThat(actions).contains(Action.MERGED);
        assertThat(actions).contains(Action.DELETED);
    }

    @Test
    void should_not_merge_when_version_mismatch() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        UUID staleVersion = UUID.randomUUID(); // wrong version
        assertThrows(MergeConflictException.class,
            () -> trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), staleVersion, "merge"));
    }

    @Test
    void should_not_merge_by_non_lead_ru() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        UUID reviewedVersion = branch.getTechnical().getVersion();
        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.merge(SOB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merge"));
    }

    @Test
    void should_not_merge_already_merged_branch() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        UUID reviewedVersion = branch.getTechnical().getVersion();
        trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "first merge");

        assertThrows(InvalidBranchStateException.class,
            () -> trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "second merge"));
    }

    @Test
    void should_reject_merge_request() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        var result = trainController.rejectMerge(SBB, train.getTrainId(), branch.getBranchId(), "not ready");

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTechnical().getBranchState()).isEqualTo(BranchState.OPEN);
    }

    @Test
    void should_record_merge_rejection_in_changelog() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");
        trainController.rejectMerge(SBB, train.getTrainId(), branch.getBranchId(), "not ready");

        var changes = trainController.getTrainChangesById(SBB, train.getTrainId(), null, null).getBody();
        assertThat(changes).isNotNull();
        assertThat(changes.stream().map(Change::getAction)).contains(Action.MERGE_REJECTED);
    }

    @Test
    void should_allow_update_after_merge_rejection() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");
        trainController.rejectMerge(SBB, train.getTrainId(), branch.getBranchId(), "not ready");

        // After rejection the branch should be OPEN again and updatable
        var updated = trainController.updateTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "fix applied", actor.loadSampleTrain());
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().getTechnical().getBranchState()).isEqualTo(BranchState.OPEN);
    }

    @Test
    void should_not_reject_merge_when_no_merge_request_open() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        assertThrows(InvalidBranchStateException.class,
            () -> trainController.rejectMerge(SBB, train.getTrainId(), branch.getBranchId(), "no request to reject"));
    }

    @Test
    void should_not_reject_merge_by_non_lead_ru() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.rejectMerge(SOB, train.getTrainId(), branch.getBranchId(), "not ready"));
    }

    @Test
    void should_implicitly_reject_merge_request_when_branch_is_deleted() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        // Branch owner deletes the branch while merge request is open
        trainController.deleteTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "aborting");

        var changes = trainController.getTrainChangesById(SBB, train.getTrainId(), null, null).getBody();
        assertThat(changes).isNotNull();
        List<Action> actions = changes.stream().map(Change::getAction).toList();
        // A MERGE_REJECTED entry must appear before the DELETED entry
        assertThat(actions).contains(Action.MERGE_REJECTED);
        assertThat(actions).contains(Action.DELETED);
        int rejectedIdx = actions.indexOf(Action.MERGE_REJECTED);
        int deletedIdx  = actions.lastIndexOf(Action.DELETED);
        assertThat(rejectedIdx).isLessThan(deletedIdx);
    }

    @Test
    void should_not_update_merged_branch() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        UUID reviewedVersion = branch.getTechnical().getVersion();
        trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merging");

        assertThrows(InvalidBranchStateException.class,
            () -> trainController.updateTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "update on merged", actor.loadSampleTrain()));
    }

    @Test
    void should_not_request_merge_on_merged_branch() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        UUID reviewedVersion = branch.getTechnical().getVersion();
        trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merging");

        assertThrows(InvalidBranchStateException.class,
            () -> trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "merge already done"));
    }

    @Test
    void should_not_delete_merged_branch() {
        var train = actor.createTrain(SBB, "new train");
        actor.shareTrain(SBB, train.getTrainId(), SOB);
        var branch = actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");
        UUID reviewedVersion = branch.getTechnical().getVersion();
        trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merging");

        assertThrows(InvalidBranchStateException.class,
            () -> trainController.deleteTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "delete merged"));
    }

    @Test
    void should_not_request_merge_on_main_branch() {
        var train = actor.createTrain(SBB, "new train");
        var mainBranch = train.getBranches().stream()
            .filter(b -> Boolean.TRUE.equals(b.getTechnical().getMainBranch()))
            .findFirst().orElseThrow();

        assertThrows(IllegalArgumentException.class,
            () -> trainController.requestMerge(SBB, train.getTrainId(), mainBranch.getBranchId(), "merge main"));
    }

    @Test
    void should_not_merge_main_branch() {
        var train = actor.createTrain(SBB, "new train");
        var mainBranch = train.getBranches().stream()
            .filter(b -> Boolean.TRUE.equals(b.getTechnical().getMainBranch()))
            .findFirst().orElseThrow();
        UUID version = mainBranch.getTechnical().getVersion();

        assertThrows(IllegalArgumentException.class,
            () -> trainController.merge(SBB, train.getTrainId(), mainBranch.getBranchId(), version, "merge main"));
    }
}

