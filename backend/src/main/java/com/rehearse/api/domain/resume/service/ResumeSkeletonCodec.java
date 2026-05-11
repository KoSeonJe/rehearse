package com.rehearse.api.domain.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeSkeletonCodec {

    private final ObjectMapper objectMapper;

    public ResumeSkeleton deserialize(ResumeSkeletonEntity entity) {
        try {
            ResumeSkeleton parsed = objectMapper.readValue(entity.getSkeletonJson(), ResumeSkeleton.class);
            return ResumeSkeleton.fromEntity(entity, parsed);
        } catch (JsonProcessingException e) {
            log.error("DB에서 ResumeSkeleton 역직렬화 실패: interviewId={}", entity.getInterviewId(), e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }

    public String serialize(ResumeSkeleton skeleton) {
        try {
            return objectMapper.writeValueAsString(skeleton);
        } catch (JsonProcessingException e) {
            log.error("ResumeSkeleton 직렬화 실패", e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }
}
