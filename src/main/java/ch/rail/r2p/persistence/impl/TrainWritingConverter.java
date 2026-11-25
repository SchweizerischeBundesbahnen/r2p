package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.api.dto.Train;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
@RequiredArgsConstructor
public class TrainWritingConverter implements Converter<Train, byte[]> {

    private final ObjectMapper objectMapper;

    @Override
    public byte[] convert(Train source) {
        try {
            return objectMapper.writeValueAsBytes(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
