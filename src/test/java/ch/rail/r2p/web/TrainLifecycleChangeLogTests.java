package ch.rail.r2p.web;

import static ch.rail.r2p.Actor.PREFIXED_MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.Actor;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.persistence.TrainPersistence;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class TrainLifecycleChangeLogTests {

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
    void should_record_full_lifecycle_in_changelog() {
        // --- 1. SBB creates the train ---
        var train = actor.createTrain(SBB, "Organize traffic for train RE 12 from Aarau to Zurich");
        UUID trainId = train.getTrainId();

        // --- 2. SBB shares the train with BLS ---
        actor.shareTrain(SBB, trainId, BLS, "Sharing with BLS for rollmaterial review");

        // --- 3. BLS creates a branch ---
        String branchName = "rollmaterial-proposal";
        String prefixedBranchName = Actor.getPrefixedBranchName(BLS, branchName);
        var branch = actor.createBranch(BLS, trainId, branchName, "Adding rollmaterial details for BLS traction unit");
        UUID branchId = branch.getBranchId();

        // --- 4. BLS updates the branch content ---
        trainController.updateTrainBranch(BLS, trainId, branchId,
            "Rollmaterial RABe 515 not compatible with track section Olten-Aarau", actor.loadSampleTrain());

        // --- 5. BLS creates a merge request ---
        trainController.requestMerge(BLS, trainId, branchId,
            "New proposal for rollmaterial, please review and merge");

        // --- 6. SBB rejects the merge request ---
        trainController.rejectMerge(SBB, trainId, branchId,
            "Traction unit capacity insufficient for peak hours, please revise");

        // --- 7. BLS updates the branch content again ---
        trainController.updateTrainBranch(BLS, trainId, branchId,
            "Updated rollmaterial to RABe 523 with sufficient capacity", actor.loadSampleTrain());

        // --- 8. BLS creates a second merge request ---
        trainController.requestMerge(BLS, trainId, branchId,
            "Revised rollmaterial proposal, capacity issue resolved");

        // --- 9. SBB accepts (merges) the merge request ---
        var blsBranchAfterSecondRequest = trainController.getTrainBranch(SBB, trainId, branchId).getBody();
        assertThat(blsBranchAfterSecondRequest).isNotNull();
        UUID reviewedVersion = blsBranchAfterSecondRequest.getTechnical().getVersion();
        trainController.merge(SBB, trainId, branchId, reviewedVersion,
            "Rollmaterial confirmed, merging BLS proposal into main plan");

        // --- Retrieve full changelog ---
        List<Change> allChanges = trainController.getTrainChangesById(SBB, trainId, null, null).getBody();
        assertThat(allChanges).isNotNull();

        // --- Assert train-level entries (no branch: SHARED / UNSHARED) ---
        List<String> trainLevelLog = allChanges.stream()
            .filter(c -> c.getBranchName() == null)
            .map(c -> "[%s] - %s - [%s] - %s".formatted(c.getPartner(), c.getAction(), c.getAffectedPartner(), c.getComment()))
            .toList();

        assertThat(String.join("\n", trainLevelLog)).isEqualTo("""
            [SBB] - shared - [BLS] - Sharing with BLS for rollmaterial review""");

        // --- Assert changelog for the main branch (SBB-main) ---
        List<String> mainBranchLog = allChanges.stream()
            .filter(c -> PREFIXED_MAIN_BRANCH_NAME.equals(c.getBranchName()))
            .map(c -> "[%s] - %s - %s".formatted(c.getPartner(), c.getAction(), c.getComment()))
            .toList();

        assertThat(String.join("\n", mainBranchLog)).isEqualTo("""
            [SBB] - created - Organize traffic for train RE 12 from Aarau to Zurich
            [SBB] - updated - Rollmaterial confirmed, merging BLS proposal into main plan""");

        // --- Assert changelog for the BLS branch (BLS-rollmaterial-proposal) ---
        List<String> blsBranchLog = allChanges.stream()
            .filter(c -> prefixedBranchName.equals(c.getBranchName()))
            .map(c -> "[%s] - %s - %s".formatted(c.getPartner(), c.getAction(), c.getComment()))
            .toList();

        assertThat(String.join("\n", blsBranchLog)).isEqualTo("""
            [BLS] - created - Adding rollmaterial details for BLS traction unit
            [BLS] - updated - Rollmaterial RABe 515 not compatible with track section Olten-Aarau
            [BLS] - merge_requested - New proposal for rollmaterial, please review and merge
            [SBB] - merge_rejected - Traction unit capacity insufficient for peak hours, please revise
            [BLS] - updated - Updated rollmaterial to RABe 523 with sufficient capacity
            [BLS] - merge_requested - Revised rollmaterial proposal, capacity issue resolved
            [SBB] - merged - Rollmaterial confirmed, merging BLS proposal into main plan""");
    }
}



