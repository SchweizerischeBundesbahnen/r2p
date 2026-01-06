package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.Constants;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class UtcToOffsetDateTimeReadingConverter implements Converter<Timestamp, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(Timestamp source) {
        return source.toInstant()
            .atZone(Constants.ZURICH_ZONE_ID)
            .toOffsetDateTime();
    }
}