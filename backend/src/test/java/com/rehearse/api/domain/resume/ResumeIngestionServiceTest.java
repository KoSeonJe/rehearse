package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.service.ResumeExtractionService;
import com.rehearse.api.domain.resume.service.ResumeIngestionService;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeIngestionService - 이력서 수집 비즈니스 로직")
class ResumeIngestionServiceTest {

    @InjectMocks
    private ResumeIngestionService service;

    @Mock
    private ResumeExtractionService extractionService;

    @Mock
    private ResumeSkeletonPersister skeletonStore;

    private static final String TEST_HASH = "abc123hash";

    @Test
    @DisplayName("ingestExtractedText_uses_existing_normalized_text_and_file_hash")
    void ingestExtractedText_usesExistingNormalizedTextAndFileHash() {
        String longText = "Java 백엔드 개발자. ".repeat(10);
        ResumeSkeleton skeleton = createMockSkeleton(TEST_HASH);

        given(skeletonStore.findByInterviewId(1L)).willReturn(Optional.empty());
        given(extractionService.extract(longText, TEST_HASH)).willReturn(skeleton);

        ResumeSkeleton result = service.ingestExtractedText(1L, longText, TEST_HASH);

        assertThat(result).isEqualTo(skeleton);
        then(skeletonStore).should().save(1L, skeleton);
    }

    private ResumeSkeleton createMockSkeleton(String fileHash) {
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
