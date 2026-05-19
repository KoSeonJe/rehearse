package com.rehearse.api.infra.ai.dto;

import java.util.Map;

public record JsonSchemaSpec(String name, Map<String, Object> schema) {

    public JsonSchemaSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("JsonSchemaSpec.name 은 비어있을 수 없습니다.");
        }
        if (schema == null || schema.isEmpty()) {
            throw new IllegalArgumentException("JsonSchemaSpec.schema 는 비어있을 수 없습니다.");
        }
    }
}
