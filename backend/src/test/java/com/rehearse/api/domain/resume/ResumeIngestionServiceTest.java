package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.service.ResumeExtractionService;
import com.rehearse.api.global.util.FileHasher;
import com.rehearse.api.domain.resume.service.ResumeIngestionService;
import com.rehearse.api.domain.resume.service.ResumeSkeletonRuntimeCache;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeIngestionService - 이력서 수집 비즈니스 로직")
class ResumeIngestionServiceTest {

    @InjectMocks
    private ResumeIngestionService service;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private ResumeExtractionService extractionService;

    @Mock
    private FileHasher fileHasher;

    @Mock
    private ResumeSkeletonPersister skeletonStore;

    @Mock
    private ResumeSkeletonRuntimeCache skeletonCache;

    private static final byte[] VALID_PDF_BYTES = {
            '%', 'P', 'D', 'F', '-', '1', '.', '4'
    };
    private static final String TEST_HASH = "abc123hash";

    @Test
    @DisplayName("ingest_throws_empty_text_exception_when_extracted_text_is_blank")
    void ingest_throws_empty_text_exception_when_extracted_text_is_blank() {
        MockMultipartFile file = createValidPdfFile(VALID_PDF_BYTES);
        given(fileHasher.hash(any())).willReturn(TEST_HASH);
        given(skeletonCache.read(1L, TEST_HASH)).willReturn(null);
        given(skeletonStore.findByInterviewId(1L)).willReturn(Optional.empty());
        given(pdfTextExtractor.extract(file)).willReturn("   ");

        assertThatThrownBy(() -> service.ingest(1L, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.EMPTY_RESUME_TEXT.getCode());
                });

        then(extractionService).should(never()).extract(anyString(), anyString());
    }

    @Test
    @DisplayName("ingest_throws_empty_text_exception_when_extracted_text_is_null")
    void ingest_throws_empty_text_exception_when_extracted_text_is_null() {
        MockMultipartFile file = createValidPdfFile(VALID_PDF_BYTES);
        given(fileHasher.hash(any())).willReturn(TEST_HASH);
        given(skeletonCache.read(1L, TEST_HASH)).willReturn(null);
        given(skeletonStore.findByInterviewId(1L)).willReturn(Optional.empty());
        given(pdfTextExtractor.extract(file)).willReturn(null);

        assertThatThrownBy(() -> service.ingest(1L, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.EMPTY_RESUME_TEXT.getCode());
                });
    }

    @Test
    @DisplayName("ingest_throws_empty_text_exception_when_extracted_text_is_too_short")
    void ingest_throws_empty_text_exception_when_extracted_text_is_too_short() {
        MockMultipartFile file = createValidPdfFile(VALID_PDF_BYTES);
        given(fileHasher.hash(any())).willReturn(TEST_HASH);
        given(skeletonCache.read(1L, TEST_HASH)).willReturn(null);
        given(skeletonStore.findByInterviewId(1L)).willReturn(Optional.empty());
        given(pdfTextExtractor.extract(file)).willReturn("짧은텍스트");

        assertThatThrownBy(() -> service.ingest(1L, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.EMPTY_RESUME_TEXT.getCode());
                });
    }

    @Test
    @DisplayName("ingest_skips_llm_call_when_db_cache_hit_with_matching_hash")
    void ingest_skips_llm_call_when_db_cache_hit_with_matching_hash() {
        MockMultipartFile file = createValidPdfFile(VALID_PDF_BYTES);
        ResumeSkeleton skeleton = createMockSkeleton(TEST_HASH);

        given(fileHasher.hash(any())).willReturn(TEST_HASH);
        given(skeletonCache.read(1L, TEST_HASH)).willReturn(null);
        given(skeletonStore.findByInterviewId(1L)).willReturn(Optional.of(skeleton));

        service.ingest(1L, file);

        then(pdfTextExtractor).should(never()).extract(any());
        then(extractionService).should(never()).extract(anyString(), anyString());
    }

    @Test
    @DisplayName("ingest_returns_cached_skeleton_when_memory_cache_hit")
    void ingest_returns_cached_skeleton_when_memory_cache_hit() {
        MockMultipartFile file = createValidPdfFile(VALID_PDF_BYTES);
        ResumeSkeleton skeleton = createMockSkeleton(TEST_HASH);

        given(fileHasher.hash(any())).willReturn(TEST_HASH);
        given(skeletonCache.read(1L, TEST_HASH)).willReturn(skeleton);

        ResumeSkeleton result = service.ingest(1L, file);

        assertThat(result).isEqualTo(skeleton);
        then(skeletonStore).should(never()).findByInterviewId(any());
        then(extractionService).should(never()).extract(anyString(), anyString());
    }

    @Test
    @DisplayName("ingest_calls_extraction_service_when_no_cache_and_text_is_sufficient")
    void ingest_calls_extraction_service_when_no_cache_and_text_is_sufficient() {
        String longText = "Java 백엔드 개발자. ".repeat(10);
        MockMultipartFile file = createValidPdfFile(VALID_PDF_BYTES);
        ResumeSkeleton skeleton = createMockSkeleton(TEST_HASH);

        given(fileHasher.hash(any())).willReturn(TEST_HASH);
        given(skeletonCache.read(1L, TEST_HASH)).willReturn(null);
        given(skeletonStore.findByInterviewId(1L)).willReturn(Optional.empty());
        given(pdfTextExtractor.extract(file)).willReturn(longText);
        given(extractionService.extract(anyString(), anyString())).willReturn(skeleton);

        service.ingest(1L, file);

        then(extractionService).should().extract(longText, TEST_HASH);
        then(skeletonStore).should().save(1L, skeleton);
        then(skeletonCache).should().write(1L, skeleton);
    }

    @Test
    @DisplayName("ingest_skips_llm_call_when_db_hit_but_hash_mismatch")
    void ingest_skips_llm_call_when_db_hit_but_hash_mismatch() {
        String longText = "Java 백엔드 개발자. ".repeat(10);
        MockMultipartFile file = createValidPdfFile(VALID_PDF_BYTES);
        ResumeSkeleton dbSkeleton = createMockSkeleton("different_hash");
        ResumeSkeleton extractedSkeleton = createMockSkeleton(TEST_HASH);

        given(fileHasher.hash(any())).willReturn(TEST_HASH);
        given(skeletonCache.read(1L, TEST_HASH)).willReturn(null);
        given(skeletonStore.findByInterviewId(1L)).willReturn(Optional.of(dbSkeleton));
        given(pdfTextExtractor.extract(file)).willReturn(longText);
        given(extractionService.extract(anyString(), anyString())).willReturn(extractedSkeleton);

        service.ingest(1L, file);

        then(extractionService).should().extract(longText, TEST_HASH);
    }

    private MockMultipartFile createValidPdfFile(byte[] content) {
        return new MockMultipartFile("resume", "resume.pdf", "application/pdf", content);
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
