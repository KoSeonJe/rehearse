package com.rehearse.api.infra.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PdfTextExtractor - 정규화 파이프라인")
class PdfTextExtractorNormalizationTest {

    private PdfTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfTextExtractor();
    }

    @Test
    @DisplayName("collapseWhitespace_reduces_consecutive_spaces_to_single")
    void collapseWhitespace_reduces_consecutive_spaces_to_single() {
        String input = "Java   Spring   Boot";
        String result = extractor.normalize(input);
        assertThat(result).doesNotContain("  ");
        assertThat(result).contains("Java Spring Boot");
    }

    @Test
    @DisplayName("collapseWhitespace_reduces_excessive_newlines_to_double")
    void collapseWhitespace_reduces_excessive_newlines_to_double() {
        String input = "Section A\n\n\n\nSection B";
        String result = extractor.normalize(input);
        assertThat(result).doesNotContain("\n\n\n");
        assertThat(result).contains("Section A");
        assertThat(result).contains("Section B");
    }

    @Test
    @DisplayName("fixKoreanTokenBreaks_merges_split_jamo_characters")
    void fixKoreanTokenBreaks_merges_split_jamo_characters() {
        // PDFBox가 자모(초성/중성/종성)를 분리 추출할 때 공백이 삽입되는 케이스
        String input = "ㄱ ㄴ ㄷ";
        String result = extractor.normalize(input);
        assertThat(result).contains("ㄱㄴㄷ");
    }

    @Test
    @DisplayName("fixKoreanTokenBreaks_converges_within_three_iterations")
    void fixKoreanTokenBreaks_converges_within_three_iterations() {
        // 최악의 경우(3단 체인 자모 깨짐)도 3회 이내 수렴
        String input = "ㄱ ㄴ ㄷ ㄹ ㅁ";
        String result = extractor.normalize(input);
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("ㄱ ㄴ");
    }

    @Test
    @DisplayName("removeHeaderFooter_removes_standalone_page_numbers")
    void removeHeaderFooter_removes_standalone_page_numbers() {
        String input = "경력 사항\n\n1\n\nJava 개발자";
        String result = extractor.normalize(input);
        assertThat(result.lines()
                .map(String::trim)
                .noneMatch(line -> line.equals("1"))).isTrue();
    }

    @Test
    @DisplayName("removeHeaderFooter_removes_standalone_urls")
    void removeHeaderFooter_removes_standalone_urls() {
        String input = "포트폴리오\n\nhttps://github.com/user\n\n프로젝트 설명";
        String result = extractor.normalize(input);
        assertThat(result).doesNotContain("https://github.com/user");
        assertThat(result).contains("포트폴리오");
        assertThat(result).contains("프로젝트 설명");
    }

    @Test
    @DisplayName("removeControlChars_strips_non_printable_characters")
    void removeControlChars_strips_non_printable_characters() {
        String input = "정상텍스트비정상";
        String result = extractor.normalize(input);
        assertThat(result).doesNotContain("");
        assertThat(result).contains("정상텍스트");
        assertThat(result).contains("비정상");
    }

    @Test
    @DisplayName("normalize_trims_to_max_length_after_pipeline")
    void normalize_trims_to_max_length_after_pipeline() {
        String longText = "가".repeat(6000);
        String result = extractor.normalize(longText);
        assertThat(result.length()).isLessThanOrEqualTo(5000);
    }

    @Test
    @DisplayName("normalize_preserves_newlines_tabs_as_whitespace")
    void normalize_preserves_newlines_tabs_as_whitespace() {
        String input = "이름\t홍길동\n경력\t3년";
        String result = extractor.normalize(input);
        assertThat(result).contains("이름");
        assertThat(result).contains("홍길동");
        assertThat(result).contains("경력");
        assertThat(result).contains("3년");
    }
}
