package com.rehearse.api.domain.resume.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeSkeleton JSON 역직렬화 호환 - 폐기된 interrogationPriorityMap 필드 무시")
class ResumeSkeletonJsonCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("폐기 필드가 포함된 과거 JSON payload 입력 시")
    class LegacyPayload {

        @Test
        @DisplayName("interrogationPriorityMap 필드는 무시되고 record 생성에 성공한다")
        void should_ignore_interrogation_priority_map_when_deserializing_legacy_json() throws Exception {
            String legacyJson = """
                    {
                      "resumeId": "resume-1",
                      "fileHash": "hash-abc",
                      "candidateLevel": "JUNIOR",
                      "targetDomain": "backend",
                      "projects": [
                        {
                          "projectId": "p1",
                          "projectName": "결제 서비스",
                          "techStack": ["Java", "Spring"],
                          "role": "BE",
                          "architecture": "MSA",
                          "decisions": ["Kafka 도입"]
                        }
                      ],
                      "interrogationPriorityMap": {
                        "p1": ["claim-1", "claim-2"]
                      }
                    }
                    """;

            ResumeSkeleton skeleton = objectMapper.readValue(legacyJson, ResumeSkeleton.class);

            assertThat(skeleton.resumeId()).isEqualTo("resume-1");
            assertThat(skeleton.fileHash()).isEqualTo("hash-abc");
            assertThat(skeleton.candidateLevel()).isEqualTo(CandidateLevel.JUNIOR);
            assertThat(skeleton.targetDomain()).isEqualTo("backend");
            assertThat(skeleton.projects()).hasSize(1);
            assertThat(skeleton.projects().get(0).projectId()).isEqualTo("p1");
        }
    }
}
