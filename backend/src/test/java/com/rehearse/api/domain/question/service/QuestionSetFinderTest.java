package com.rehearse.api.domain.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionSetFinder - question 도메인 cross-domain read 진입점")
class QuestionSetFinderTest {

    @InjectMocks
    private QuestionSetFinder questionSetFinder;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Test
    @DisplayName("findByInterviewIdWithQuestions 는 Repository 결과를 그대로 반환한다")
    void delegates_to_repository() {
        QuestionSet qs = QuestionSet.builder().orderIndex(0).build();
        given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of(qs));

        List<QuestionSet> result = questionSetFinder.findByInterviewIdWithQuestions(1L);

        assertThat(result).containsExactly(qs);
    }
}
