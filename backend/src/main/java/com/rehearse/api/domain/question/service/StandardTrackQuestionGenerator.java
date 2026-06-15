package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.dto.QuestionGenerationCommand;
import com.rehearse.api.domain.question.entity.QuestionDistribution;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.infra.ai.prompt.QuestionCountCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StandardTrackQuestionGenerator {

    private final StandardQuestionProvider standardQuestionProvider;
    private final QuestionSetAssembler assembler;

    public List<QuestionSet> generate(QuestionGenerationCommand command) {
        int totalCount = QuestionCountCalculator.calculate(command.durationMinutes(), command.interviewTypes().size());
        QuestionDistribution distribution = QuestionDistribution.create(command.interviewTypes(), totalCount);

        List<QuestionSet> generatedQuestions = assembleQuestions(command, distribution);

        for (int i = 0; i < generatedQuestions.size(); i++) {
            generatedQuestions.get(i).updateOrderIndex(i);
        }
        return generatedQuestions;
    }

    private List<QuestionSet> assembleQuestions(QuestionGenerationCommand command, QuestionDistribution distribution) {

        List<QuestionSet> result = new ArrayList<>();
        for (var entry : distribution.getDistribution().entrySet()) {
            InterviewType type = entry.getKey();
            int count = entry.getValue();

            List<QuestionPool> poolQuestions = standardQuestionProvider.provide(type, count, command.toGetQuestionPoolCommand());

            for (QuestionPool questionPool : poolQuestions) {
                result.add(assembler.fromPool(type, questionPool));
            }
        }

        log.info("[STANDARD] 질문 제공 완료: interviewId={}, count={}", command.interviewId(), result.size());
        return result;
    }
}
