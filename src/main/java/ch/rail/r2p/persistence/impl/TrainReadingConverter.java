package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.api.dto.Train;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
@RequiredArgsConstructor
public class TrainReadingConverter implements Converter<byte[], Train> {

    private final ObjectMapper objectMapper;

    @NullMarked
    @Override
    public Train convert(byte[] source) {
        try {
            return objectMapper.readValue(source, Train.class);
        } catch (Exception e) {
            throw new PayloadDeserializationException("Failed to deserialize train payload: " + e.getMessage(), e);
        }
    }
}
