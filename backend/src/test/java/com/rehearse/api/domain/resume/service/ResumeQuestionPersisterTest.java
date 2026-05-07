package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeQuestionPersister - RESUME_BASED 단일 카테고리 cardinality")
class ResumeQuestionPersisterTest {

    @InjectMocks
    private ResumeQuestionPersister persister;

    @Mock
    private QuestionSetRepository questionSetRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private InterviewFinder interviewFinder;

    @Test
    @DisplayName("RESUME_BASED 로 조회하고 없으면 신규 RESUME_BASED QuestionSet 을 생성한다")
    void persist_creates_resume_based_question_set_when_absent() {
        long interviewId = 19L;
        Interview interview = Interview.builder().build();
        given(interviewFinder.findById(interviewId)).willReturn(interview);
        given(questionSetRepository.findByInterviewIdAndCategory(eq(interviewId), eq(QuestionSetCategory.RESUME_BASED)))
                .willReturn(Optional.empty());
        given(questionSetRepository.countByInterviewId(interviewId)).willReturn(0L);
        given(questionSetRepository.save(any(QuestionSet.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(questionRepository.save(any(Question.class)))
                .willAnswer(inv -> inv.getArgument(0));

        persister.persist(interviewId, QuestionType.RESUME_OPENER, "Q?", "Q", "answer", 0);

        ArgumentCaptor<QuestionSet> captor = ArgumentCaptor.forClass(QuestionSet.class);
        then(questionSetRepository).should().save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(QuestionSetCategory.RESUME_BASED);
        then(questionSetRepository).should()
                .findByInterviewIdAndCategory(eq(interviewId), eq(QuestionSetCategory.RESUME_BASED));
    }

    @Test
    @DisplayName("기존 RESUME_BASED 행이 있으면 재사용한다 — 인터뷰당 1행 유지")
    void persist_reuses_existing_resume_based_question_set() {
        long interviewId = 19L;
        QuestionSet existing = QuestionSet.builder()
                .category(QuestionSetCategory.RESUME_BASED)
                .orderIndex(0)
                .build();
        given(questionSetRepository.findByInterviewIdAndCategory(eq(interviewId), eq(QuestionSetCategory.RESUME_BASED)))
                .willReturn(Optional.of(existing));
        given(questionRepository.save(any(Question.class)))
                .willAnswer(inv -> inv.getArgument(0));

        persister.persist(interviewId, QuestionType.RESUME_PLAYGROUND, "Q?", "Q", "answer", 1);

        then(questionSetRepository).should(org.mockito.Mockito.never()).save(any(QuestionSet.class));
        then(interviewFinder).shouldHaveNoInteractions();
    }
}
