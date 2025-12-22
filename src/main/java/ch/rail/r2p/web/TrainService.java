package ch.rail.r2p.web;

import static ch.rail.r2p.Constants.MAIN_BRANCH_NAME;
import static java.util.stream.Collectors.groupingBy;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.TrainPersistence;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TrainService {

    @Autowired
    private TrainPersistence trainPersistence;
    @Value("${r2p.instance-owner}")
    private String instanceOwner;

    public record TrainWithPartners(List<TrainVersion> trainVersions, List<String> partners) {

    }

    public TrainWithPartners createTrain(String owner, String comment, Train train) {
        UUID trainId = UUID.randomUUID();
        checkInstanceOwner(owner);
        TrainVersion createdTrain = TrainVersion.builder()
            .trainId(trainId)
            .trainNumber(train.getId())
            .operationalDay(train.getStartdate())
            .branchName(MAIN_BRANCH_NAME)
            .comment(comment)
            .timestamp(OffsetDateTime.now())
            .version(UUID.randomUUID())
            .payload(train)
            .owner(owner)
            .branchState(BranchState.OPEN)
            .head(true)
            .build();
        // Train is created implicitly with the new version
        trainPersistence.insertVersion(createdTrain);
        trainPersistence.addPartner(trainId, owner);
        return new TrainWithPartners(List.of(createdTrain), List.of(owner));
    }

    public TrainVersion deleteTrainBranch(String partner, UUID trainId, String branchName, String comment) {
        TrainVersion trainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, branchName).orElseThrow();
        checkSharing(partner, trainVersion);
        checkBranchOrInstanceOwner(partner, trainVersion);
        // Comment is currently ignored
        // TODO store deletion and comment in globoal changelog of train
        trainVersion.setBranchState(BranchState.DELETED);
        trainPersistence.updateVersion(trainVersion); // Update the train keeping the current version
        return trainVersion;
    }

    public TrainWithPartners getTrainById(String partner, UUID trainId) {
        checkSharing(partner, trainId);
        List<TrainVersion> trains = trainPersistence.findHeadVersionsByTrainId(trainId);
        List<String> partners = trainPersistence.getPartners(trainId);
        return new TrainWithPartners(trains, partners);
    }

    public TrainVersion getTrainBranch(String partner, UUID trainId, String branchName) {
        TrainVersion trainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, branchName).orElseThrow();
        checkSharing(partner, trainVersion);
        return trainVersion;
    }

    public Train getTrainVersion(String partner, UUID version) {
        TrainVersion trainVersion = trainPersistence.getVersion(version);
        checkSharing(partner, trainVersion);
        return trainVersion.getPayload();
    }

    public List<TrainWithPartners> listTrains(String partner, LocalDate from, LocalDate to) {
        // PERF: Paging needed when dealing with hundreds or thousands of trains per day
        List<TrainVersion> trainVersions = trainPersistence.findHeadVersionsByCriteria(partner, from, to);
        Map<UUID, List<TrainVersion>> trainVersionsByTrainId = trainVersions.stream().collect(groupingBy(TrainVersion::getTrainId));
        return trainVersionsByTrainId.entrySet().stream()
            .map(entry -> new TrainWithPartners(
                entry.getValue(),
                trainPersistence.getPartners(entry.getKey()) // PERF: Could be optimized if necessary (getting all partners of all trains in one query)
            ))
            .toList();
    }

    public TrainVersion shareTrain(String partner, UUID trainId, String shareWith) {
        TrainVersion trainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
        checkSharing(partner, trainVersion);
        trainPersistence.addPartner(trainId, shareWith);
        return trainVersion;
    }

    public TrainVersion updateTrainBranch(String partner, UUID trainId, String branchName, String comment, Train train) {
        Optional<TrainVersion> branchTrainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, branchName);
        TrainVersion newVersion;
        if (branchTrainVersion.isPresent()) {
            checkSharing(partner, branchTrainVersion.get());
            checkBranchOwner(partner, branchTrainVersion.get());
            branchTrainVersion.get().setHead(false);
            trainPersistence.updateVersion(branchTrainVersion.get());
            newVersion = branchTrainVersion.get().createNewVersion(train, comment);
        } else {
            TrainVersion mainTrainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
            checkSharing(partner, mainTrainVersion);
            newVersion = mainTrainVersion.createBranch(branchName, partner, train, comment);
        }
        newVersion.setPayload(train);
        newVersion.setTimestamp(OffsetDateTime.now());
        newVersion.setComment(comment);
        newVersion.setTrainNumber(train.getId());
        newVersion.setOperationalDay(train.getStartdate());
        trainPersistence.insertVersion(newVersion);
        return newVersion;
    }

    private void checkSharing(String partner, UUID trainId) {
        if (!trainPersistence.getPartners(trainId).contains(partner)) {
            throw new AuthorizationDeniedException("Train not shared with partner");
        }
    }

    private void checkSharing(String partner, TrainVersion trainVersion) {
        checkSharing(partner, trainVersion.getTrainId());
    }

    private void checkInstanceOwner(String partner) {
        if (!partner.equals(instanceOwner)) {
            throw new AuthorizationDeniedException("Partner is not instance owner");
        }
    }

    private void checkBranchOwner(String partner, TrainVersion trainVersion) {
        if (!partner.equals(trainVersion.getOwner())) {
            throw new AuthorizationDeniedException("Partner is not branch owner");
        }
    }

    private void checkBranchOrInstanceOwner(String partner, TrainVersion trainVersion) {
        if (!partner.equals(trainVersion.getOwner()) && !partner.equals(instanceOwner)) {
            throw new AuthorizationDeniedException("Partner is not branch or instance owner");
        }
    }
}
