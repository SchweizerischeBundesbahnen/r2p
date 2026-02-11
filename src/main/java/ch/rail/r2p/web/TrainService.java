package ch.rail.r2p.web;

import static ch.rail.r2p.Constants.MAIN_BRANCH_NAME;
import static java.util.stream.Collectors.groupingBy;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.Branch;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.domain.TrainVersionEnriched;
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

    public TrainVersionEnriched createTrain(String owner, String comment, Train train) {
        UUID trainId = UUID.randomUUID();
        checkInstanceOwner(owner);
        Branch branch = Branch.createBranch(trainId, MAIN_BRANCH_NAME, owner);
        trainPersistence.insertBranch(branch);
        TrainVersion createdTrain = TrainVersion.builder()
            .trainId(trainId)
            .trainNumber(train.getId())
            .operationalDay(train.getStartdate())
            .branchId(branch.getBranchId())
            .comment(comment)
            .timestamp(OffsetDateTime.now())
            .version(UUID.randomUUID())
            .payload(train)
            .head(true)
            .build();
        // Train is created implicitly with the new version
        trainPersistence.insertVersion(createdTrain);
        trainPersistence.addPartner(trainId, owner);
        Changelog changelog = Changelog.forCreation(owner, createdTrain, branch, comment);
        changelogPersistence.insert(changelog);
        return new TrainVersionEnriched(createdTrain, branch, List.of(owner));
    }

    public TrainVersionEnriched deleteTrainBranch(String partner, UUID trainId, UUID branchId, String comment) {
        var train = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId).orElseThrow();
        checkSharing(partner, train);
        checkBranchOwner(partner, train.branch());
        var updatedBranch = trainPersistence.updateBranchState(branchId, BranchState.DELETED);
        Changelog changelog = Changelog.forDeletion(partner, train.branch(), comment);
        changelogPersistence.insert(changelog);
        // If main branch is deleted, delete all other branches as well
        if (train.branch().getName().equals(MAIN_BRANCH_NAME)) {
            List<Branch> branches = trainPersistence.findBranchesByTrainId(trainId);
            for (Branch branch : branches) {
                if (branch.getState() != BranchState.DELETED) {
                    trainPersistence.updateBranchState(branch.getBranchId(), BranchState.DELETED);
                    changelogPersistence.insert(Changelog.forDeletion(partner, branch, comment));
                }
            }
        }
        return new TrainVersionEnriched(train.trainVersion(), updatedBranch, train.partners());
    }

    public List<TrainVersionEnriched> getTrainById(String partner, UUID trainId) {
        List<TrainVersionEnriched> trains = trainPersistence.findHeadVersionsByTrainId(trainId);
        trains.getFirst(); // Check if the train exists, throws a NoSuchElementException if it doesn't
        checkSharing(partner, trains.getFirst());
        return trains;
    }

    public TrainVersionEnriched getTrainBranch(String partner, UUID trainId, UUID branchId) {
        TrainVersionEnriched train = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId).orElseThrow();
        checkSharing(partner, train);
        return train;
    }

    public Train getTrainVersion(String partner, UUID version) {
        TrainVersionEnriched train = trainPersistence.getVersion(version);
        checkSharing(partner, train);
        return train.trainVersion().getPayload();
    }

    public Map<UUID, List<TrainVersionEnriched>> listTrains(String partner, LocalDate from, LocalDate to) {
        // PERF: Paging needed when dealing with hundreds or thousands of trains per day
        List<TrainVersionEnriched> trainVersions = trainPersistence.findHeadVersionsByCriteria(partner, from, to);
        return trainVersions.stream().collect(groupingBy(t -> t.trainVersion().getTrainId()));
    }

    public void shareTrain(String partner, UUID trainId, String shareWith) {
        TrainVersionEnriched main = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
        checkSharing(partner, main);
        trainPersistence.addPartner(trainId, shareWith);
        Changelog changelog = Changelog.forSharing(partner, shareWith, main.trainVersion(), "Train shared");
        changelogPersistence.insert(changelog);
    }

    public void unshareTrain(String partner, UUID trainId, String unshareWith) {
        TrainVersionEnriched main = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
        if (!partner.equals(instanceOwner) && !partner.equals(unshareWith)) {
            throw new AuthorizationDeniedException("Partner is not instance owner or himself");
        }
        trainPersistence.removePartner(trainId, unshareWith);
        Changelog changelog = Changelog.forUnsharing(partner, unshareWith, main.trainVersion(), "Train unshared");
        changelogPersistence.insert(changelog);
        List<Branch> branches = trainPersistence.findBranchesByTrainId(trainId);
        for (Branch branch : branches) {
            if (branch.getOwner().equals(unshareWith) && branch.getState() != BranchState.DELETED) {
                trainPersistence.updateBranchState(branch.getBranchId(), BranchState.DELETED);
                changelogPersistence.insert(Changelog.forDeletion(partner, branch, "Branch deleted due to unsharing of train"));
            }
        }
    }

    public TrainVersionEnriched createTrainBranch(String partner, UUID trainId, String branchName, String comment, Train train) {
        TrainVersionEnriched main = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME).orElseThrow();
        checkSharing(partner, main);
        checkBranchState(main.branch(), BranchState.OPEN);
        Branch newBranch = Branch.createBranch(trainId, branchName, partner);
        trainPersistence.insertBranch(newBranch);
        TrainVersion newVersion = main.trainVersion().createNewVersion(train, comment);
        newVersion.setBranchId(newBranch.getBranchId());
        trainPersistence.insertVersion(newVersion);
        Changelog changelog = Changelog.forCreation(partner, newVersion, newBranch, comment);
        changelogPersistence.insert(changelog);
        return new TrainVersionEnriched(newVersion, newBranch, main.partners());
    }

    public TrainVersionEnriched updateTrainBranch(String partner, UUID trainId, UUID branchId, String comment, Train train) {
        TrainVersionEnriched main = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId).orElseThrow();
        checkSharing(partner, main);
        checkBranchOwner(partner, main.branch());
        checkBranchState(main.branch(), BranchState.OPEN);
        main.trainVersion().setHead(false);
        trainPersistence.updateVersion(main.trainVersion());
        TrainVersion newVersion = main.trainVersion().createNewVersion(train, comment);
        newVersion.setPayload(train);
        newVersion.setTimestamp(OffsetDateTime.now());
        newVersion.setComment(comment);
        newVersion.setTrainNumber(train.getId());
        newVersion.setOperationalDay(train.getStartdate());
        trainPersistence.insertVersion(newVersion);
        Changelog changelog = Changelog.forUpdate(partner, newVersion, main.branch(), comment);
        changelogPersistence.insert(changelog);
        return new TrainVersionEnriched(newVersion, main.branch(), main.partners());
    }

    public List<Changelog> getChangelogs(String partner, OffsetDateTime from, OffsetDateTime to, UUID trainId) {
        return changelogPersistence.find(partner, from, to, trainId);
    }

    private void checkSharing(String partner, TrainVersionEnriched train) {
        if (!train.partners().contains(partner)) {
            throw new AuthorizationDeniedException("Train not shared with partner");
        }
    }

    private void checkInstanceOwner(String partner) {
        if (!partner.equals(instanceOwner)) {
            throw new AuthorizationDeniedException("Partner is not instance owner");
        }
    }

    private void checkBranchOwner(String partner, Branch branch) {
        if (!partner.equals(branch.getOwner())) {
            throw new AuthorizationDeniedException("Partner is not branch owner");
        }
    }

    private void checkBranchState(Branch branch, BranchState... allowedStates) {
        Set<BranchState> allowedStateSet = Arrays.stream(allowedStates).collect(Collectors.toSet());
        if (!allowedStateSet.contains(branch.getState())) {
            throw new IllegalStateException("Branch state not allowed: " + branch.getState());
        }
    }
}
