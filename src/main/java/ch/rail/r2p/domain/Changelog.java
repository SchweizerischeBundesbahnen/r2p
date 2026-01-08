package ch.rail.r2p.domain;

import ch.rail.r2p.api.dto.Action;
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
    private final UUID trainId;
    private final String branchName;
    private final UUID version;
    private final OffsetDateTime timestamp;
    private final String comment;

    public static Changelog forCreation(String partner, TrainVersion trainVersion, String comment) {
        return new Changelog(UUID.randomUUID(), Action.CREATED, partner, trainVersion.trainId, trainVersion.branchName, trainVersion.version, trainVersion.timestamp, comment);
    }

    public static Changelog forUpdate(String partner, TrainVersion trainVersion, String comment) {
        return new Changelog(UUID.randomUUID(), Action.UPDATED, partner, trainVersion.trainId, trainVersion.branchName, trainVersion.version, trainVersion.timestamp, comment);
    }

    public static Changelog forDeletion(String partner, TrainVersion trainVersion, String comment) {
        return new Changelog(UUID.randomUUID(), Action.DELETED, partner, trainVersion.trainId, trainVersion.branchName, trainVersion.version, OffsetDateTime.now(), comment);
    }
}
