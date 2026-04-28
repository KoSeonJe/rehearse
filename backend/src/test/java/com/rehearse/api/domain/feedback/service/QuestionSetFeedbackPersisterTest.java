package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionSetFeedbackPersister - QuestionSetFeedback 생성 및 저장")
class QuestionSetFeedbackPersisterTest {

    @InjectMocks
    private QuestionSetFeedbackPersister persister;

    @Mock
    private QuestionSetFeedbackRepository feedbackRepository;

    @Mock
    private TimestampFeedbackBatch timestampFeedbackBatch;

    @Test
    @DisplayName("persist_saves_feedback_with_comment_from_request")
    void persist_saves_feedback_with_correct_comment() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        SaveFeedbackRequest request = createRequest("좋은 답변입니다.", true, true);
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        QuestionSetFeedback result = persister.persist(questionSet, request);

        // then
        assertThat(result.getQuestionSetComment()).isEqualTo("좋은 답변입니다.");
        assertThat(result.getQuestionSet()).isEqualTo(questionSet);
    }

    @Test
    @DisplayName("persist_delegates_timestamp_items_to_TimestampFeedbackBatch")
    void persist_delegates_to_timestampFeedbackBatch() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        SaveFeedbackRequest request = createRequest("코멘트", true, true);
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        persister.persist(questionSet, request);

        // then
        then(timestampFeedbackBatch).should().attachTo(any(QuestionSetFeedback.class), any());
    }

    @Test
    @DisplayName("persist_passes_feedback_to_repository_save")
    void persist_calls_feedbackRepository_save() {
        // given
        QuestionSet questionSet = createQuestionSet(2L);
        SaveFeedbackRequest request = createRequest("저장 확인", false, false);
        ArgumentCaptor<QuestionSetFeedback> captor = ArgumentCaptor.forClass(QuestionSetFeedback.class);
        given(feedbackRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

        // when
        persister.persist(questionSet, request);

        // then
        assertThat(captor.getValue().getQuestionSetComment()).isEqualTo("저장 확인");
        then(feedbackRepository).should().save(any(QuestionSetFeedback.class));
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private QuestionSet createQuestionSet(Long id) {
        Interview interview = mock(Interview.class);
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(qs, "id", id);
        return qs;
    }

    private SaveFeedbackRequest createRequest(String comment, boolean verbal, boolean nonverbal) {
        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetComment", comment);
        ReflectionTestUtils.setField(request, "timestampFeedbacks", null);
        ReflectionTestUtils.setField(request, "verbalCompleted", verbal);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", nonverbal);
        return request;
    }
}
