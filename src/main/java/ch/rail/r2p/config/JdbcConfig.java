package ch.rail.r2p.config;

import ch.rail.r2p.persistence.impl.TrainReadingConverter;
import ch.rail.r2p.persistence.impl.TrainWritingConverter;
import ch.rail.r2p.persistence.impl.UtcToOffsetDateTimeReadingConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

@Configuration
@RequiredArgsConstructor
public class JdbcConfig extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;
    private final TrainReadingConverter trainReadingConverter;

    @NullMarked
    @Override
    public List<?> userConverters() {
        return Arrays.asList(
            new UtcToOffsetDateTimeReadingConverter(),
            trainReadingConverter,
            new TrainWritingConverter(objectMapper)
        );
    }

}