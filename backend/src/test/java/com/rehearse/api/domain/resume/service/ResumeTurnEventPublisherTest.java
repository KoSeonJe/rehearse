package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.IntentResult;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeTurnEventPublisher - Resume 턴 이벤트 발행")
class ResumeTurnEventPublisherTest {

    @InjectMocks
    private ResumeTurnEventPublisher publisher;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("결함성 questionId 방어")
    class QuestionIdGuard {

        @Test
        @DisplayName("questionId 가 null 이면 이벤트를 발행하지 않는다")
        void publish_nullQuestionId_skipsEvent() {
            publisher.publish(
                    1L, 0L,
                    new AnswerAnalysis(0L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE),
                    IntentResult.of(IntentType.ANSWER, 0.9, "answer"),
                    ResumeMode.PLAYGROUND, 1,
                    null, "답변", null);

            then(interviewFinder).shouldHaveNoInteractions();
            then(questionSetRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }
}
