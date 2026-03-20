package com.rehearse.api.infra.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@ConditionalOnMissingBean(WhisperService.class)
public class MockSttService implements SttService {

    @PostConstruct
    void init() {
        log.warn("=== MockSttService 활성화: OpenAI API 키 없이 Mock 텍스트로 동작합니다 ===");
    }

    @Override
    public String transcribe(MultipartFile audioFile) {
        log.info("[Mock] transcribe 호출 - fileName={}, size={}bytes",
                audioFile.getOriginalFilename(), audioFile.getSize());
        return "[Mock] 저는 Spring Boot와 JPA를 활용한 백엔드 개발 경험이 있으며, "
                + "대규모 트래픽 처리를 위한 캐싱 전략과 데이터베이스 최적화에 집중해왔습니다. "
                + "특히 최근 프로젝트에서는 Redis를 활용한 세션 관리와 "
                + "QueryDSL을 통한 동적 쿼리 최적화를 수행했습니다.";
    }
}
