package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.analysis.entity.AnalysisStatus;
import com.rehearse.api.domain.analysis.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionSet 엔티티")
class QuestionSetTest {

    @Nested
    @DisplayName("assignInterview 메서드")
    class AssignInterview {

        @Test
        @DisplayName("인터뷰를 할당하면 interview 필드가 변경된다")
        void assignInterview_validInterview_updatesInterview() {
            // given
            Interview original = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(original);

            Interview newInterview = TestFixtures.createInterview();
            ReflectionTestUtils.setField(newInterview, "id", 99L);

            // when
            questionSet.assignInterview(newInterview);

            // then
            assertThat(questionSet.getInterview()).isSameAs(newInterview);
        }
    }

    @Nested
    @DisplayName("updateOrderIndex 메서드")
    class UpdateOrderIndex {

        @Test
        @DisplayName("orderIndex를 변경하면 새 값이 반영된다")
        void updateOrderIndex_newValue_updatesOrderIndex() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            assertThat(questionSet.getOrderIndex()).isEqualTo(0);

            // when
            questionSet.updateOrderIndex(5);

            // then
            assertThat(questionSet.getOrderIndex()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("addQuestion 메서드")
    class AddQuestion {

        @Test
        @DisplayName("질문을 추가하면 questions 컬렉션에 포함된다")
        void addQuestion_validQuestion_addsToCollection() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("Spring IoC 컨테이너에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            // when
            questionSet.addQuestion(question);

            // then
            assertThat(questionSet.getQuestions()).hasSize(1);
            assertThat(questionSet.getQuestions().get(0)).isSameAs(question);
        }

        @Test
        @DisplayName("질문을 추가하면 해당 질문의 questionSet이 자신으로 설정된다")
        void addQuestion_validQuestion_assignsQuestionSetToQuestion() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("DI에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            // when
            questionSet.addQuestion(question);

            // then
            assertThat(question.getQuestionSet()).isSameAs(questionSet);
        }

        @Test
        @DisplayName("여러 질문을 추가하면 모두 컬렉션에 포함된다")
        void addQuestion_multipleQuestions_allAddedToCollection() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            Question question1 = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("질문 1")
                    .orderIndex(0)
                    .build();
            Question question2 = Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("질문 2")
                    .orderIndex(1)
                    .build();

            // when
            questionSet.addQuestion(question1);
            questionSet.addQuestion(question2);

            // then
            assertThat(questionSet.getQuestions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getQuestions 메서드")
    class GetQuestions {

        @Test
        @DisplayName("반환된 리스트는 수정 불가능하다")
        void getQuestions_returnedList_isUnmodifiable() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);

            // when & then
            assertThat(questionSet.getQuestions()).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("assignFileMetadata 메서드")
    class AssignFileMetadata {

        @Test
        @DisplayName("파일 메타데이터를 할당하면 fileMetadata 필드가 설정된다")
        void assignFileMetadata_validMetadata_updatesFileMetadata() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            FileMetadata fileMetadata = FileMetadata.builder()
                    .fileType(FileType.VIDEO)
                    .s3Key("videos/test.webm")
                    .bucket("rehearse-videos-dev")
                    .contentType("video/webm")
                    .build();

            // when
            questionSet.assignFileMetadata(fileMetadata);

            // then
            assertThat(questionSet.getFileMetadata()).isSameAs(fileMetadata);
        }
    }

    @Nested
    @DisplayName("getEffectiveAnalysisStatus 메서드")
    class GetEffectiveAnalysisStatus {

        @Test
        @DisplayName("analysis가 null이면 PENDING을 반환한다")
        void getEffectiveAnalysisStatus_noAnalysis_returnsPending() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);

            // when
            AnalysisStatus status = questionSet.getEffectiveAnalysisStatus();

            // then
            assertThat(status).isEqualTo(AnalysisStatus.PENDING);
        }

        @Test
        @DisplayName("analysis가 존재하면 해당 분석 상태를 반환한다")
        void getEffectiveAnalysisStatus_withAnalysis_returnsAnalysisStatus() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                    .questionSet(questionSet)
                    .build();
            ReflectionTestUtils.setField(questionSet, "analysis", analysis);
            ReflectionTestUtils.setField(analysis, "analysisStatus", AnalysisStatus.ANALYZING);

            // when
            AnalysisStatus status = questionSet.getEffectiveAnalysisStatus();

            // then
            assertThat(status).isEqualTo(AnalysisStatus.ANALYZING);
        }
    }

    @Nested
    @DisplayName("getAnalysisFailureReason 메서드")
    class GetAnalysisFailureReason {

        @Test
        @DisplayName("analysis가 null이면 null을 반환한다")
        void getAnalysisFailureReason_noAnalysis_returnsNull() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);

            // when
            String reason = questionSet.getAnalysisFailureReason();

            // then
            assertThat(reason).isNull();
        }

        @Test
        @DisplayName("analysis가 존재하면 실패 사유를 반환한다")
        void getAnalysisFailureReason_withAnalysis_returnsFailureReason() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                    .questionSet(questionSet)
                    .build();
            ReflectionTestUtils.setField(questionSet, "analysis", analysis);
            ReflectionTestUtils.setField(analysis, "failureReason", "STT 변환 실패");

            // when
            String reason = questionSet.getAnalysisFailureReason();

            // then
            assertThat(reason).isEqualTo("STT 변환 실패");
        }
    }
}
