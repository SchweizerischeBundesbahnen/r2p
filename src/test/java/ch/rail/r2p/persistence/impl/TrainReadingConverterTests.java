package ch.rail.r2p.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.rail.r2p.api.dto.Train;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TrainReadingConverterTests {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules();
    private final TrainReadingConverter converter = new TrainReadingConverter(objectMapper);

    @Test
    void should_throw_payload_deserialization_exception_for_invalid_enum_value() {
        // Given: a payload containing an unknown enum value (simulating the 'tttreftrain' scenario)
        byte[] corruptPayload = """
            {"references":[{"type":"tttreftrain","value":"12345"}]}
            """.getBytes();

        // When / Then
        assertThatThrownBy(() -> converter.convert(corruptPayload))
            .isInstanceOf(PayloadDeserializationException.class)
            .hasMessageContaining("Failed to deserialize train payload");
    }

    @Test
    void should_throw_payload_deserialization_exception_for_malformed_json() {
        // Given: bytes that are not valid JSON at all
        byte[] malformedPayload = "this is not json".getBytes();

        // When / Then
        assertThatThrownBy(() -> converter.convert(malformedPayload))
            .isInstanceOf(PayloadDeserializationException.class)
            .hasMessageContaining("Failed to deserialize train payload");
    }

    @Test
    void should_not_expose_jackson_internals_in_exception_type() {
        // Given: a payload with invalid JSON structure (missing closing brace)
        byte[] corruptPayload = "{\"broken\":".getBytes();

        // When / Then: the thrown exception is always PayloadDeserializationException, never a raw Jackson type
        assertThatThrownBy(() -> converter.convert(corruptPayload))
            .isExactlyInstanceOf(PayloadDeserializationException.class);
    }

    @Test
    void should_convert_valid_payload() throws Exception {
        // Given: a minimal valid Train payload
        Train train = new Train();
        byte[] validPayload = objectMapper.writeValueAsBytes(train);

        // When
        Train result = converter.convert(validPayload);

        // Then
        assertThat(result).isNotNull();
    }
}


