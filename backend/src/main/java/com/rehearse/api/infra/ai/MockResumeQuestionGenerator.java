package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.QuestionDepthType;
import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.OpenAiResumeQuestionGenerator;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions.GeneratedResumeQuestion;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnMissingBean(OpenAiResumeQuestionGenerator.class)
public class MockResumeQuestionGenerator implements ResumeQuestionGenerator {

    @PostConstruct
    void init() {
        log.warn("=== MockResumeQuestionGenerator 활성화: API 키 없이 Mock 이력서 질문으로 동작합니다 ===");
    }

    @Override
    public GeneratedResumeQuestions generate(
            byte[] pdfBytes, int openerCount, int mainCount, Position position, TechStack techStack) {
        List<GeneratedResumeQuestion> openers = new ArrayList<>(Math.max(0, openerCount));
        for (int i = 0; i < openerCount; i++) {
            openers.add(new GeneratedResumeQuestion(
                    "[Mock] 자기소개와 최근 프로젝트 한 가지를 간단히 말씀해주세요.",
                    "자기소개와 최근 프로젝트 한 가지를 간단히 말씀해주세요.",
                    "이름·역할·기여한 핵심 프로젝트 한 개를 1분 안에 정리하세요.",
                    QuestionDepthType.PRINCIPLE));
        }
        List<GeneratedResumeQuestion> mains = new ArrayList<>(Math.max(0, mainCount));
        for (int i = 0; i < mainCount; i++) {
            mains.add(new GeneratedResumeQuestion(
                    "[Mock] 가장 어려웠던 기술적 결정과 그 사유를 설명해주세요. (질문 #" + (i + 1) + ")",
                    "가장 어려웠던 기술적 결정과 그 사유를 설명해주세요.",
                    "STAR 기법으로 상황·과제·행동·결과를 구조화하세요.",
                    QuestionDepthType.TRADEOFF));
        }
        return new GeneratedResumeQuestions(openers, mains);
    }
}
