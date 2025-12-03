package ch.rail.r2p.web;

import ch.rail.r2p.api.V1Api;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.ModelApiResponse;
import ch.rail.r2p.api.dto.PartnerType;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainList;
import ch.rail.r2p.api.dto.Variant;
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
        .timeStamp(OffsetDateTime.now());

    private static final Variant DEFAULT_VARIANT = new Variant()
        .variant("default").comment("comment").timeStamp(OffsetDateTime.now());

    private static final TrainList DEFAULT_TRAIN = new TrainList()
        .trainId("1234").trainNumber("1234").operationalDay(LocalDate.now())
        .addPartnersItem(PartnerType.SBB).addVariantsItem(DEFAULT_VARIANT);

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
    public ResponseEntity<ModelApiResponse> createTrain(List<PartnerType> partner, String comment, Train train) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteTrainVariant(UUID trainId, String variant, List<PartnerType> partner, String comment) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }

    @Override
    public ResponseEntity<TrainList> getTrainById(UUID trainId, PartnerType partner) {
        return ResponseEntity.ok(DEFAULT_TRAIN);
    }

    @Override
    public ResponseEntity<List<Change>> getTrainChangesById(UUID trainId, PartnerType partner, OffsetDateTime from, OffsetDateTime to) {
        return ResponseEntity.ok(List.of(DEFAULT_CHANGE));
    }

    @Override
    public ResponseEntity<Train> getTrainVariant(UUID trainId, String variant, PartnerType partner) {
        return ResponseEntity.ok(loadSampleTrain());
    }

    @Override
    public ResponseEntity<Train> getTrainVersion(UUID trainId, UUID version, PartnerType partner) {
        return ResponseEntity.ok(loadSampleTrain());
    }

    @Override
    public ResponseEntity<List<Change>> listTrainChanges(OffsetDateTime from, OffsetDateTime to, PartnerType partner) {
        return ResponseEntity.ok(List.of(DEFAULT_CHANGE));
    }

    @Override
    public ResponseEntity<List<TrainList>> listTrains(PartnerType partner) {
        return ResponseEntity.ok(List.of(DEFAULT_TRAIN));
    }

    @Override
    public ResponseEntity<ModelApiResponse> shareTrain(UUID trainId, List<PartnerType> partner) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateTrain(UUID trainId, List<PartnerType> partner, String comment, Train train) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateTrainVariant(UUID trainId, String variant, List<PartnerType> partner, String comment, Train train) {
        return ResponseEntity.ok(DEFAULT_RESPONSE);
    }
}
