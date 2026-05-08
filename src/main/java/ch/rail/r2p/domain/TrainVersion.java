package ch.rail.r2p.domain;

import ch.rail.r2p.api.dto.Train;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
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
    UUID branchId;
    String comment;
    OffsetDateTime timestamp;
    Train payload;
    boolean head;

    public TrainVersion createNewVersion(Train payload, String comment) {
        Objects.requireNonNull(payload.getStartdate(), "payload.startdate must not be null");
        if (comment.isBlank()) {
            throw new IllegalArgumentException("comment must not be blank");
        }
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
}
