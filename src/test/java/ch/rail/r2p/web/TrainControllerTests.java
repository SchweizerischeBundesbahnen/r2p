package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.TrainPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashSet;
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
class TrainControllerTests {

    @Autowired
    private TrainController trainController;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private TrainPersistence trainPersistence;

    private Train sampleTrain;

    Set<UUID> trainIdsForCleanup = new HashSet<>();

    Train loadSampleTrain() {
        if (sampleTrain == null) {
            try {
                sampleTrain = objectMapper.readValue(
                    this.getClass().getResourceAsStream("/openapi/sampleTrain.json"), Train.class);
            } catch (Exception e) {
                return null;
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
        trainIdsForCleanup.clear();
    }

    @Test
    void should_create_train_for_instance_owner() {

        var result = trainController.createTrain(SBB, "new train", loadSampleTrain());
        assertThat(result).isNotNull();
    }

    @Test
    void should_not_create_train_for_other_partner() {
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
    void should_not_list_unshared_trains() {
        trainController.createTrain(SBB, "new train", loadSampleTrain());
        var result = trainController.listTrains(BLS, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getBody()).isEmpty();
    }

    @Test
    void should_list_trains_according_to_operationg_day() {
        trainController.createTrain(SBB, "new train", loadSampleTrain());
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2024-01-01"), LocalDate.parse("2027-01-01")).getBody()).hasSize(1);
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2024-01-01"), null).getBody()).hasSize(1);
        assertThat(trainController.listTrains(SBB, null, LocalDate.parse("2027-01-01")).getBody()).hasSize(1);
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2000-01-01"), LocalDate.parse("2000-01-01")).getBody()).isEmpty();
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2100-01-01"), LocalDate.parse("2101-01-01")).getBody()).isEmpty();
        assertThat(trainController.listTrains(SBB, null, LocalDate.parse("2000-01-01")).getBody()).isEmpty();
        assertThat(trainController.listTrains(SBB, LocalDate.parse("2100-01-01"), null).getBody()).isEmpty();
    }

    @Test
    void should_get_train_by_id() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
    }

    @Test
    void should_not_get_inexisting_train_by_id() {
        assertThrows(AuthorizationDeniedException.class, () -> trainController.getTrainById(SOB, UUID.randomUUID()));
    }

    @Test
    void should_not_get_unshared_train_by_id() {
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
        trainController.updateTrainBranch(SOB, train.getBody().getTrainId(), "myBranch", "test", loadSampleTrain());
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
    }

    @Test
    void should_delete_branch() {
        String branchName = "myBranch";
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.shareTrain(SBB, train.getBody().getTrainId(), "SOB");
        trainController.updateTrainBranch(SBB, train.getBody().getTrainId(), branchName, "test", loadSampleTrain());
        trainController.deleteTrainBranch(SBB, train.getBody().getTrainId(), branchName, "delete branch");
        var result = trainController.getTrainById(SBB, train.getBody().getTrainId());
        assertThat(result).isNotNull();
        assertThat(result.getBody().getBranches()).hasSize(2);
        var branch = result.getBody().getBranches().stream().filter(b -> b.getBranchName().equals(branchName)).findAny();
        assertThat(branch).isPresent();
        assertThat(branch.get().getTechnical().getBranchState()).isEqualTo(BranchState.DELETED);
    }

    @Test
    void should_update_main_branch() {
        var train = trainController.createTrain(SBB, "new train", loadSampleTrain());
        trainController.updateTrainBranch(SBB, train.getBody().getTrainId(), "main", "test", loadSampleTrain());
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
        assertThrows(AuthorizationDeniedException.class, () -> trainController.updateTrainBranch(SOB, train.getBody().getTrainId(), "main", "test", loadSampleTrain()));
    }
}
