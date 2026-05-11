package com.rehearse.api.domain.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeSkeletonCodec - JSON 직렬화/역직렬화 + 실패 시 BusinessException")
class ResumeSkeletonCodecTest {

    @InjectMocks
    private ResumeSkeletonCodec codec;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("deserialize_엔티티_JSON을_ResumeSkeleton으로_파싱")
    void deserialize_returns_parsed_skeleton_when_json_valid() throws Exception {
        ResumeSkeletonEntity entity = createEntity("abc123");
        ResumeSkeleton parsed = createSkeleton("abc123");

        given(objectMapper.readValue(anyString(), eq(ResumeSkeleton.class))).willReturn(parsed);

        ResumeSkeleton result = codec.deserialize(entity);

        assertThat(result.fileHash()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("deserialize_JSON_파싱_실패_시_BusinessException_PARSE_FAILED")
    void deserialize_throws_business_exception_when_json_is_malformed() throws Exception {
        ResumeSkeletonEntity entity = createEntity("abc123");

        given(objectMapper.readValue(anyString(), eq(ResumeSkeleton.class)))
                .willThrow(new JsonProcessingException("parse error") {});

        assertThatThrownBy(() -> codec.deserialize(entity))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
    }

    @Test
    @DisplayName("serialize_ResumeSkeleton을_JSON으로_변환")
    void serialize_returns_json_when_skeleton_valid() throws Exception {
        ResumeSkeleton skeleton = createSkeleton("abc123");

        given(objectMapper.writeValueAsString(skeleton)).willReturn("{\"fileHash\":\"abc123\"}");

        String result = codec.serialize(skeleton);

        assertThat(result).contains("abc123");
    }

    @Test
    @DisplayName("serialize_직렬화_실패_시_BusinessException_PARSE_FAILED")
    void serialize_throws_business_exception_when_serialization_fails() throws Exception {
        ResumeSkeleton skeleton = createSkeleton("abc123");

        given(objectMapper.writeValueAsString(skeleton))
                .willThrow(new JsonProcessingException("serialize error") {});

        assertThatThrownBy(() -> codec.serialize(skeleton))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
    }

    private ResumeSkeletonEntity createEntity(String fileHash) {
        return ResumeSkeletonEntity.builder()
                .interviewId(1L)
                .fileHash(fileHash)
                .candidateLevel("JUNIOR")
                .targetDomain("backend")
                .skeletonJson("{}")
                .build();
    }

    private ResumeSkeleton createSkeleton(String fileHash) {
        return new ResumeSkeleton(
                "r_test",
                fileHash,
                CandidateLevel.JUNIOR,
                "backend",
                List.of(),
                Map.of()
        );
    }
}
