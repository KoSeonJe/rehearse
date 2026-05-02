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
@DisplayName("ResumeQuestionPersister - 카테고리 분리 후 cardinality")
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
    @DisplayName("RESUME_DYNAMIC 으로 조회하므로 RESUME_BASED 다행과 충돌하지 않고 신규 RESUME_DYNAMIC 생성")
    void persist_uses_resume_dynamic_category_avoiding_legacy_collision() {
        long interviewId = 19L;
        Interview interview = Interview.builder().build();
        given(interviewFinder.findById(interviewId)).willReturn(interview);
        given(questionSetRepository.findByInterviewIdAndCategory(eq(interviewId), eq(QuestionSetCategory.RESUME_DYNAMIC)))
                .willReturn(Optional.empty());
        given(questionSetRepository.countByInterviewId(interviewId)).willReturn(5L);
        given(questionSetRepository.save(any(QuestionSet.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(questionRepository.save(any(Question.class)))
                .willAnswer(inv -> inv.getArgument(0));

        persister.persist(interviewId, QuestionType.RESUME_OPENER, "Q?", 0);

        ArgumentCaptor<QuestionSet> captor = ArgumentCaptor.forClass(QuestionSet.class);
        then(questionSetRepository).should().save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(QuestionSetCategory.RESUME_DYNAMIC);
        then(questionSetRepository).should()
                .findByInterviewIdAndCategory(eq(interviewId), eq(QuestionSetCategory.RESUME_DYNAMIC));
    }

    @Test
    @DisplayName("기존 RESUME_DYNAMIC 행이 있으면 재사용한다")
    void persist_reuses_existing_resume_dynamic_question_set() {
        long interviewId = 19L;
        QuestionSet existing = QuestionSet.builder()
                .category(QuestionSetCategory.RESUME_DYNAMIC)
                .orderIndex(5)
                .build();
        given(questionSetRepository.findByInterviewIdAndCategory(eq(interviewId), eq(QuestionSetCategory.RESUME_DYNAMIC)))
                .willReturn(Optional.of(existing));
        given(questionRepository.save(any(Question.class)))
                .willAnswer(inv -> inv.getArgument(0));

        persister.persist(interviewId, QuestionType.RESUME_PLAYGROUND, "Q?", 1);

        then(questionSetRepository).should(org.mockito.Mockito.never()).save(any(QuestionSet.class));
        then(interviewFinder).shouldHaveNoInteractions();
    }
}
