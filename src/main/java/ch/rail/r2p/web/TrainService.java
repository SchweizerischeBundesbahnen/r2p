package ch.rail.r2p.web;

import static ch.rail.r2p.Constants.MAIN_BRANCH_NAME;
import static java.util.stream.Collectors.groupingBy;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.ChangelogPersistence;
import ch.rail.r2p.persistence.TrainPersistence;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TrainService {

    @Autowired
    private TrainPersistence trainPersistence;
    @Autowired
    private ChangelogPersistence changelogPersistence;
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
            .branchId(UUID.randomUUID())
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
        Changelog changelog = Changelog.forCreation(owner, createdTrain, comment);
        changelogPersistence.insert(changelog);
        return new TrainWithPartners(List.of(createdTrain), List.of(owner));
    }

    public TrainVersion deleteTrainBranch(String partner, UUID trainId, UUID branchId, String comment) {
        TrainVersion trainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId).orElseThrow();
        checkSharing(partner, trainVersion);
        checkBranchOwner(partner, trainVersion);
        trainVersion.setBranchState(BranchState.DELETED);
        trainPersistence.updateVersion(trainVersion); // Update the train keeping the current version
        Changelog changelog = Changelog.forDeletion(partner, trainVersion, comment);
        changelogPersistence.insert(changelog);
        // If main branch is deleted, delete all other branches as well
        if (trainVersion.getBranchName().equals(MAIN_BRANCH_NAME)) {
            List<TrainVersion> branches = trainPersistence.findHeadVersionsByTrainId(trainId);
            for (TrainVersion branch : branches) {
                if (branch.getBranchState() != BranchState.DELETED) {
                    branch.setBranchState(BranchState.DELETED);
                    trainPersistence.updateVersion(branch);
                    changelogPersistence.insert(Changelog.forDeletion(partner, branch, comment));
                }
            }
        }
        return trainVersion;
    }

    public TrainWithPartners getTrainById(String partner, UUID trainId) {
        List<TrainVersion> trains = trainPersistence.findHeadVersionsByTrainId(trainId);
        trains.getFirst(); // Check if the train exists, throws a NoSuchElementException if it doesn't
        checkSharing(partner, trainId);
        List<String> partners = trainPersistence.getPartners(trainId);
        return new TrainWithPartners(trains, partners);
    }

    public TrainVersion getTrainBranch(String partner, UUID trainId, UUID branchId) {
        TrainVersion trainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId).orElseThrow();
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

    public void shareTrain(String partner, UUID trainId, String shareWith) {
        TrainVersion trainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
        checkSharing(partner, trainVersion);
        trainPersistence.addPartner(trainId, shareWith);
        Changelog changelog = Changelog.forSharing(partner, shareWith, trainVersion, "Train shared");
        changelogPersistence.insert(changelog);
    }

    public void unshareTrain(String partner, UUID trainId, String unshareWith) {
        TrainVersion trainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
        checkBranchOrInstanceOwner(partner, trainVersion);
        trainPersistence.removePartner(trainId, unshareWith);
        Changelog changelog = Changelog.forUnsharing(partner, unshareWith, trainVersion, "Train unshared");
        changelogPersistence.insert(changelog);
        List<TrainVersion> branches = trainPersistence.findHeadVersionsByTrainId(trainId);
        for (TrainVersion branch : branches) {
            if (branch.getOwner().equals(unshareWith) && branch.getBranchState() != BranchState.DELETED) {
                branch.setBranchState(BranchState.DELETED);
                trainPersistence.updateVersion(branch);
                changelogPersistence.insert(Changelog.forDeletion(partner, branch, "Branch deleted due to unsharing of train"));
            }
        }
    }

    public TrainVersion createTrainBranch(String partner, UUID trainId, String branchName, String comment, Train train) {
        TrainVersion mainTrainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
        checkSharing(partner, mainTrainVersion);
        checkBranchState(mainTrainVersion, BranchState.OPEN);
        TrainVersion newVersion = mainTrainVersion.createBranch(branchName, partner, train, comment);
        newVersion.setPayload(train);
        newVersion.setTimestamp(OffsetDateTime.now());
        newVersion.setComment(comment);
        newVersion.setTrainNumber(train.getId());
        newVersion.setOperationalDay(train.getStartdate());
        trainPersistence.insertVersion(newVersion);
        Changelog changelog = Changelog.forCreation(partner, newVersion, comment);
        changelogPersistence.insert(changelog);
        return newVersion;
    }

    public TrainVersion updateTrainBranch(String partner, UUID trainId, UUID branchId, String comment, Train train) {
        TrainVersion branchTrainVersion = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId).orElseThrow();
        checkSharing(partner, branchTrainVersion);
        checkBranchOwner(partner, branchTrainVersion);
        checkBranchState(branchTrainVersion, BranchState.OPEN);
        branchTrainVersion.setHead(false);
        trainPersistence.updateVersion(branchTrainVersion);
        TrainVersion newVersion = branchTrainVersion.createNewVersion(train, comment);
        newVersion.setPayload(train);
        newVersion.setTimestamp(OffsetDateTime.now());
        newVersion.setComment(comment);
        newVersion.setTrainNumber(train.getId());
        newVersion.setOperationalDay(train.getStartdate());
        trainPersistence.insertVersion(newVersion);
        Changelog changelog = Changelog.forUpdate(partner, newVersion, comment);
        changelogPersistence.insert(changelog);
        return newVersion;
    }

    public List<Changelog> getChangelogs(String partner, OffsetDateTime from, OffsetDateTime to, UUID trainId) {
        return changelogPersistence.find(partner, from, to, trainId);
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

    private void checkBranchState(TrainVersion trainVersion, BranchState... allowedStates) {
        Set<BranchState> allowedStateSet = Arrays.stream(allowedStates).collect(Collectors.toSet());
        if (!allowedStateSet.contains(trainVersion.getBranchState())) {
            throw new IllegalStateException("Branch state not allowed: " + trainVersion.getBranchState());
        }
    }
}
