package br.com.clinton.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonPayloadFormatter {

    private final ObjectMapper objectMapper;

    public JsonPayloadFormatter() {
        this.objectMapper = new ObjectMapper();
    }

    public String format(Object payload) {

        if (payload == null) {
            return "";
        }

        try {
            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(payload);

        } catch (JsonProcessingException e) {
            return String.valueOf(payload);
        }
    }
}