package ch.rail.r2p.web;

import ch.rail.r2p.api.V1Api;
import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.BranchBusinessData;
import ch.rail.r2p.api.dto.BranchTechnicalData;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.ModelApiResponse;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TrainController implements V1Api {

    private static final ModelApiResponse DEFAULT_RESPONSE = new ModelApiResponse()
        .message("Not implemented")
        .code(0)
        .version(UUID.randomUUID())
        .type("none");

    private static final Change DEFAULT_CHANGE = new Change()
        .timestamp(OffsetDateTime.now());

    private static final Branch DEFAULT_BRANCH = new Branch()
        .branchName("default")
        .business(new BranchBusinessData().trainNumber("1234").operationalDay(LocalDate.now()))
        .technical(new BranchTechnicalData().owner("SBB").timestamp(OffsetDateTime.now()).comment("comment"));

    private static final TrainListElement DEFAULT_TRAIN = new TrainListElement()
        .trainId("1234").addPartnersItem("SBB").addBranchesItem(DEFAULT_BRANCH);

    private static Train sampleTrain = null;

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

    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<ModelApiResponse> createTrain(PartnerToken token, String comment, Train train) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteTrainBranch(PartnerToken token, UUID trainId, String branchName, String comment) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }

    @Override
    public ResponseEntity<TrainListElement> getTrainById(PartnerToken token, UUID trainId) {
        return ResponseEntity.ok(DEFAULT_TRAIN);
    }

    @Override
    public ResponseEntity<List<Change>> getTrainChangesById(PartnerToken token, UUID trainId, OffsetDateTime from, OffsetDateTime to) {
        return ResponseEntity.ok(List.of(DEFAULT_CHANGE));
    }

    @Override
    public ResponseEntity<Train> getTrainBranch(PartnerToken token, UUID trainId, String branchName) {
        return ResponseEntity.ok(loadSampleTrain());
    }

    @Override
    public ResponseEntity<Train> getTrainVersion(PartnerToken token, UUID version) {
        return ResponseEntity.ok(loadSampleTrain());
    }

    @Override
    public ResponseEntity<List<Change>> listTrainChanges(PartnerToken token, OffsetDateTime from, OffsetDateTime to) {
        return ResponseEntity.ok(List.of(DEFAULT_CHANGE));
    }

    @Override
    public ResponseEntity<List<TrainListElement>> listTrains(PartnerToken token, LocalDate from, LocalDate to) {
        return ResponseEntity.ok(List.of(DEFAULT_TRAIN));
    }

    @Override
    public ResponseEntity<ModelApiResponse> shareTrain(PartnerToken token, UUID trainId, String shareWith) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateTrainBranch(PartnerToken token, UUID trainId, String branchName, String comment, Train train) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }
}
