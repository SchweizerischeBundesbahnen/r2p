package ch.rail.r2p.web;

import ch.rail.r2p.api.R2PApi;
import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.MergeAction;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.TrainVersionEnriched;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Valid
public class TrainController implements R2PApi {

    private final TrainService trainService;
    private final TrainMapper trainMapper;

    @Override
    public ResponseEntity<TrainListElement> createTrain(PartnerToken token, String comment, Train train) {
        String owner = token.name();
        TrainVersionEnriched createdTrain = trainService.createTrain(owner, comment, train);
        return ResponseEntity.ok(trainMapper.toTrainListElement(List.of(createdTrain)));
    }

    @Override
    public ResponseEntity<Branch> createTrainBranch(PartnerToken token, UUID trainId, String branchName, String comment, Train train) {
        TrainVersionEnriched created = trainService.createTrainBranch(token.name(), trainId, branchName, comment, train);
        return ResponseEntity.ok(trainMapper.toBranch(created));
    }

    @Override
    public ResponseEntity<Branch> deleteTrainBranch(PartnerToken token, UUID trainId, UUID branchId, String comment) {
        ch.rail.r2p.domain.Branch deleted = trainService.deleteTrainBranch(token.name(), trainId, branchId, comment);
        return ResponseEntity.ok(trainMapper.toBranch(deleted));
    }

    @Override
    public ResponseEntity<TrainListElement> getTrainById(PartnerToken token, UUID trainId) {
        List<TrainVersionEnriched> train = trainService.getTrainById(token.name(), trainId);
        return ResponseEntity.ok(trainMapper.toTrainListElement(train));
    }

    @Override
    public ResponseEntity<Branch> getTrainBranch(PartnerToken token, UUID trainId, UUID branchId) {
        TrainVersionEnriched train = trainService.getTrainBranch(token.name(), trainId, branchId);
        return ResponseEntity.ok(trainMapper.toBranch(train));
    }

    @Override
    public ResponseEntity<Train> getTrainVersion(PartnerToken token, UUID version) {
        Train train = trainService.getTrainVersion(token.name(), version);
        return ResponseEntity.ok(train);
    }

    @Override
    public ResponseEntity<List<TrainListElement>> listTrains(PartnerToken token, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Parameter 'operationalDayTo' (to) must not be before 'operationalDayFrom' (from)");
        }
        Map<UUID, List<TrainVersionEnriched>> trains = trainService.listTrains(token.name(), from, to);
        return ResponseEntity.ok(trains.values().stream().map(trainMapper::toTrainListElement).toList());
    }

    @Override
    public ResponseEntity<TrainListElement> shareTrain(PartnerToken token, UUID trainId, String shareWith, String comment) {
        trainService.shareTrain(token.name(), trainId, shareWith, comment);
        List<TrainVersionEnriched> train = trainService.getTrainById(token.name(), trainId);
        return ResponseEntity.ok(trainMapper.toTrainListElement(train));
    }

    @Override
    public ResponseEntity<TrainListElement> unshareTrain(PartnerToken token, UUID trainId, String unshareWith, String comment) {
        trainService.unshareTrain(token.name(), trainId, unshareWith, comment);
        List<TrainVersionEnriched> train = trainService.getTrainById(token.name(), trainId);
        return ResponseEntity.ok(trainMapper.toTrainListElement(train));
    }

    @Override
    public ResponseEntity<Branch> updateTrainBranch(PartnerToken token, UUID trainId, UUID branchId, String comment, Train train) {
        TrainVersionEnriched newVersion = trainService.updateTrainBranch(token.name(), trainId, branchId, comment, train);
        return ResponseEntity.ok(trainMapper.toBranch(newVersion));
    }

    @Override
    public ResponseEntity<List<Change>> getTrainChangesById(PartnerToken token, UUID trainId, OffsetDateTime from, OffsetDateTime to) {
        List<Changelog> changelogs = trainService.getChangelogs(token.name(), from, to, trainId);
        return ResponseEntity.ok(changelogs.stream().map(trainMapper::toChange).toList());
    }

    @Override
    public ResponseEntity<List<Change>> listTrainChanges(PartnerToken token, OffsetDateTime from, OffsetDateTime to) {
        List<Changelog> changelogs = trainService.getChangelogs(token.name(), from, to, null);
        return ResponseEntity.ok(changelogs.stream().map(trainMapper::toChange).toList());
    }

    @Override
    public ResponseEntity<List<Change>> listMergeRequests(PartnerToken token, UUID trainId, MergeAction state) {
        List<Changelog> entries = trainService.listMergeRequests(token.name(), trainId, state);
        return ResponseEntity.ok(entries.stream().map(trainMapper::toChange).toList());
    }

    @Override
    public ResponseEntity<Branch> requestMerge(PartnerToken token, UUID trainId, UUID branchId, String comment) {
        TrainVersionEnriched result = trainService.requestMerge(token.name(), trainId, branchId, comment);
        return ResponseEntity.ok(trainMapper.toBranch(result));
    }

    @Override
    public ResponseEntity<TrainListElement> merge(PartnerToken token, UUID trainId, UUID branchId, UUID reviewedVersion, String comment) {
        List<TrainVersionEnriched> trains = trainService.merge(token.name(), trainId, branchId, reviewedVersion, comment);
        return ResponseEntity.ok(trainMapper.toTrainListElement(trains));
    }

    @Override
    public ResponseEntity<Branch> rejectMerge(PartnerToken token, UUID trainId, UUID branchId, String comment) {
        TrainVersionEnriched result = trainService.rejectMerge(token.name(), trainId, branchId, comment);
        return ResponseEntity.ok(trainMapper.toBranch(result));
    }
}
