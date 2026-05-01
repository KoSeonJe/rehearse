package com.rehearse.api.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class DimensionScoreMapConverter implements AttributeConverter<Map<String, DimensionScore>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, DimensionScore> map) {
        if (map == null) {
            return null;
        }
        try {
            return JsonMapperHolder.get().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Map<String,DimensionScore> → JSON 변환 실패", e);
        }
    }

    @Override
    public Map<String, DimensionScore> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            // H2 JSON column returns string literals as double-encoded JSON strings;
            // unwrap to raw JSON object text before deserializing.
            JsonNode node = JsonMapperHolder.get().readTree(json);
            if (node.isTextual()) {
                return JsonMapperHolder.get().readValue(node.asText(), new TypeReference<>() {});
            }
            return JsonMapperHolder.get().treeToValue(node, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON → Map<String,DimensionScore> 변환 실패", e);
        }
    }
}
