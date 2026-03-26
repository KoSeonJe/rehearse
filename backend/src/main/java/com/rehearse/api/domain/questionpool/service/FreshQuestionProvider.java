package com.rehearse.api.domain.questionpool.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreshQuestionProvider {

    private final AiClient aiClient;

    public List<GeneratedQuestion> provide(Position position, InterviewLevel level,
                                           TechStack techStack, Set<InterviewType> types,
                                           int requiredCount, String resumeText,
                                           List<String> csSubTopics, Integer durationMinutes) {

        QuestionGenerationRequest request = new QuestionGenerationRequest(
                position, null, level,
                types,
                csSubTopics != null ? new HashSet<>(csSubTopics) : Set.of(),
                resumeText, durationMinutes, techStack
        );

        List<GeneratedQuestion> generated = aiClient.generateQuestions(request);

        log.info("[FRESH] AI 호출 완료: types={}, generated={}", types, generated.size());

        if (generated.size() > requiredCount) {
            return generated.subList(0, requiredCount);
        }
        return generated;
    }
}
