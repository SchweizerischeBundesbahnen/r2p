package ch.rail.r2p.web;

import static ch.rail.r2p.domain.Branch.MAIN_BRANCH_NAME;
import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.ChangelogPersistence;
import ch.rail.r2p.persistence.TrainPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
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
class SharingTests {

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
    void should_share_train() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        var result = trainController.getTrainById(SOB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
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
    void should_write_changelog_for_unsharing() {
        String branchName = "myBranch";
        String prefixedBranchName = getPrefixedBranchName(SOB, branchName);
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
        assertThat(sobChangelog.stream().map(Change::getAffectedPartner)).containsExactly("SOB", "SOB");

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
