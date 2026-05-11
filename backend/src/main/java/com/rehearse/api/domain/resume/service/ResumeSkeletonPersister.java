package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
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
public class ResumeSkeletonPersister {

    private final ResumeSkeletonRepository skeletonRepository;
    private final ResumeSkeletonCodec resumeSkeletonCodec;

    @Transactional(readOnly = true)
    public Optional<ResumeSkeleton> findByInterviewId(Long interviewId) {
        return skeletonRepository.findByInterviewId(interviewId)
                .map(resumeSkeletonCodec::deserialize);
    }

    @Transactional
    public void save(Long interviewId, ResumeSkeleton skeleton) {
        String skeletonJson = resumeSkeletonCodec.serialize(skeleton);
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
}
