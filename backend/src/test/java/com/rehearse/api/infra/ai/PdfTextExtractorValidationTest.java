package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PdfTextExtractor - 파일 검증")
class PdfTextExtractorValidationTest {

    private PdfTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfTextExtractor();
    }

    @Test
    @DisplayName("extract_throws_when_file_is_empty")
    void extract_throws_when_file_is_empty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "resume", "resume.pdf", "application/pdf", new byte[0]
        );

        assertThatThrownBy(() -> extractor.extract(emptyFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.INVALID_FILE_EMPTY.getCode());
                });
    }

    @Test
    @DisplayName("extract_throws_when_file_exceeds_5mb")
    void extract_throws_when_file_exceeds_5mb() {
        byte[] oversized = new byte[5_242_881];
        oversized[0] = '%'; oversized[1] = 'P'; oversized[2] = 'D'; oversized[3] = 'F';
        MockMultipartFile bigFile = new MockMultipartFile(
                "resume", "resume.pdf", "application/pdf", oversized
        );

        assertThatThrownBy(() -> extractor.extract(bigFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.INVALID_FILE_SIZE.getCode());
                });
    }

    @Test
    @DisplayName("extract_throws_when_content_type_is_not_pdf")
    void extract_throws_when_content_type_is_not_pdf() {
        MockMultipartFile wordFile = new MockMultipartFile(
                "resume", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3, 4}
        );

        assertThatThrownBy(() -> extractor.extract(wordFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.INVALID_FILE_TYPE.getCode());
                });
    }

    @Test
    @DisplayName("extract_throws_when_magic_bytes_are_invalid")
    void extract_throws_when_magic_bytes_are_invalid() {
        byte[] fakeBytes = {0x50, 0x4B, 0x03, 0x04, 0x00};
        MockMultipartFile fakeFile = new MockMultipartFile(
                "resume", "fake.pdf", "application/pdf", fakeBytes
        );

        assertThatThrownBy(() -> extractor.extract(fakeFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.INVALID_FILE_MAGIC_BYTES.getCode());
                });
    }

    @Test
    @DisplayName("extract_throws_when_content_type_is_null")
    void extract_throws_when_content_type_is_null() {
        MockMultipartFile noTypeFile = new MockMultipartFile(
                "resume", "resume.pdf", null, new byte[]{1, 2, 3, 4}
        );

        assertThatThrownBy(() -> extractor.extract(noTypeFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.INVALID_FILE_TYPE.getCode());
                });
    }
}
