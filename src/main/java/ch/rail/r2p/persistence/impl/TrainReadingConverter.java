package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.api.dto.Train;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
@RequiredArgsConstructor
public class TrainReadingConverter implements Converter<byte[], Train> {

    private final ObjectMapper objectMapper;

    @Override
    public Train convert(byte[] source) {
        try {
            return objectMapper.readValue(source, Train.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
