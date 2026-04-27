package com.rehearse.api.domain.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ExtractedResumeSkeleton;
import com.rehearse.api.infra.ai.prompt.ResumeExtractorPromptBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeExtractionService - 이력서 텍스트 → ResumeSkeleton 변환")
class ResumeExtractionServiceTest {

    @InjectMocks
    private ResumeExtractionService service;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    @Mock
    private ResumeExtractorPromptBuilder promptBuilder;

    @Test
    @DisplayName("extract_returns_resume_skeleton_when_llm_response_is_valid")
    void extract_returns_resume_skeleton_when_llm_response_is_valid() {
        // given
        String resumeText = "Java 백엔드 개발자";
        String fileHash = "abc123";

        given(promptBuilder.buildSystemPrompt()).willReturn("system prompt");
        given(promptBuilder.buildUserPrompt(resumeText)).willReturn("user prompt");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockChatResponse());
        given(aiResponseParser.parseOrRetry(any(), eq(ExtractedResumeSkeleton.class), any(), any()))
                .willReturn(mockExtractedSkeleton());

        // when
        ResumeSkeleton result = service.extract(resumeText, fileHash);

        // then
        assertThat(result).isNotNull();
        assertThat(result.fileHash()).isEqualTo(fileHash);
        assertThat(result.resumeId()).isEqualTo("r_test1234");
        assertThat(result.candidateLevel()).isEqualTo(ResumeSkeleton.CandidateLevel.MID);
        assertThat(result.targetDomain()).isEqualTo("backend");
        assertThat(result.projects()).hasSize(1);
    }

    @Test
    @DisplayName("extract_uses_resume_extractor_call_type_for_ai_request")
    void extract_uses_resume_extractor_call_type_for_ai_request() {
        // given
        given(promptBuilder.buildSystemPrompt()).willReturn("system");
        given(promptBuilder.buildUserPrompt(any())).willReturn("user");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockChatResponse());
        given(aiResponseParser.parseOrRetry(any(), eq(ExtractedResumeSkeleton.class), any(), any()))
                .willReturn(mockExtractedSkeleton());

        // when
        service.extract("이력서 내용", "hash");

        // then
        then(aiClient).should().chat(
                argThat(req -> "resume_extractor".equals(req.callType()))
        );
    }

    @Test
    @DisplayName("extract_filters_implicit_cs_topics_below_confidence_threshold")
    void extract_filters_implicit_cs_topics_below_confidence_threshold() {
        // given
        given(promptBuilder.buildSystemPrompt()).willReturn("system");
        given(promptBuilder.buildUserPrompt(any())).willReturn("user");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockChatResponse());

        ExtractedResumeSkeleton rawWithLowConfidence = mockExtractedSkeletonWithLowConfidenceTopic();
        given(aiResponseParser.parseOrRetry(any(), eq(ExtractedResumeSkeleton.class), any(), any()))
                .willReturn(rawWithLowConfidence);

        // when
        ResumeSkeleton result = service.extract("이력서", "hash");

        // then — confidence 0.2는 0.3 미만이므로 필터링됨
        assertThat(result.projects()).hasSize(1);
        assertThat(result.projects().get(0).implicitCsTopics()).isEmpty();
    }

    @Test
    @DisplayName("extract_defaults_candidate_level_to_junior_when_llm_returns_unknown_value")
    void extract_defaults_candidate_level_to_junior_when_llm_returns_unknown_value() {
        // given
        given(promptBuilder.buildSystemPrompt()).willReturn("system");
        given(promptBuilder.buildUserPrompt(any())).willReturn("user");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockChatResponse());

        ExtractedResumeSkeleton rawWithUnknownLevel = mockExtractedSkeletonWithUnknownLevel();
        given(aiResponseParser.parseOrRetry(any(), eq(ExtractedResumeSkeleton.class), any(), any()))
                .willReturn(rawWithUnknownLevel);

        // when
        ResumeSkeleton result = service.extract("이력서", "hash");

        // then
        assertThat(result.candidateLevel()).isEqualTo(ResumeSkeleton.CandidateLevel.JUNIOR);
    }

    @Test
    @DisplayName("extract_maps_priority_map_from_llm_response")
    void extract_maps_priority_map_from_llm_response() {
        // given
        given(promptBuilder.buildSystemPrompt()).willReturn("system");
        given(promptBuilder.buildUserPrompt(any())).willReturn("user");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockChatResponse());
        given(aiResponseParser.parseOrRetry(any(), eq(ExtractedResumeSkeleton.class), any(), any()))
                .willReturn(mockExtractedSkeleton());

        // when
        ResumeSkeleton result = service.extract("이력서", "hash");

        // then
        assertThat(result.interrogationPriorityMap()).containsKey("high");
        assertThat(result.interrogationPriorityMap().get("high")).contains("p1_c1");
    }

    // ---- helpers ----

    private ChatResponse mockChatResponse() {
        return new ChatResponse("{}", null, "openai", "gpt-4o-mini", false, false);
    }

    private ExtractedResumeSkeleton mockExtractedSkeleton() {
        try {
            String json = """
                    {
                      "resume_id": "r_test1234",
                      "candidate_level": "mid",
                      "target_domain": "backend",
                      "projects": [
                        {
                          "project_id": "p1",
                          "claims": [
                            {
                              "claim_id": "p1_c1",
                              "text": "Redis 캐시 도입으로 응답 시간 50% 단축",
                              "claim_type": "IMPACT_METRIC",
                              "priority": "high",
                              "depth_hooks": ["Redis", "캐시 전략"]
                            }
                          ],
                          "implicit_cs_topics": [
                            {
                              "topic": "caching",
                              "confidence": 0.9,
                              "interrogation_chain": [
                                {"level": 1, "type": "WHAT", "question": "캐시란 무엇인가요?"},
                                {"level": 2, "type": "HOW", "question": "어떻게 구현했나요?"},
                                {"level": 3, "type": "WHY_MECH", "question": "왜 그 방식을 선택했나요?"},
                                {"level": 4, "type": "TRADEOFF", "question": "트레이드오프는 무엇인가요?"}
                              ]
                            }
                          ]
                        }
                      ],
                      "interrogation_priority_map": {
                        "high": ["p1_c1"],
                        "medium": [],
                        "low": []
                      }
                    }
                    """;
            return new ObjectMapper().readValue(json, ExtractedResumeSkeleton.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ExtractedResumeSkeleton mockExtractedSkeletonWithLowConfidenceTopic() {
        try {
            String json = """
                    {
                      "resume_id": "r_low",
                      "candidate_level": "junior",
                      "target_domain": "backend",
                      "projects": [
                        {
                          "project_id": "p1",
                          "claims": [],
                          "implicit_cs_topics": [
                            {
                              "topic": "low-confidence-topic",
                              "confidence": 0.2,
                              "interrogation_chain": [
                                {"level": 1, "type": "WHAT", "question": "Q1"},
                                {"level": 2, "type": "HOW", "question": "Q2"},
                                {"level": 3, "type": "WHY_MECH", "question": "Q3"},
                                {"level": 4, "type": "TRADEOFF", "question": "Q4"}
                              ]
                            }
                          ]
                        }
                      ],
                      "interrogation_priority_map": {}
                    }
                    """;
            return new ObjectMapper().readValue(json, ExtractedResumeSkeleton.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ExtractedResumeSkeleton mockExtractedSkeletonWithUnknownLevel() {
        try {
            String json = """
                    {
                      "resume_id": "r_unknown",
                      "candidate_level": "expert",
                      "target_domain": "backend",
                      "projects": [],
                      "interrogation_priority_map": {}
                    }
                    """;
            return new ObjectMapper().readValue(json, ExtractedResumeSkeleton.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
