package com.rehearse.api.infra.ai.persona;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ProfileYamlLoader {

    private static final Map<String, Position> BASE_FILE_MAP = Map.of(
            "backend",      Position.BACKEND,
            "frontend",     Position.FRONTEND,
            "devops",       Position.DEVOPS,
            "data-engineer", Position.DATA_ENGINEER,
            "fullstack",    Position.FULLSTACK
    );

    private static final Map<String, TechStack> OVERLAY_FILE_MAP = Map.of(
            "java-spring",    TechStack.JAVA_SPRING,
            "react-ts",       TechStack.REACT_TS,
            "aws-k8s",        TechStack.AWS_K8S,
            "spark-airflow",  TechStack.SPARK_AIRFLOW,
            "react-spring",   TechStack.REACT_SPRING
    );

    public Map<Position, BaseProfile> loadBaseProfiles() {
        Map<Position, BaseProfile> result = new HashMap<>();
        Yaml yaml = new Yaml();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources("classpath:prompts/base/*.yaml");
            for (Resource resource : resources) {
                String filename = getFilenameWithoutExtension(resource.getFilename());
                Position position = BASE_FILE_MAP.get(filename);

                if (position == null) {
                    log.warn("알 수 없는 base YAML 파일명: {}, 건너뜀", filename);
                    continue;
                }

                try (InputStream is = resource.getInputStream()) {
                    BaseProfile profile = parseBaseProfile(yaml.load(is));
                    result.put(position, profile);
                    log.debug("Base 프로필 로드 완료: position={}, file={}", position, filename);
                }
            }
        } catch (IOException e) {
            log.error("Base YAML 로드 실패", e);
            throw new IllegalStateException("prompts/base/*.yaml 로딩 중 오류 발생", e);
        }

        log.info("Base 프로필 로드 완료: {}개", result.size());
        return Map.copyOf(result);
    }

    public Map<TechStack, StackOverlay> loadStackOverlays() {
        Map<TechStack, StackOverlay> result = new HashMap<>();
        Yaml yaml = new Yaml();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources("classpath:prompts/overlay/**/*.yaml");
            for (Resource resource : resources) {
                String filename = getFilenameWithoutExtension(resource.getFilename());
                TechStack techStack = OVERLAY_FILE_MAP.get(filename);

                if (techStack == null) {
                    log.warn("알 수 없는 overlay YAML 파일명: {}, 건너뜀", filename);
                    continue;
                }

                try (InputStream is = resource.getInputStream()) {
                    StackOverlay overlay = parseStackOverlay(yaml.load(is));
                    result.put(techStack, overlay);
                    log.debug("Stack 오버레이 로드 완료: techStack={}, file={}", techStack, filename);
                }
            }
        } catch (IOException e) {
            log.error("Overlay YAML 로드 실패", e);
            throw new IllegalStateException("prompts/overlay/**/*.yaml 로딩 중 오류 발생", e);
        }

        log.info("Stack 오버레이 로드 완료: {}개", result.size());
        return Map.copyOf(result);
    }

    private BaseProfile parseBaseProfile(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalStateException("Base YAML 내용이 비어있습니다");
        }
        return new BaseProfile(
                Objects.requireNonNull((String) data.get("persona_block"), "persona_block 필수"),
                Objects.requireNonNull((String) data.get("evaluation_perspective"), "evaluation_perspective 필수"),
                Objects.requireNonNull((String) data.get("follow_up_depth"), "follow_up_depth 필수")
        );
    }

    @SuppressWarnings("unchecked")
    private StackOverlay parseStackOverlay(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalStateException("Overlay YAML 내용이 비어있습니다");
        }
        Map<String, Object> persona = (Map<String, Object>) data.get("persona");
        if (persona == null) {
            throw new IllegalStateException("Overlay YAML에 persona 키가 없습니다");
        }
        Map<String, String> interviewTypeGuideMap =
                (Map<String, String>) data.getOrDefault("interview_type_guide", Map.of());

        return new StackOverlay(
                Objects.requireNonNull((String) persona.get("full"), "persona.full 필수"),
                Objects.requireNonNull((String) persona.get("medium"), "persona.medium 필수"),
                Objects.requireNonNull((String) persona.get("minimal"), "persona.minimal 필수"),
                Map.copyOf(interviewTypeGuideMap),
                (String) data.getOrDefault("follow_up_depth_append", ""),
                (String) data.getOrDefault("verbal_expertise", "")
        );
    }

    private String getFilenameWithoutExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
