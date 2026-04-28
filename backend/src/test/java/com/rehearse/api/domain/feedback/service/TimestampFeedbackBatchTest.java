package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.mapper.TimestampFeedbackMapper;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimestampFeedbackBatch - TimestampFeedback 다중 적재")
class TimestampFeedbackBatchTest {

    @InjectMocks
    private TimestampFeedbackBatch batch;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TimestampFeedbackMapper timestampFeedbackMapper;

    @Test
    @DisplayName("attachTo_does_nothing_when_items_is_null")
    void attachTo_null_items_doesNothing() {
        QuestionSetFeedback feedback = createFeedback();

        batch.attachTo(feedback, null);

        then(questionRepository).should(never()).findById(any());
        then(timestampFeedbackMapper).should(never()).toEntity(any(), any());
    }

    @Test
    @DisplayName("attachTo_attaches_all_items_to_feedback")
    void attachTo_attaches_each_item_to_feedback() {
        // given
        QuestionSetFeedback feedback = createFeedback();
        Question question = createQuestion(10L);
        SaveFeedbackRequest.TimestampFeedbackItem item = createItem(10L);

        TimestampFeedback stub = TimestampFeedback.builder()
                .startMs(0L).endMs(5000L).isAnalyzed(true).build();

        given(questionRepository.findById(10L)).willReturn(Optional.of(question));
        given(timestampFeedbackMapper.toEntity(eq(item), eq(question))).willReturn(stub);

        // when
        batch.attachTo(feedback, List.of(item));

        // then
        assertThat(feedback.getTimestampFeedbacks()).hasSize(1);
    }

    @Test
    @DisplayName("attachTo_uses_null_question_when_questionId_not_found")
    void attachTo_nullQuestion_when_questionId_unknown() {
        // given
        QuestionSetFeedback feedback = createFeedback();
        SaveFeedbackRequest.TimestampFeedbackItem item = createItem(999L);

        TimestampFeedback stub = TimestampFeedback.builder()
                .startMs(0L).endMs(3000L).isAnalyzed(true).build();

        given(questionRepository.findById(999L)).willReturn(Optional.empty());
        given(timestampFeedbackMapper.toEntity(eq(item), eq(null))).willReturn(stub);

        // when
        batch.attachTo(feedback, List.of(item));

        // then
        assertThat(feedback.getTimestampFeedbacks()).hasSize(1);
        then(timestampFeedbackMapper).should().toEntity(eq(item), eq(null));
    }

    @Test
    @DisplayName("attachTo_skips_question_lookup_when_questionId_is_null")
    void attachTo_noLookup_when_questionId_null() {
        // given
        QuestionSetFeedback feedback = createFeedback();
        SaveFeedbackRequest.TimestampFeedbackItem item = createItem(null);

        TimestampFeedback stub = TimestampFeedback.builder()
                .startMs(0L).endMs(1000L).isAnalyzed(true).build();

        given(timestampFeedbackMapper.toEntity(eq(item), eq(null))).willReturn(stub);

        // when
        batch.attachTo(feedback, List.of(item));

        // then
        then(questionRepository).should(never()).findById(any());
        assertThat(feedback.getTimestampFeedbacks()).hasSize(1);
    }

    @Test
    @DisplayName("attachTo_attaches_multiple_items_in_order")
    void attachTo_multipleItems_allAttached() {
        // given
        QuestionSetFeedback feedback = createFeedback();
        SaveFeedbackRequest.TimestampFeedbackItem item1 = createItem(null);
        SaveFeedbackRequest.TimestampFeedbackItem item2 = createItem(null);

        TimestampFeedback stub1 = TimestampFeedback.builder().startMs(0L).endMs(1000L).isAnalyzed(true).build();
        TimestampFeedback stub2 = TimestampFeedback.builder().startMs(1000L).endMs(2000L).isAnalyzed(true).build();

        given(timestampFeedbackMapper.toEntity(eq(item1), eq(null))).willReturn(stub1);
        given(timestampFeedbackMapper.toEntity(eq(item2), eq(null))).willReturn(stub2);

        // when
        batch.attachTo(feedback, List.of(item1, item2));

        // then
        assertThat(feedback.getTimestampFeedbacks()).hasSize(2);
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private QuestionSetFeedback createFeedback() {
        Interview interview = mock(Interview.class);
        QuestionSet questionSet = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(1)
                .build();
        return QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment("테스트")
                .build();
    }

    private Question createQuestion(Long id) {
        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("질문 텍스트")
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private SaveFeedbackRequest.TimestampFeedbackItem createItem(Long questionId) {
        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "questionId", questionId);
        ReflectionTestUtils.setField(item, "startMs", 0L);
        ReflectionTestUtils.setField(item, "endMs", 5000L);
        return item;
    }
}
