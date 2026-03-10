package com.devlens.api.infra.ai;

import com.devlens.api.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class PdfTextExtractor {

    private static final int MAX_TEXT_LENGTH = 5000;

    public String extract(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();

            if (text.isEmpty()) {
                log.warn("PDF에서 텍스트를 추출할 수 없습니다: {}", file.getOriginalFilename());
                return null;
            }

            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }

            log.info("PDF 텍스트 추출 완료: filename={}, length={}", file.getOriginalFilename(), text.length());
            return text;
        } catch (IOException e) {
            log.error("PDF 파싱 실패: {}", file.getOriginalFilename(), e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INTERVIEW_004", "PDF 파일을 읽을 수 없습니다.");
        }
    }
}
