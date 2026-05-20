package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeExtractionService — port 호출 결과를 도메인 ResumeSkeleton 으로 매핑")
class ResumeExtractionServiceTest {

    @InjectMocks
    private ResumeExtractionService extractionService;

    @Mock
    private ResumeSkeletonExtractor resumeSkeletonExtractor;

    @Nested
    @DisplayName("extract")
    class Extract {

        @Test
        @DisplayName("port 반환 GeneratedResumeSkeleton → 도메인 ResumeSkeleton 매핑 + fileHash 보존")
        void extract_mapsGeneratedSkeletonToDomain() {
            byte[] pdf = "pdf-bytes".getBytes();
            String fileHash = "hash-1";
            GeneratedResumeSkeleton generated = new GeneratedResumeSkeleton(
                    "resume-1",
                    "MID",
                    "backend",
                    List.of(new GeneratedResumeSkeleton.GeneratedProject(
                            "p1",
                            "주문 캐싱 개선",
                            List.of("Redis", "MySQL"),
                            "백엔드",
                            "Cache-Aside",
                            List.of("TTL 5분"),
                            null
                    ))
            );
            given(resumeSkeletonExtractor.extract(any(byte[].class), eq(fileHash))).willReturn(generated);

            ResumeSkeleton skeleton = extractionService.extract(pdf, fileHash);

            assertThat(skeleton.resumeId()).isEqualTo("resume-1");
            assertThat(skeleton.fileHash()).isEqualTo(fileHash);
            assertThat(skeleton.candidateLevel()).isEqualTo(CandidateLevel.MID);
            assertThat(skeleton.targetDomain()).isEqualTo("backend");
            assertThat(skeleton.projects()).hasSize(1);
            Project project = skeleton.projects().get(0);
            assertThat(project.projectId()).isEqualTo("p1");
            assertThat(project.projectName()).isEqualTo("주문 캐싱 개선");
            assertThat(project.techStack()).containsExactly("Redis", "MySQL");
            assertThat(project.architecture()).isEqualTo("Cache-Aside");
            assertThat(project.decisions()).containsExactly("TTL 5분");
        }

        @Test
        @DisplayName("candidate_level 이 null 이면 JUNIOR 로 fallback")
        void extract_fallsBackToJunior_whenCandidateLevelNull() {
            GeneratedResumeSkeleton generated = new GeneratedResumeSkeleton(
                    "resume-2", null, "backend", List.of());
            given(resumeSkeletonExtractor.extract(any(byte[].class), eq("h"))).willReturn(generated);

            ResumeSkeleton skeleton = extractionService.extract("pdf".getBytes(), "h");

            assertThat(skeleton.candidateLevel()).isEqualTo(CandidateLevel.JUNIOR);
        }

        @Test
        @DisplayName("알 수 없는 candidate_level 문자열은 JUNIOR 로 fallback")
        void extract_fallsBackToJunior_whenCandidateLevelUnknown() {
            GeneratedResumeSkeleton generated = new GeneratedResumeSkeleton(
                    "resume-3", "PRINCIPAL", "backend", List.of());
            given(resumeSkeletonExtractor.extract(any(byte[].class), eq("h"))).willReturn(generated);

            ResumeSkeleton skeleton = extractionService.extract("pdf".getBytes(), "h");

            assertThat(skeleton.candidateLevel()).isEqualTo(CandidateLevel.JUNIOR);
        }

        @Test
        @DisplayName("project.projectName 이 null 이면 빈 문자열로 매핑")
        void extract_mapsNullProjectNameToEmptyString() {
            GeneratedResumeSkeleton generated = new GeneratedResumeSkeleton(
                    "resume-4",
                    "JUNIOR",
                    "backend",
                    List.of(new GeneratedResumeSkeleton.GeneratedProject(
                            "p1", null, List.of(), "백엔드", "", List.of(), null))
            );
            given(resumeSkeletonExtractor.extract(any(byte[].class), eq("h"))).willReturn(generated);

            ResumeSkeleton skeleton = extractionService.extract("pdf".getBytes(), "h");

            assertThat(skeleton.projects().get(0).projectName()).isEmpty();
        }
    }
}
