package com.rehearse.api.domain.feedback.rubric.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class NonverbalImprovementActionsLoader {

    private static final String RESOURCE_PATH = "classpath:rubric/nonverbal-improvement-actions.yaml";

    private Map<String, Object> actionData = Map.of();

    public NonverbalImprovementActionsLoader() {
        load();
    }

    @PostConstruct
    void init() {
        load();
    }

    public List<String> resolve(String dimension, double averageScore) {
        Map<String, Object> actions = objectMap(actionData.get("actions"));
        Map<String, Object> dimensionActions = objectMap(actions.get(dimension));
        if (dimensionActions.isEmpty()) {
            return List.of();
        }

        String band = averageScore < 2.0 ? "level_1_to_2" : "level_2_to_3";
        return toStringList(dimensionActions.get(band));
    }

    private void load() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(RESOURCE_PATH);
        if (!resource.exists()) {
            actionData = Map.of();
            return;
        }

        Yaml yaml = new Yaml();
        try (InputStream is = resource.getInputStream()) {
            actionData = objectMap(yaml.load(is));
        } catch (IOException e) {
            throw new IllegalStateException("Nonverbal improvement actions YAML 로딩 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private List<String> toStringList(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
