package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerType.SBB;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
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
        var result = trainController.listTrains(SBB);
        assertNotNull(result);
    }

    @Test
    void testGetTrainById() {
        var result = trainController.getTrainById(UUID.randomUUID(), SBB);
        assertNotNull(result);
    }

    @Test
    void testCreateTrain() {
        var result = trainController.createTrain(List.of(SBB), "new train", trainController.loadSampleTrain());
        assertNotNull(result);
    }

    @Test
    void testShareTrain() {
        var result = trainController.shareTrain(UUID.randomUUID(), List.of(SBB));
        assertNotNull(result);
    }

    @Test
    void testDeleteTrafficVariant() {
        var result = trainController.deleteTrainVariant(UUID.randomUUID(), "variant", List.of(SBB), "delete variant");
        assertNotNull(result);
    }

    @Test
    void testUpdateTrafficById() {
        var result = trainController.updateTrain(UUID.randomUUID(), List.of(SBB), "test", trainController.loadSampleTrain());
        assertNotNull(result);
    }
}
