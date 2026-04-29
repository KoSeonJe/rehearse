package com.rehearse.api.domain.feedback.rubric.nonverbal;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class NonverbalContextWeightsLoader {

    private static final String RESOURCE_PATH = "classpath:rubric/nonverbal-context-weights.yaml";
    private static final NonverbalContextWeight FALLBACK = new NonverbalContextWeight(1.0, true);

    private Map<String, Object> weights = Map.of();

    public NonverbalContextWeightsLoader() {
        load();
    }

    @PostConstruct
    void init() {
        load();
    }

    public NonverbalContextWeight resolve(String category, String track, String mode, String difficulty) {
        Map<String, Object> data = objectMap(weights.get("weights"));
        NonverbalContextWeight defaultWeight = parseWeight(objectMap(data.get("default")), FALLBACK);

        NonverbalContextWeight trackModeWeight = resolveTrackMode(data, track, mode);
        if (trackModeWeight != null) {
            return trackModeWeight;
        }

        NonverbalContextWeight categoryWeight = resolveNamed(data, "category", category);
        if (categoryWeight != null) {
            return categoryWeight;
        }

        NonverbalContextWeight difficultyWeight = resolveNamed(data, "difficulty", difficulty);
        return difficultyWeight != null ? difficultyWeight : defaultWeight;
    }

    private NonverbalContextWeight resolveTrackMode(Map<String, Object> data, String track, String mode) {
        if (track == null || mode == null) {
            return null;
        }
        Map<String, Object> tracks = objectMap(data.get("track_mode"));
        Map<String, Object> modes = objectMap(tracks.get(track));
        Map<String, Object> raw = objectMap(modes.get(mode));
        return raw.isEmpty() ? null : parseWeight(raw, FALLBACK);
    }

    private NonverbalContextWeight resolveNamed(Map<String, Object> data, String group, String key) {
        if (key == null) {
            return null;
        }
        Map<String, Object> groupData = objectMap(data.get(group));
        Map<String, Object> raw = objectMap(groupData.get(key));
        return raw.isEmpty() ? null : parseWeight(raw, FALLBACK);
    }

    private NonverbalContextWeight parseWeight(Map<String, Object> raw, NonverbalContextWeight fallback) {
        if (raw.isEmpty()) {
            return fallback;
        }
        double multiplier = toDouble(raw.getOrDefault("multiplier", fallback.multiplier()));
        boolean composureEnabled = toBoolean(raw.getOrDefault("composure_enabled", fallback.composureEnabled()));
        return new NonverbalContextWeight(multiplier, composureEnabled);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private double toDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return 1.0;
        }
        return Double.parseDouble(raw.toString());
    }

    private boolean toBoolean(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        return raw == null || Boolean.parseBoolean(raw.toString());
    }

    private void load() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(RESOURCE_PATH);
        if (!resource.exists()) {
            weights = Map.of();
            return;
        }

        Yaml yaml = new Yaml();
        try (InputStream is = resource.getInputStream()) {
            Object loaded = yaml.load(is);
            weights = objectMap(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Nonverbal context weights YAML 로딩 실패", e);
        }
    }
}
