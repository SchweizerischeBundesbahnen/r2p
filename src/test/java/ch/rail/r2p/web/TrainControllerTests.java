package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrainControllerTests {

    @Autowired
    private TrainController trainController;

    @Test
    void testListTrain() {
        var result = trainController.listTrains(SBB, null, null);
        assertNotNull(result);
    }

    @Test
    void testGetTrainById() {
        var result = trainController.getTrainById(SBB, UUID.randomUUID());
        assertNotNull(result);
    }

    @Test
    void testCreateTrain() {
        var result = trainController.createTrain(SBB, "new train", trainController.loadSampleTrain());
        assertNotNull(result);
    }

    @Test
    void testShareTrain() {
        var result = trainController.shareTrain(SBB, UUID.randomUUID(), "SOB");
        assertNotNull(result);
    }

    @Test
    void testDeleteTrafficBranch() {
        var result = trainController.deleteTrainBranch(SBB, UUID.randomUUID(), "branch", "delete variant");
        assertNotNull(result);
    }

    @Test
    void testUpdateTrafficById() {
        var result = trainController.updateTrainBranch(SBB, UUID.randomUUID(), "main", "test", trainController.loadSampleTrain());
        assertNotNull(result);
    }
}
