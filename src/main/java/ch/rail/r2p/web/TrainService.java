package ch.rail.r2p.web;

import static ch.rail.r2p.domain.Branch.MAIN_BRANCH_NAME;
import static java.util.stream.Collectors.groupingBy;

import ch.rail.r2p.api.dto.Action;
import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.MergeAction;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.Branch;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.HeadVersionRef;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.domain.TrainVersionEnriched;
import ch.rail.r2p.persistence.ChangelogPersistence;
import ch.rail.r2p.persistence.TrainPersistence;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TrainService {

    @Autowired
    private TrainPersistence trainPersistence;
    @Autowired
    private ChangelogPersistence changelogPersistence;
    @Value("${r2p.instance-owner}")
    private String instanceOwner;

    @Transactional
    public TrainVersionEnriched createTrain(String owner, String comment, Train train) {
        validateComment(comment);
        Objects.requireNonNull(train.getStartdate(), "Train startdate must not be null");
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

    @Transactional
    public Branch deleteTrainBranch(String partner, UUID trainId, UUID branchId, String comment) {
        validateComment(comment);
        Branch branch = trainPersistence.findBranch(trainId, branchId)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
        List<String> partners = trainPersistence.getPartners(trainId);
        checkSharing(partner, partners);
        checkBranchOwner(partner, branch);
        checkBranchState(branch, BranchState.OPEN, BranchState.MERGE_REQUESTED);
        // If an open merge request exists, implicitly reject it first so the changelog stays consistent
        if (branch.getState() == BranchState.MERGE_REQUESTED) {
            HeadVersionRef ref = trainPersistence.findHeadVersionRef(trainId, branchId)
                .orElseThrow(() -> new NoSuchElementException("Head version not found for branch " + branchId));
            changelogPersistence.insert(Changelog.forMergeRejection(partner, trainId, branchId,
                branch.getPrefixedName(), ref.version(),
                "Merge request implicitly rejected due to branch deletion"));
        }
        Branch updatedBranch = trainPersistence.updateBranchState(branchId, BranchState.DELETED);
        changelogPersistence.insert(Changelog.forDeletion(partner, branch, comment));
        // If main branch is deleted, delete all other branches as well
        if (branch.isMainBranch()) {
            List<Branch> allBranches = trainPersistence.findBranchesByTrainId(trainId);
            for (Branch b : allBranches) {
                if (b.getState() != BranchState.DELETED) {
                    if (b.getState() == BranchState.MERGE_REQUESTED) {
                        HeadVersionRef ref = trainPersistence.findHeadVersionRef(trainId, b.getBranchId())
                            .orElseThrow(() -> new NoSuchElementException("Head version not found for branch " + b.getBranchId()));
                        changelogPersistence.insert(Changelog.forMergeRejection(partner, trainId, b.getBranchId(),
                            b.getPrefixedName(), ref.version(),
                            "Merge request implicitly rejected due to branch deletion"));
                    }
                    trainPersistence.updateBranchState(b.getBranchId(), BranchState.DELETED);
                    changelogPersistence.insert(Changelog.forDeletion(partner, b, comment));
                }
            }
        }
        return updatedBranch;
    }

    public List<TrainVersionEnriched> getTrainById(String partner, UUID trainId) {
        List<TrainVersionEnriched> trains = trainPersistence.findHeadVersionsByTrainId(trainId);
        if (trains.isEmpty()) {
            throw new NoSuchElementException("Train not found");
        }
        checkSharing(partner, trains.getFirst());
        return trains;
    }

    public TrainVersionEnriched getTrainBranch(String partner, UUID trainId, UUID branchId) {
        TrainVersionEnriched train = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
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
        List<TrainVersionEnriched> trainVersions = trainPersistence.findHeadVersions(partner, from, to);
        return trainVersions.stream().collect(groupingBy(t -> t.trainVersion().getTrainId()));
    }

    @Transactional
    public void shareTrain(String partner, UUID trainId, String shareWith, String comment) {
        validateComment(comment);
        TrainVersionEnriched main = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
        checkSharing(partner, main);
        if (main.partners().contains(shareWith)) {
            log.warn("Train {} is already shared with partner '{}' - doing nothing", trainId, shareWith);
            return;
        }
        trainPersistence.addPartner(trainId, shareWith);
        Changelog changelog = Changelog.forSharing(partner, shareWith, main.trainVersion(), comment);
        changelogPersistence.insert(changelog);
    }

    @Transactional
    public void unshareTrain(String partner, UUID trainId, String unshareWith, String comment) {
        validateComment(comment);
        TrainVersionEnriched main = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
        checkUnsharingAuthorization(main.partners(), partner, unshareWith);
        trainPersistence.removePartner(trainId, unshareWith);
        Changelog changelog = Changelog.forUnsharing(partner, unshareWith, main.trainVersion(), comment);
        changelogPersistence.insert(changelog);
        List<Branch> branches = trainPersistence.findBranchesByTrainId(trainId);
        for (Branch branch : branches) {
            if (branch.getOwner().equals(unshareWith) && branch.getState() != BranchState.DELETED) {
                // If the branch has an open merge request, implicitly reject it first
                if (branch.getState() == BranchState.MERGE_REQUESTED) {
                    HeadVersionRef ref = trainPersistence.findHeadVersionRef(trainId, branch.getBranchId())
                        .orElseThrow(() -> new NoSuchElementException("Head version not found for branch " + branch.getBranchId()));
                    changelogPersistence.insert(Changelog.forMergeRejection(partner, trainId, branch.getBranchId(),
                        branch.getPrefixedName(), ref.version(),
                        "Merge request implicitly rejected due to unsharing of train"));
                }
                trainPersistence.updateBranchState(branch.getBranchId(), BranchState.DELETED);
                changelogPersistence.insert(Changelog.forDeletion(partner, branch, "Branch deleted due to unsharing of train"));
            }
        }
    }

    @Transactional
    public TrainVersionEnriched createTrainBranch(String partner, UUID trainId, String branchName, String comment, Train train) {
        validateComment(comment);
        TrainVersionEnriched main = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
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

    @Transactional
    public TrainVersionEnriched updateTrainBranch(String partner, UUID trainId, UUID branchId, String comment, Train train) {
        validateComment(comment);
        Objects.requireNonNull(train.getStartdate(), "Train startdate must not be null");
        TrainVersionEnriched branch = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
        checkSharing(partner, branch);
        checkBranchOwner(partner, branch.branch());
        checkBranchState(branch.branch(), BranchState.OPEN);
        branch.trainVersion().setHead(false);
        trainPersistence.updateVersion(branch.trainVersion());
        TrainVersion newVersion = branch.trainVersion().createNewVersion(train, comment);
        newVersion.setPayload(train);
        newVersion.setTimestamp(OffsetDateTime.now());
        newVersion.setComment(comment);
        newVersion.setTrainNumber(train.getId());
        newVersion.setOperationalDay(train.getStartdate());
        trainPersistence.insertVersion(newVersion);
        Changelog changelog = Changelog.forUpdate(partner, newVersion, branch.branch(), comment);
        changelogPersistence.insert(changelog);
        return new TrainVersionEnriched(newVersion, branch.branch(), branch.partners());
    }

    public List<Changelog> listMergeRequests(String partner, UUID trainId, MergeAction state) {
        if (!trainPersistence.existsByTrainId(trainId)) {
            throw new NoSuchElementException("Train not found");
        }
        // Authorization: partner must have access to this train
        List<TrainVersionEnriched> trains = trainPersistence.findHeadVersionsByTrainId(trainId);
        if (trains.isEmpty()) {
            throw new NoSuchElementException("Train not found");
        }
        checkSharing(partner, trains.getFirst());
        List<Changelog> entries = changelogPersistence.findLatestMergeRelatedPerBranch(partner, trainId);
        if (state != null) {
            Action actionFilter = Action.fromValue(state.getValue());
            entries = entries.stream().filter(e -> e.getAction() == actionFilter).toList();
        }
        return entries;
    }

    public List<Changelog> getChangelogs(String partner, OffsetDateTime from, OffsetDateTime to, UUID trainId) {
        if (trainId != null && !trainPersistence.existsByTrainId(trainId)) {
            throw new NoSuchElementException("Train not found");
        }
        return changelogPersistence.find(partner, from, to, trainId);
    }

    @Transactional
    public TrainVersionEnriched requestMerge(String partner, UUID trainId, UUID branchId, String comment) {
        validateComment(comment);
        TrainVersionEnriched branch = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
        checkSharing(partner, branch);
        checkBranchOwner(partner, branch.branch());
        if (branch.branch().isMainBranch()) {
            throw new IllegalArgumentException("Cannot request a merge on the main branch");
        }
        checkBranchState(branch.branch(), BranchState.OPEN);
        var updatedBranch = trainPersistence.updateBranchState(branchId, BranchState.MERGE_REQUESTED);
        Changelog changelog = Changelog.forMergeRequest(partner, branch.trainVersion(), updatedBranch, comment);
        changelogPersistence.insert(changelog);
        return new TrainVersionEnriched(branch.trainVersion(), updatedBranch, branch.partners());
    }

    @Transactional
    public List<TrainVersionEnriched> merge(String partner, UUID trainId, UUID branchId, UUID reviewedVersion, String comment) {
        validateComment(comment);
        checkInstanceOwner(partner);
        TrainVersionEnriched branchEnriched = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
        if (branchEnriched.branch().isMainBranch()) {
            throw new IllegalArgumentException("Cannot merge the main branch into itself");
        }
        checkBranchState(branchEnriched.branch(), BranchState.OPEN, BranchState.MERGE_REQUESTED);
        if (!branchEnriched.trainVersion().getVersion().equals(reviewedVersion)) {
            throw new MergeConflictException("Reviewed version does not match current head version of the branch");
        }
        TrainVersionEnriched mainEnriched = trainPersistence.findHeadVersionByTrainIdAndBranchName(trainId, MAIN_BRANCH_NAME)
            .orElseThrow(() -> new NoSuchElementException("Main branch not found"));
        checkBranchState(mainEnriched.branch(), BranchState.OPEN);
        // Mark old main head as non-head
        mainEnriched.trainVersion().setHead(false);
        trainPersistence.updateVersion(mainEnriched.trainVersion());
        // Create new main version from branch content
        TrainVersion newMainVersion = mainEnriched.trainVersion().createNewVersion(branchEnriched.trainVersion().getPayload(),
            comment);
        newMainVersion.setTrainNumber(branchEnriched.trainVersion().getTrainNumber());
        newMainVersion.setOperationalDay(branchEnriched.trainVersion().getOperationalDay());
        trainPersistence.insertVersion(newMainVersion);
        // Close the merged branch
        var closedBranch = trainPersistence.updateBranchState(branchId, BranchState.MERGED);
        // Two changelog entries: one on the source branch (MERGED) for merge-request tracking,
        // recording the branch version that was merged; and one on the main branch (UPDATED) to record why its content changed.
        changelogPersistence.insert(Changelog.forMerge(partner, closedBranch, branchEnriched.trainVersion(), comment));
        changelogPersistence.insert(Changelog.forUpdate(partner, newMainVersion, mainEnriched.branch(), comment));
        return trainPersistence.findHeadVersionsByTrainId(trainId);
    }

    @Transactional
    public TrainVersionEnriched rejectMerge(String partner, UUID trainId, UUID branchId, String comment) {
        validateComment(comment);
        checkInstanceOwner(partner);
        TrainVersionEnriched branch = trainPersistence.findHeadVersionByTrainIdAndBranchId(trainId, branchId)
            .orElseThrow(() -> new NoSuchElementException("Train or Branch not found"));
        checkBranchState(branch.branch(), BranchState.MERGE_REQUESTED);
        var updatedBranch = trainPersistence.updateBranchState(branchId, BranchState.OPEN);
        changelogPersistence.insert(Changelog.forMergeRejection(partner, branch.trainVersion(), updatedBranch, comment));
        return new TrainVersionEnriched(branch.trainVersion(), updatedBranch, branch.partners());
    }

    private void checkSharing(String partner, TrainVersionEnriched train) {
        checkSharing(partner, train.partners());
    }

    private void checkSharing(String partner, List<String> partners) {
        if (!partners.contains(partner)) {
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

    private void checkUnsharingAuthorization(List<String> allPartners, String currentPartner, String unshareWith) {
        if (!currentPartner.equals(instanceOwner) && !currentPartner.equals(unshareWith)) {
            throw new AuthorizationDeniedException("Partner is not instance owner or himself");
        }
        if (!allPartners.contains(unshareWith)) {
            throw new AuthorizationDeniedException("Train is not shared with partner '" + unshareWith + "'");
        }
    }

    private void checkBranchState(Branch branch, BranchState... allowedStates) {
        Set<BranchState> allowedStateSet = Arrays.stream(allowedStates).collect(Collectors.toSet());
        if (!allowedStateSet.contains(branch.getState())) {
            throw new InvalidBranchStateException("Branch state not allowed: " + branch.getState());
        }
    }

    private static void validateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("comment must not be blank");
        }
    }
}
