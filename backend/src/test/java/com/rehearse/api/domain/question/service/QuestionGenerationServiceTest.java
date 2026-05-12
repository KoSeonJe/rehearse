package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.dto.QuestionGenerationCommand;
import com.rehearse.api.domain.question.entity.QuestionSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionGenerationService - 트랙 라우팅")
class QuestionGenerationServiceTest {

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    @Mock
    private QuestionGenerationTransactionHandler transactionHandler;

    @Mock
    private ResumeTrackInitiator resumeTrackInitiator;

    @Mock
    private StandardTrackQuestionGenerator standardTrackGenerator;

    @Nested
    @DisplayName("RESUME_BASED 단독 분기")
    class ResumeBranch {

        @Test
        @DisplayName("RESUME_BASED 단독이면 resumeTrackInitiator 로 위임하고 standardTrackGenerator 는 호출하지 않는다")
        void resumeBasedOnly_routesToResumeInitiator() {
            List<InterviewType> types = List.of(InterviewType.RESUME_BASED);

            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), "이력서", "hash-1", 30, TechStack.JAVA_SPRING);

            then(transactionHandler).should().startGeneration(1L);
            then(resumeTrackInitiator).should().initiate(1L, InterviewLevel.JUNIOR, "hash-1", "이력서", 30);
            then(standardTrackGenerator).should(never()).generate(any(QuestionGenerationCommand.class));
            then(transactionHandler).should(never()).saveResults(anyLong(), any());
        }

        @Test
        @DisplayName("RESUME_BASED 단독 시 resumeFileHash 가 없는 오버로드도 정상 위임된다")
        void resumeBasedOnly_overloadWithoutHash_routesToResumeInitiator() {
            List<InterviewType> types = List.of(InterviewType.RESUME_BASED);

            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), "이력서", 30, TechStack.JAVA_SPRING);

            then(resumeTrackInitiator).should().initiate(1L, InterviewLevel.JUNIOR, null, "이력서", 30);
        }
    }

    @Nested
    @DisplayName("표준 분기")
    class StandardBranch {

        @Test
        @DisplayName("RESUME_BASED 가 없으면 standardTrackGenerator 가 호출되고 결과가 saveResults 로 전달된다")
        void nonResumeTypes_routesToStandardGenerator() {
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            List<QuestionSet> generated = List.of(
                    QuestionSet.builder().category(InterviewType.CS_FUNDAMENTAL).orderIndex(0).build());
            given(standardTrackGenerator.generate(any(QuestionGenerationCommand.class)))
                    .willReturn(generated);

            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                    transactionHandler, standardTrackGenerator);
            inOrder.verify(transactionHandler).startGeneration(1L);
            inOrder.verify(standardTrackGenerator).generate(any(QuestionGenerationCommand.class));
            inOrder.verify(transactionHandler).saveResults(1L, generated);
            then(resumeTrackInitiator).should(never()).initiate(any(), any(), any(), any(), any());
        }
    }
}
