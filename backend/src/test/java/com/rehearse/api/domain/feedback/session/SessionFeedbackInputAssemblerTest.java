package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInputAssembler;
import com.rehearse.api.domain.feedback.session.synthesis.TurnScoreView;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionFeedbackInputAssembler - timestamp_feedback 코멘트 기반 입력 조립")
class SessionFeedbackInputAssemblerTest {

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private TimestampFeedbackRepository timestampFeedbackRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    private SessionFeedbackInputAssembler assembler;
    private Interview interview;

    @BeforeEach
    void setUp() {
        assembler = new SessionFeedbackInputAssembler(
                interviewFinder, timestampFeedbackRepository, questionSetRepository);
        interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
    }

    @Nested
    @DisplayName("turnScores 조립")
    class TurnScores {

        @Test
        @DisplayName("코멘트가 turnLabel(메인 1-1, 후속 1-2)과 함께 코멘트형 뷰로 매핑된다")
        void assemble_mapsCommentsToTurnLabels() {
            Question main = question(10L, QuestionType.TECH_MAIN, 0);
            Question followUp = question(11L, QuestionType.TECH_FOLLOWUP, 1);
            QuestionSet questionSet = questionSet(100L, 0, List.of(main, followUp));
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of(questionSet));
            given(timestampFeedbackRepository.findByInterviewIdOrderByStartMs(1L)).willReturn(List.of(
                    timestampFeedback(main, 0, "답변이 명확함", "[]", "STAR 구조", "근거 보강", "종합1"),
                    timestampFeedback(followUp, 1000, "후속 답변 보강 필요", null, null, null, "종합2")
            ));

            SessionFeedbackInput input = assembler.assemble(1L);

            assertThat(input.turnScores()).hasSize(2);
            TurnScoreView first = input.turnScores().get(0);
            assertThat(first.turnLabel()).isEqualTo("1-1");
            assertThat(first.verbalComment()).isEqualTo("답변이 명확함");
            assertThat(first.coachingStructure()).isEqualTo("STAR 구조");
            assertThat(first.overallComment()).isEqualTo("종합1");
            assertThat(input.turnScores().get(1).turnLabel()).isEqualTo("1-2");
        }

        @Test
        @DisplayName("question 이 null 인 코멘트는 turnLabel 이 null 로 유지된다")
        void assemble_keepsNullTurnLabel_whenQuestionMissing() {
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of());
            given(timestampFeedbackRepository.findByInterviewIdOrderByStartMs(1L)).willReturn(List.of(
                    timestampFeedback(null, 0, "코멘트", null, null, null, "종합")
            ));

            SessionFeedbackInput input = assembler.assemble(1L);

            assertThat(input.turnScores()).hasSize(1);
            assertThat(input.turnScores().get(0).turnLabel()).isNull();
        }
    }

    @Nested
    @DisplayName("coverage 표기")
    class Coverage {

        @Test
        @DisplayName("모든 turn 에 코멘트가 있으면 all turns scored")
        void coverage_allScored() {
            Question main = question(10L, QuestionType.TECH_MAIN, 0);
            QuestionSet questionSet = questionSet(100L, 0, List.of(main));
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of(questionSet));
            given(timestampFeedbackRepository.findByInterviewIdOrderByStartMs(1L)).willReturn(List.of(
                    timestampFeedback(main, 0, "코멘트", null, null, null, "종합")
            ));

            SessionFeedbackInput input = assembler.assemble(1L);

            assertThat(input.coverage()).isEqualTo("all turns scored");
        }

        @Test
        @DisplayName("코멘트가 비어있는 turn 이 섞이면 부분 비율로 표기된다")
        void coverage_partialWhenSomeTurnsEmpty() {
            Question main = question(10L, QuestionType.TECH_MAIN, 0);
            Question followUp = question(11L, QuestionType.TECH_FOLLOWUP, 1);
            QuestionSet questionSet = questionSet(100L, 0, List.of(main, followUp));
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of(questionSet));
            given(timestampFeedbackRepository.findByInterviewIdOrderByStartMs(1L)).willReturn(List.of(
                    timestampFeedback(main, 0, "코멘트", null, null, null, "종합"),
                    timestampFeedback(followUp, 1000, null, null, null, null, null)
            ));

            SessionFeedbackInput input = assembler.assemble(1L);

            assertThat(input.coverage()).isEqualTo("1/2 turns scored");
        }
    }

    @Nested
    @DisplayName("sessionMetadata 와 delivery 인자")
    class Metadata {

        @Test
        @DisplayName("세션 메타데이터는 인터뷰 정보 + 코멘트 turn 수로 채워진다")
        void assemble_fillsSessionMetadata() {
            Question main = question(10L, QuestionType.TECH_MAIN, 0);
            QuestionSet questionSet = questionSet(100L, 0, List.of(main));
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of(questionSet));
            given(timestampFeedbackRepository.findByInterviewIdOrderByStartMs(1L)).willReturn(List.of(
                    timestampFeedback(main, 0, "코멘트", null, null, null, "종합")
            ));

            SessionFeedbackInput input = assembler.assemble(1L);

            assertThat(input.sessionMetadata().position()).isEqualTo("BACKEND");
            assertThat(input.sessionMetadata().level()).isEqualTo("MID");
            assertThat(input.sessionMetadata().interviewTypes()).containsExactly("CS_FUNDAMENTAL");
            assertThat(input.sessionMetadata().totalTurns()).isEqualTo(1);
            assertThat(input.sessionMetadata().durationMinutes()).isEqualTo(30);
        }

        @Test
        @DisplayName("assembleWithDelivery 는 delivery/vision 분석 문자열을 입력에 보존한다")
        void assembleWithDelivery_keepsDeliveryAndVision() {
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of());
            given(timestampFeedbackRepository.findByInterviewIdOrderByStartMs(1L)).willReturn(List.of());

            SessionFeedbackInput input = assembler.assembleWithDelivery(1L, "{delivery}", "{vision}", "{nonverbal}");

            assertThat(input.deliveryAnalysis()).isEqualTo("{delivery}");
            assertThat(input.visionAnalysis()).isEqualTo("{vision}");
        }
    }

    private Question question(Long id, QuestionType type, int orderIndex) {
        Question question = Question.builder()
                .questionType(type)
                .questionText("질문")
                .orderIndex(orderIndex)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private QuestionSet questionSet(Long id, int orderIndex, List<Question> questions) {
        QuestionSet questionSet = QuestionSet.builder()
                .category(InterviewType.CS_FUNDAMENTAL)
                .orderIndex(orderIndex)
                .build();
        ReflectionTestUtils.setField(questionSet, "id", id);
        questions.forEach(questionSet::addQuestion);
        return questionSet;
    }

    private TimestampFeedback timestampFeedback(Question question, long startMs, String verbalComment,
                                                String accuracyIssues, String coachingStructure,
                                                String coachingImprovement, String overallComment) {
        QuestionSetFeedback questionSetFeedback = QuestionSetFeedback.builder()
                .questionSetComment("set comment")
                .build();
        TimestampFeedback feedback = TimestampFeedback.builder()
                .question(question)
                .startMs(startMs)
                .endMs(startMs + 1000)
                .verbalComment(verbalComment)
                .accuracyIssues(accuracyIssues)
                .coachingStructure(coachingStructure)
                .coachingImprovement(coachingImprovement)
                .overallComment(overallComment)
                .isAnalyzed(true)
                .build();
        questionSetFeedback.addTimestampFeedback(feedback);
        return feedback;
    }
}
