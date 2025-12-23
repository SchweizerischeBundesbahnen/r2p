package ch.rail.r2p.domain;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.api.dto.Train;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class TrainVersion {

    @Id
    UUID version;
    UUID trainId;
    String trainNumber;
    LocalDate operationalDay;
    String branchName;
    String owner;
    String comment;
    OffsetDateTime timestamp;
    Train payload;
    BranchState branchState;
    boolean head;

    public TrainVersion createNewVersion(Train payload, String comment) {
        return toBuilder()
            .version(UUID.randomUUID())
            .timestamp(OffsetDateTime.now())
            .payload(payload)
            .trainNumber(payload.getId())
            .operationalDay(payload.getStartdate())
            .comment(comment)
            .head(true)
            .build();
    }

    public TrainVersion createBranch(String branchName, String owner, Train payload, String comment) {
        TrainVersion newVersion = createNewVersion(payload, comment);
        newVersion.setBranchName(branchName);
        newVersion.setOwner(owner);
        return newVersion;
    }
}
