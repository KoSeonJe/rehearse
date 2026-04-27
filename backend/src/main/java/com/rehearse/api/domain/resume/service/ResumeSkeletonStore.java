package com.rehearse.api.domain.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
import com.rehearse.api.domain.resume.repository.ResumeSkeletonRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeSkeletonStore {

    private final ResumeSkeletonRepository skeletonRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<ResumeSkeleton> findByInterviewId(Long interviewId) {
        return skeletonRepository.findByInterviewId(interviewId)
                .map(entity -> deserialize(entity));
    }

    @Transactional
    public void save(Long interviewId, ResumeSkeleton skeleton) {
        String skeletonJson = serialize(skeleton);
        ResumeSkeletonEntity entity = ResumeSkeletonEntity.builder()
                .interviewId(interviewId)
                .fileHash(skeleton.fileHash())
                .candidateLevel(skeleton.candidateLevel().name())
                .targetDomain(skeleton.targetDomain())
                .skeletonJson(skeletonJson)
                .build();
        try {
            skeletonRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.warn("이력서 중복 저장 감지, DB 재조회: interviewId={}", interviewId);
            skeletonRepository.findByInterviewId(interviewId)
                    .orElseThrow(() -> new BusinessException(AiErrorCode.PARSE_FAILED));
        }
    }

    private ResumeSkeleton deserialize(ResumeSkeletonEntity entity) {
        try {
            ResumeSkeleton parsed = objectMapper.readValue(entity.getSkeletonJson(), ResumeSkeleton.class);
            return ResumeSkeleton.fromEntity(entity, parsed);
        } catch (JsonProcessingException e) {
            log.error("DB에서 ResumeSkeleton 역직렬화 실패: interviewId={}", entity.getInterviewId(), e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }

    private String serialize(ResumeSkeleton skeleton) {
        try {
            return objectMapper.writeValueAsString(skeleton);
        } catch (JsonProcessingException e) {
            log.error("ResumeSkeleton 직렬화 실패", e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }
}
