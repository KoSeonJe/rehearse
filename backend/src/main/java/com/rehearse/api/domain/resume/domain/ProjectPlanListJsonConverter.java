package com.rehearse.api.domain.resume.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter(autoApply = false)
public class ProjectPlanListJsonConverter implements AttributeConverter<List<ProjectPlan>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<ProjectPlan>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<ProjectPlan> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ProjectPlan 리스트 직렬화 실패", e);
        }
    }

    @Override
    public List<ProjectPlan> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ProjectPlan 리스트 역직렬화 실패", e);
        }
    }
}
