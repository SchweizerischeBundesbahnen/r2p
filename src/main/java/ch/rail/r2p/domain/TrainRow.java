package ch.rail.r2p.domain;

import ch.rail.r2p.api.dto.ChangeType;
import ch.rail.r2p.api.dto.PartnerType;
import ch.rail.r2p.api.dto.Train;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class TrainRow {
    UUID trainId;
    String trainNumber;
    LocalDate operationalDay;
    String variant;
    String comment;
    OffsetDateTime timestamp;
    UUID version;
    Train payload;
    List<PartnerType> partners;
    ChangeType changeType;
}
