package ch.rail.r2p.domain;

import ch.rail.r2p.api.dto.Action;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@ToString
public class Changelog {

    @Id
    private final UUID id;
    private final Action action;
    private final String partner;
    @Nullable
    private final String affectedPartner;
    private final UUID trainId;
    @Nullable
    private final UUID branchId;
    @Nullable
    private final String branchName;
    @Nullable
    private final UUID version;
    private final OffsetDateTime timestamp;
    private final String comment;

    public static Changelog forCreation(String partner, TrainVersion trainVersion, Branch branch, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.CREATED, partner, null, trainVersion.trainId, trainVersion.branchId, branch.getPrefixedName(), trainVersion.version, trainVersion.timestamp,
            comment);
    }

    public static Changelog forUpdate(String partner, TrainVersion trainVersion, Branch branch, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.UPDATED, partner, null, trainVersion.trainId, trainVersion.branchId, branch.getPrefixedName(), trainVersion.version, trainVersion.timestamp,
            comment);
    }

    public static Changelog forDeletion(String partner, Branch branch, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.DELETED, partner, null, branch.trainId, branch.branchId, branch.getPrefixedName(), null, OffsetDateTime.now(),
            comment);
    }

    public static Changelog forSharing(String partner, String affectedPartner, TrainVersion trainVersion, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.SHARED, partner, affectedPartner, trainVersion.trainId, null, null, trainVersion.version,
            OffsetDateTime.now(), comment);
    }

    public static Changelog forUnsharing(String partner, String affectedPartner, TrainVersion trainVersion, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.UNSHARED, partner, affectedPartner, trainVersion.trainId, null, null, trainVersion.version,
            OffsetDateTime.now(), comment);
    }

    public static Changelog forMergeRequest(String partner, TrainVersion trainVersion, Branch branch, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.MERGE_REQUESTED, partner, null, trainVersion.trainId, trainVersion.branchId, branch.getPrefixedName(), trainVersion.version,
            OffsetDateTime.now(), comment);
    }

    public static Changelog forMerge(String partner, Branch sourceBranch, TrainVersion mergedBranchVersion, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.MERGED, partner, null, mergedBranchVersion.trainId, sourceBranch.branchId, sourceBranch.getPrefixedName(), mergedBranchVersion.version,
            OffsetDateTime.now(), comment);
    }

    public static Changelog forMergeRejection(String partner, TrainVersion trainVersion, Branch branch, String comment) {
        validateComment(comment);
        return forMergeRejection(partner, trainVersion.trainId, trainVersion.branchId, branch.getPrefixedName(),
            trainVersion.version, comment);
    }

    /**
     * Creates a MERGE_REJECTED changelog entry without requiring a full {@link TrainVersion} to be loaded.
     * Use this overload when the payload must not be deserialized (e.g. during branch deletion).
     */
    public static Changelog forMergeRejection(String partner, UUID trainId, UUID branchId,
            String prefixedBranchName, UUID version, String comment) {
        validateComment(comment);
        return new Changelog(UUID.randomUUID(), Action.MERGE_REJECTED, partner, null, trainId, branchId,
            prefixedBranchName, version, OffsetDateTime.now(), comment);
    }

    private static void validateComment(String comment) {
        if (comment.isBlank()) {
            throw new IllegalArgumentException("comment must not be blank");
        }
    }
}
