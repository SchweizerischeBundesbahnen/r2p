package ch.rail.r2p.web;

import ch.rail.r2p.api.V1Api;
import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.web.TrainService.TrainWithPartners;
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

    private static final Change DEFAULT_CHANGE = new Change()
        .timestamp(OffsetDateTime.now());

    private final TrainService trainService;
    private final TrainMapper trainMapper;

    @Override
    public ResponseEntity<TrainListElement> createTrain(PartnerToken token, String comment, Train train) {
        String owner = token.name();
        TrainWithPartners createdTrain = trainService.createTrain(owner, comment, train);
        return ResponseEntity.ok(toTrainListElement(createdTrain));
    }

    @Override
    public ResponseEntity<Branch> deleteTrainBranch(PartnerToken token, UUID trainId, String branchName, String comment) {
        TrainVersion trainVersion = trainService.deleteTrainBranch(token.name(), trainId, branchName, comment);
        return ResponseEntity.ok(trainMapper.toBranch(trainVersion));
    }

    @Override
    public ResponseEntity<TrainListElement> getTrainById(PartnerToken token, UUID trainId) {
        TrainWithPartners train = trainService.getTrainById(token.name(), trainId);
        return ResponseEntity.ok(toTrainListElement(train));
    }

    @Override
    public ResponseEntity<Branch> getTrainBranch(PartnerToken token, UUID trainId, String branchName) {
        TrainVersion trainVersion = trainService.getTrainBranch(token.name(), trainId, branchName);
        return ResponseEntity.ok(trainMapper.toBranch(trainVersion));
    }

    @Override
    public ResponseEntity<Train> getTrainVersion(PartnerToken token, UUID version) {
        Train train = trainService.getTrainVersion(token.name(), version);
        return ResponseEntity.ok(train);
    }

    @Override
    public ResponseEntity<List<TrainListElement>> listTrains(PartnerToken token, LocalDate from, LocalDate to) {
        List<TrainWithPartners> trains = trainService.listTrains(token.name(), from, to);
        return ResponseEntity.ok(trains.stream().map(this::toTrainListElement).toList());
    }

    @Override
    public ResponseEntity<TrainListElement> shareTrain(PartnerToken token, UUID trainId, String shareWith) {
        trainService.shareTrain(token.name(), trainId, shareWith);
        TrainWithPartners train = trainService.getTrainById(token.name(), trainId);
        return ResponseEntity.ok(toTrainListElement(train));
    }

    @Override
    public ResponseEntity<Branch> updateTrainBranch(PartnerToken token, UUID trainId, String branchName, String comment, Train train) {
        TrainVersion newVersion = trainService.updateTrainBranch(token.name(), trainId, branchName, comment, train);
        return ResponseEntity.ok(trainMapper.toBranch(newVersion));
    }

    @Override
    public ResponseEntity<List<Change>> getTrainChangesById(PartnerToken token, UUID trainId, OffsetDateTime from, OffsetDateTime to) {
        return ResponseEntity.ok(List.of(DEFAULT_CHANGE));
    }

    @Override
    public ResponseEntity<List<Change>> listTrainChanges(PartnerToken token, OffsetDateTime from, OffsetDateTime to) {
        return ResponseEntity.ok(List.of(DEFAULT_CHANGE));
    }

    private TrainListElement toTrainListElement(TrainWithPartners train) {
        return trainMapper.toTrainListElement(train.trainVersions(), train.partners());
    }
}
