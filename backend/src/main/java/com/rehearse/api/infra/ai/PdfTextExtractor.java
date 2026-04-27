package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PdfTextExtractor {

    private static final int MAX_TEXT_LENGTH = 5000;
    private static final long MAX_FILE_SIZE = 5_242_880L;
    // 대부분의 경우 1-2회 내 수렴하므로 3회로 상한을 둠
    private static final int KOREAN_FIX_MAX_ITERATIONS = 3;

    private static final Pattern HEADER_FOOTER_PATTERN = Pattern.compile(
            "(?m)^[ \\t]*(?:\\d+[ \\t]*|(?:https?://|www\\.)\\S+|\\d{4}[-./]\\d{2}[-./]\\d{2})[ \\t]*$"
    );

    // PDFBox가 자모 글자 사이에 공백을 삽입하는 경우 복원.
    // 완성형 한글(AC00-D7A3) 사이의 공백은 의미 있는 어절 경계일 수 있으므로 건드리지 않는다.
    private static final Pattern BROKEN_KOREAN_PATTERN = Pattern.compile(
            "([\\u3131-\\u318E\\u3165-\\u318F]) ([\\u3131-\\u318E\\u3165-\\u318F\\uAC00-\\uD7A3])"
    );

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    public String extract(MultipartFile file) {
        validateFile(file);

        try {
            byte[] bytes = file.getBytes();
            validateMagicBytes(bytes);

            try (PDDocument document = Loader.loadPDF(bytes)) {
                String raw = extractWithColumnOrder(document);

                if (raw.isEmpty()) {
                    log.warn("PDF에서 텍스트를 추출할 수 없습니다.");
                    return null;
                }

                String normalized = normalize(raw);
                log.info("PDF 텍스트 추출 완료: rawLength={}, normalizedLength={}",
                        raw.length(), normalized.length());
                return normalized;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("PDF 파싱 실패", e);
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_TYPE);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_SIZE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_TYPE);
        }
    }

    private void validateMagicBytes(byte[] bytes) {
        if (bytes.length < PDF_MAGIC.length) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_MAGIC_BYTES);
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                throw new BusinessException(ResumeErrorCode.INVALID_FILE_MAGIC_BYTES);
            }
        }
    }

    // 2단 컬럼 레이아웃을 올바른 읽기 순서로 추출하기 위해 setSortByPosition(true) 사용
    private String extractWithColumnOrder(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        return stripper.getText(document).trim();
    }

    String normalize(String raw) {
        String text = removeControlChars(raw);
        text = collapseWhitespace(text);
        text = fixKoreanTokenBreaks(text);
        text = removeHeaderFooter(text);
        return trimToMaxLength(text);
    }

    private String removeControlChars(String text) {
        return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }

    private String collapseWhitespace(String text) {
        String result = text.replaceAll("[ \\t]+", " ");
        return result.replaceAll("\\n{3,}", "\n\n");
    }

    private String fixKoreanTokenBreaks(String text) {
        String current = text;
        for (int i = 0; i < KOREAN_FIX_MAX_ITERATIONS; i++) {
            String next = BROKEN_KOREAN_PATTERN.matcher(current).replaceAll("$1$2");
            if (next.equals(current)) {
                break;
            }
            current = next;
        }
        return current;
    }

    private String removeHeaderFooter(String text) {
        return HEADER_FOOTER_PATTERN.matcher(text).replaceAll("");
    }

    private String trimToMaxLength(String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            return text.substring(0, MAX_TEXT_LENGTH);
        }
        return text;
    }
}
