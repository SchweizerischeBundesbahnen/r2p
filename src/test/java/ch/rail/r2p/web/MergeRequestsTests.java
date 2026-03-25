package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.rail.r2p.Actor;
import ch.rail.r2p.api.dto.Action;
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
class MergeRequestsTests {

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

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void should_return_empty_list_when_no_merge_related_entries_exist() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_single_merge_request() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getBranchId()).isEqualTo(branch.getBranchId());
        assertThat(result.getFirst().getAction()).isEqualTo(Action.MERGE_REQUESTED);
    }

    @Test
    void should_return_multiple_merge_requests() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        trainController.shareTrain(SBB, train.getTrainId(), BLS.name());

        var branch1 = trainController.createTrainBranch(SOB, train.getTrainId(), "branch1", "test", actor.loadSampleTrain()).getBody();
        assert branch1 != null;
        var branch2 = trainController.createTrainBranch(BLS, train.getTrainId(), "branch2", "test", actor.loadSampleTrain()).getBody();
        assert branch2 != null;
        trainController.requestMerge(SOB, train.getTrainId(), branch1.getBranchId(), "please merge branch1");
        trainController.requestMerge(BLS, train.getTrainId(), branch2.getBranchId(), "please merge branch2");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        List<UUID> returnedBranchIds = result.stream().map(Change::getBranchId).toList();
        assertThat(returnedBranchIds).containsExactlyInAnyOrder(branch1.getBranchId(), branch2.getBranchId());
    }

    @Test
    void should_not_include_open_branches_without_merge_history() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());

        // Create a branch but do NOT request a merge
        trainController.createTrainBranch(SOB, train.getTrainId(), "openBranch", "test", actor.loadSampleTrain());

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void should_show_merged_branch_with_merged_action() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        UUID reviewedVersion = branch.getTechnical().getVersion();
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");
        trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merging");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getBranchId()).isEqualTo(branch.getBranchId());
        assertThat(result.getFirst().getAction()).isEqualTo(Action.MERGED);
    }

    @Test
    void should_show_rejected_branch_with_merge_rejected_action() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");
        trainController.rejectMerge(SBB, train.getTrainId(), branch.getBranchId(), "not ready");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getBranchId()).isEqualTo(branch.getBranchId());
        assertThat(result.getFirst().getAction()).isEqualTo(Action.MERGE_REJECTED);
    }

    @Test
    void should_not_include_deleted_branch_without_merge_history() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        // Delete without ever requesting a merge
        trainController.deleteTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "deleting");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void should_filter_by_state_merge_requested() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        trainController.shareTrain(SBB, train.getTrainId(), BLS.name());

        var branch1 = trainController.createTrainBranch(SOB, train.getTrainId(), "branch1", "test", actor.loadSampleTrain()).getBody();
        assert branch1 != null;
        var branch2 = trainController.createTrainBranch(BLS, train.getTrainId(), "branch2", "test", actor.loadSampleTrain()).getBody();
        assert branch2 != null;
        trainController.requestMerge(SOB, train.getTrainId(), branch1.getBranchId(), "please merge branch1");
        trainController.requestMerge(BLS, train.getTrainId(), branch2.getBranchId(), "please merge branch2");
        // Reject branch2
        trainController.rejectMerge(SBB, train.getTrainId(), branch2.getBranchId(), "not ready");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), Action.MERGE_REQUESTED).getBody();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getBranchId()).isEqualTo(branch1.getBranchId());
        assertThat(result.getFirst().getAction()).isEqualTo(Action.MERGE_REQUESTED);
    }

    @Test
    void should_filter_by_state_merge_rejected() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");
        trainController.rejectMerge(SBB, train.getTrainId(), branch.getBranchId(), "not ready");

        var resultRejected = trainController.listMergeRequests(SBB, train.getTrainId(), Action.MERGE_REJECTED).getBody();
        var resultRequested = trainController.listMergeRequests(SBB, train.getTrainId(), Action.MERGE_REQUESTED).getBody();

        assertThat(resultRejected).isNotNull().hasSize(1);
        assertThat(resultRejected.getFirst().getAction()).isEqualTo(Action.MERGE_REJECTED);
        assertThat(resultRequested).isNotNull().isEmpty();
    }

    @Test
    void should_filter_by_state_merged() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        UUID reviewedVersion = branch.getTechnical().getVersion();
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");
        trainController.merge(SBB, train.getTrainId(), branch.getBranchId(), reviewedVersion, "merging");

        var resultMerged = trainController.listMergeRequests(SBB, train.getTrainId(), Action.MERGED).getBody();
        var resultRequested = trainController.listMergeRequests(SBB, train.getTrainId(), Action.MERGE_REQUESTED).getBody();

        assertThat(resultMerged).isNotNull().hasSize(1);
        assertThat(resultMerged.getFirst().getAction()).isEqualTo(Action.MERGED);
        assertThat(resultRequested).isNotNull().isEmpty();
    }

    @Test
    void should_return_latest_entry_per_branch_only_once() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        // Request, reject, request again
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "first request");
        trainController.rejectMerge(SBB, train.getTrainId(), branch.getBranchId(), "not ready");
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "second request");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        // Only the latest entry for this branch should appear
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.getFirst().getBranchId()).isEqualTo(branch.getBranchId());
        assertThat(result.getFirst().getAction()).isEqualTo(Action.MERGE_REQUESTED);
        assertThat(result.getFirst().getComment()).isEqualTo("second request");
    }

    @Test
    void should_return_merge_requests_to_shared_partner() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        // SOB is a shared partner and should also be able to list the merge requests
        var result = trainController.listMergeRequests(SOB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    void should_return_correct_change_details_in_merge_request() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        trainController.shareTrain(SBB, train.getTrainId(), SOB.name());
        var branch = trainController.createTrainBranch(SOB, train.getTrainId(), "myBranch", "test", actor.loadSampleTrain()).getBody();
        assert branch != null;
        trainController.requestMerge(SOB, train.getTrainId(), branch.getBranchId(), "please merge");

        var result = trainController.listMergeRequests(SBB, train.getTrainId(), null).getBody();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        Change mr = result.getFirst();
        assertThat(mr.getBranchId()).isEqualTo(branch.getBranchId());
        assertThat(mr.getPartner()).isEqualTo(SOB.name());
        assertThat(mr.getAction()).isEqualTo(Action.MERGE_REQUESTED);
        assertThat(mr.getVersion()).isNotNull();
        assertThat(mr.getTimestamp()).isNotNull();
        assertThat(mr.getComment()).isEqualTo("please merge");
    }

    // ── Access control ────────────────────────────────────────────────────────

    @Test
    void should_deny_access_for_non_shared_partner() {
        var train = trainController.createTrain(SBB, "new train", actor.loadSampleTrain()).getBody();
        assert train != null;
        // BLS is not shared with this train

        assertThrows(AuthorizationDeniedException.class,
            () -> trainController.listMergeRequests(BLS, train.getTrainId(), null));
    }

    @Test
    void should_throw_not_found_for_unknown_train() {
        UUID unknownTrainId = UUID.randomUUID();

        assertThrows(java.util.NoSuchElementException.class,
            () -> trainController.listMergeRequests(SBB, unknownTrainId, null));
    }
}

