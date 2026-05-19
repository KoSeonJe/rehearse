package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.RubricIds;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricFamily;
import com.rehearse.api.domain.feedback.rubric.entity.RubricDimension;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class RubricLoader implements RubricCatalog {

    private RubricFamily family;
    private Map<String, Rubric> rubrics;
    private Map<String, String> nameToRef;

    @PostConstruct
    void init() {
        try {
            Map<String, RubricDimension> dimensions = loadDimensions();
            family = new RubricFamily(dimensions);
            nameToRef = buildNameToRefMap(dimensions);

            rubrics = new HashMap<>();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:rubric/*-rubric.yaml");
            for (Resource resource : resources) {
                Rubric rubric = parseRubric(resource);
                rubrics.put(rubric.rubricId(), rubric);
                log.debug("Rubric 로드 완료: rubricId={}", rubric.rubricId());
            }

            validateFallbackRubricPresent();

            log.info("RubricLoader 초기화 완료: dimensions={}, rubrics={}", dimensions.size(), rubrics.size());
        } catch (IOException e) {
            throw new IllegalStateException("Rubric YAML 로딩 실패", e);
        }
    }

    private void validateFallbackRubricPresent() {
        if (!rubrics.containsKey(RubricIds.FALLBACK)) {
            throw new IllegalStateException(
                    "YAML 구조 오류: fallback rubricId='" + RubricIds.FALLBACK +
                    "'에 해당하는 rubric 파일이 없습니다. 로드된 rubricId=" + rubrics.keySet());
        }
    }

    public Rubric resolveFor(Question question, QuestionSet questionSet, Interview interview) {
        boolean resumeTrack = interview.getInterviewTypes().contains(InterviewType.RESUME_BASED);
        InterviewType category = questionSet.getCategory();
        QuestionType questionType = question.getQuestionType();

        String rubricId = resolveRubricId(resumeTrack, category, questionType);

        Rubric resolved = rubrics.get(rubricId);
        if (resolved == null) {
            throw new IllegalStateException(
                    "YAML 구조 오류: resolve된 rubricId='" + rubricId + "'에 해당하는 rubric이 없습니다. " +
                    "로드된 rubricId=" + rubrics.keySet());
        }
        return resolved;
    }

    private String resolveRubricId(boolean resumeTrack, InterviewType category, QuestionType questionType) {
        if (resumeTrack) {
            return RubricIds.RESUME;
        }
        return switch (category) {
            case RESUME_BASED -> RubricIds.RESUME;
            case CS_FUNDAMENTAL -> RubricIds.CONCEPT_CS;
            case LANGUAGE_FRAMEWORK, UI_FRAMEWORK -> RubricIds.CONCEPT_LANG;
            case BEHAVIORAL -> RubricIds.EXPERIENCE_COLLAB;
            default -> questionType.rubricCategory() == RubricCategory.EXPERIENCE
                    ? RubricIds.EXPERIENCE_TECH
                    : RubricIds.FALLBACK;
        };
    }

    public RubricDimension getDimension(String ref) {
        return family.getDimension(ref);
    }

    public Map<String, RubricDimension> getAllDimensions() {
        return family.getDimensions();
    }

    @Override
    public Optional<String> findRefByName(String koreanLabel) {
        if (koreanLabel == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nameToRef.get(koreanLabel));
    }

    private Map<String, String> buildNameToRefMap(Map<String, RubricDimension> dimensions) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, RubricDimension> entry : dimensions.entrySet()) {
            RubricDimension dim = entry.getValue();
            if (dim == null || dim.name() == null) {
                continue;
            }
            String existing = result.putIfAbsent(dim.name(), entry.getKey());
            if (existing != null) {
                throw new IllegalStateException(
                        "중복 dimension name=" + dim.name() + " (refs=" + existing + ", " + entry.getKey() + ")");
            }
        }
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, RubricDimension> loadDimensions() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("classpath:rubric/_dimensions.yaml");
        Yaml yaml = new Yaml();

        Map<String, Object> data;
        try (InputStream is = resource.getInputStream()) {
            data = yaml.load(is);
        }

        Map<String, Object> dimMap = (Map<String, Object>) data.get("dimensions");
        if (dimMap == null) {
            throw new IllegalStateException("_dimensions.yaml 에 'dimensions' 키가 없습니다");
        }

        Map<String, RubricDimension> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : dimMap.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> dimData = (Map<String, Object>) entry.getValue();
            result.put(key, parseDimension(key, dimData));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private RubricDimension parseDimension(String key, Map<String, Object> data) {
        String id = (String) data.get("id");
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        String scope = (String) data.getOrDefault("scope", "general");

        Map<Integer, RubricDimension.ScoringLevel> scoring = new HashMap<>();
        Map<Object, Object> scoringData = (Map<Object, Object>) data.get("scoring");
        if (scoringData != null) {
            for (Map.Entry<Object, Object> se : scoringData.entrySet()) {
                int score = se.getKey() instanceof Integer i ? i : Integer.parseInt(se.getKey().toString());
                Map<String, Object> levelData = (Map<String, Object>) se.getValue();
                String label = (String) levelData.getOrDefault("label", "");
                List<String> observable = (List<String>) levelData.getOrDefault("observable", List.of());
                scoring.put(score, new RubricDimension.ScoringLevel(label, observable));
            }
        }
        return new RubricDimension(id, name, description, scope, Map.copyOf(scoring));
    }

    @SuppressWarnings("unchecked")
    private Rubric parseRubric(Resource resource) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> data;
        try (InputStream is = resource.getInputStream()) {
            data = yaml.load(is);
        }

        String rubricId = (String) data.get("rubric_id");
        String description = (String) data.getOrDefault("description", "");

        List<Map<String, Object>> rawDims = (List<Map<String, Object>>) data.get("uses_dimensions");
        List<DimensionRef> usesDimensions = new ArrayList<>();
        if (rawDims != null) {
            for (Map<String, Object> rd : rawDims) {
                String ref = (String) rd.get("ref");
                double weight = toDouble(rd.getOrDefault("weight", 0.0));
                String conditional = (String) rd.get("conditional");
                usesDimensions.add(new DimensionRef(ref, weight, conditional));
            }
        }

        Map<String, Object> rawRules = (Map<String, Object>) data.get("per_turn_rules");
        Map<String, List<String>> perTurnRules = new HashMap<>();
        if (rawRules != null) {
            for (Map.Entry<String, Object> entry : rawRules.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof List<?> list) {
                    perTurnRules.put(entry.getKey(), list.stream().map(Object::toString).toList());
                } else if (val == null) {
                    perTurnRules.put(entry.getKey(), List.of());
                }
            }
        }

        Map<String, Object> rawLevel = (Map<String, Object>) data.get("level_expectations");
        Map<String, Rubric.LevelExpectation> levelExpectations = new HashMap<>();
        if (rawLevel != null) {
            for (Map.Entry<String, Object> entry : rawLevel.entrySet()) {
                Map<String, Object> le = (Map<String, Object>) entry.getValue();
                levelExpectations.put(entry.getKey(), parseLevelExpectation(le));
            }
        }

        return new Rubric(rubricId, description, List.copyOf(usesDimensions),
                Map.copyOf(perTurnRules), Map.copyOf(levelExpectations));
    }

    @SuppressWarnings("unchecked")
    private Rubric.LevelExpectation parseLevelExpectation(Map<String, Object> le) {
        Object r2Raw = le.get("must_reach_2");
        Object r3Raw = le.get("must_reach_3");
        Object r1Raw = le.get("must_reach_1");

        List<String> mustReach2 = toStringList(r2Raw);
        List<String> mustReach3 = toStringList(r3Raw);
        boolean mustReach1All = "all".equals(r1Raw);

        return new Rubric.LevelExpectation(mustReach2, mustReach3, mustReach1All);
    }

    private List<String> toStringList(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private double toDouble(Object val) {
        if (val instanceof Double d) return d;
        if (val instanceof Integer i) return i.doubleValue();
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
