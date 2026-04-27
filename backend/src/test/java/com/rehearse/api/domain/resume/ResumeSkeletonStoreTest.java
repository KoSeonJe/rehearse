package com.rehearse.api.domain.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
import com.rehearse.api.domain.resume.repository.ResumeSkeletonRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeSkeletonStore - DB read/write + 직렬화/역직렬화")
class ResumeSkeletonStoreTest {

    @InjectMocks
    private ResumeSkeletonStore store;

    @Mock
    private ResumeSkeletonRepository skeletonRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("findByInterviewId_returns_deserialized_skeleton_when_entity_exists")
    void findByInterviewId_returns_deserialized_skeleton_when_entity_exists() throws Exception {
        ResumeSkeletonEntity entity = createEntity("abc123", "JUNIOR");
        ResumeSkeleton parsed = createSkeleton("abc123");

        given(skeletonRepository.findByInterviewId(1L)).willReturn(Optional.of(entity));
        given(objectMapper.readValue(anyString(), eq(ResumeSkeleton.class))).willReturn(parsed);

        Optional<ResumeSkeleton> result = store.findByInterviewId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().fileHash()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("findByInterviewId_returns_empty_when_no_entity")
    void findByInterviewId_returns_empty_when_no_entity() {
        given(skeletonRepository.findByInterviewId(99L)).willReturn(Optional.empty());

        Optional<ResumeSkeleton> result = store.findByInterviewId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByInterviewId_throws_business_exception_when_json_is_malformed")
    void findByInterviewId_throws_business_exception_when_json_is_malformed() throws Exception {
        ResumeSkeletonEntity entity = createEntity("abc123", "JUNIOR");

        given(skeletonRepository.findByInterviewId(1L)).willReturn(Optional.of(entity));
        given(objectMapper.readValue(anyString(), eq(ResumeSkeleton.class)))
                .willThrow(new JsonProcessingException("parse error") {});

        assertThatThrownBy(() -> store.findByInterviewId(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
    }

    @Test
    @DisplayName("save_persists_entity_with_correct_fields")
    void save_persists_entity_with_correct_fields() throws Exception {
        ResumeSkeleton skeleton = createSkeleton("abc123");
        given(objectMapper.writeValueAsString(skeleton)).willReturn("{\"fileHash\":\"abc123\"}");

        store.save(1L, skeleton);

        then(skeletonRepository).should().save(any(ResumeSkeletonEntity.class));
    }

    @Test
    @DisplayName("save_throws_business_exception_when_serialization_fails")
    void save_throws_business_exception_when_serialization_fails() throws Exception {
        ResumeSkeleton skeleton = createSkeleton("abc123");
        given(objectMapper.writeValueAsString(skeleton))
                .willThrow(new JsonProcessingException("serialize error") {});

        assertThatThrownBy(() -> store.save(1L, skeleton))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
    }

    private ResumeSkeletonEntity createEntity(String fileHash, String level) {
        return ResumeSkeletonEntity.builder()
                .interviewId(1L)
                .fileHash(fileHash)
                .candidateLevel(level)
                .targetDomain("backend")
                .skeletonJson("{}")
                .build();
    }

    private ResumeSkeleton createSkeleton(String fileHash) {
        return new ResumeSkeleton(
                "r_test",
                fileHash,
                ResumeSkeleton.CandidateLevel.JUNIOR,
                "backend",
                List.of(),
                Map.of()
        );
    }
}
