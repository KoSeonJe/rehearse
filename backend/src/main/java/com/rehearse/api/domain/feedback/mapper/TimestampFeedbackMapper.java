package com.rehearse.api.domain.feedback.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.question.entity.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimestampFeedbackMapper {

    private final ObjectMapper objectMapper;

    public TimestampFeedback toEntity(SaveFeedbackRequest.TimestampFeedbackItem item, Question question) {
        return TimestampFeedback.builder()
                .question(question)
                .startMs(item.getStartMs())
                .endMs(item.getEndMs())
                .transcript(item.getTranscript())
                .fillerWordCount(item.getFillerWordCount())
                .isAnalyzed(true)
                .fillerWords(toJson(item.getFillerWords()))
                .build();
    }

    String toJson(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("List<String> JSON 직렬화 실패: {}", list, e);
            return null;
        }
    }
}
