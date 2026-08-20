package br.com.clinton.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.clinton.sanitizer.SensitiveDataSanitizer;

public class JsonPayloadFormatter {

    private final ObjectMapper objectMapper;
    private final SensitiveDataSanitizer sanitizer;

    public JsonPayloadFormatter() {

        this.objectMapper =
                new ObjectMapper();

        this.sanitizer =
                new SensitiveDataSanitizer();
    }

    public String format(Object payload) {

        if (payload == null) {
            return "";
        }

        Object sanitizedPayload =
                sanitizer.sanitize(payload);

        try {

            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(
                            sanitizedPayload
                    );

        } catch (JsonProcessingException e) {

            return String.valueOf(
                    sanitizedPayload
            );
        }
    }
}