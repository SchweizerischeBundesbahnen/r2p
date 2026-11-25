package ch.rail.r2p.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.Constants;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UtcToOffsetDateTimeReadingConverterTests {

    private final UtcToOffsetDateTimeReadingConverter converter = new UtcToOffsetDateTimeReadingConverter();

    @Test
    void should_convert_utc_timestamp_to_zurich_offset_date_time() {
        // Given: a UTC timestamp for 2026-03-25T10:00:00Z
        // In winter time Europe/Zurich is UTC+1, so expected offset is +01:00
        Instant instant = Instant.parse("2026-03-25T10:00:00Z");
        Timestamp timestamp = Timestamp.from(instant);

        // When
        OffsetDateTime result = converter.convert(timestamp);

        // Then: the instant is preserved and the offset matches Zurich (CET = +01:00 in March)
        assertThat(result).isNotNull();
        assertThat(result.toInstant()).isEqualTo(instant);
        assertThat(result.getOffset()).isEqualTo(
            Constants.ZURICH_ZONE_ID.getRules().getOffset(instant));
    }

    @Test
    void should_convert_summer_time_timestamp_to_zurich_offset_date_time() {
        // Given: a UTC timestamp during CEST (summer time), Europe/Zurich is UTC+2
        Instant instant = Instant.parse("2026-07-15T10:00:00Z");
        Timestamp timestamp = Timestamp.from(instant);

        // When
        OffsetDateTime result = converter.convert(timestamp);

        // Then: the instant is preserved and the offset is +02:00 (CEST)
        assertThat(result).isNotNull();
        assertThat(result.toInstant()).isEqualTo(instant);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.of("+02:00"));
    }

    @Test
    void should_preserve_the_exact_instant() {
        // Given: an arbitrary timestamp
        Instant instant = Instant.parse("2024-01-01T00:00:00Z");
        Timestamp timestamp = Timestamp.from(instant);

        // When
        OffsetDateTime result = converter.convert(timestamp);

        // Then: no information is lost – the underlying instant stays the same
        assertThat(result).isNotNull();
        assertThat(result.toInstant()).isEqualTo(instant);
    }
}




