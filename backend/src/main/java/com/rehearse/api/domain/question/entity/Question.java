package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType questionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String ttsText;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReferenceType referenceType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeedbackPerspective feedbackPerspective;

    @Column(nullable = false)
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_pool_id")
    private QuestionPool questionPool;

    @Builder
    public Question(QuestionType questionType, String questionText,
                    String ttsText, String modelAnswer, ReferenceType referenceType,
                    FeedbackPerspective feedbackPerspective,
                    int orderIndex, QuestionPool questionPool) {
        this.questionType = questionType;
        this.questionText = questionText;
        this.ttsText = ttsText;
        this.modelAnswer = modelAnswer;
        this.referenceType = referenceType;
        this.feedbackPerspective = feedbackPerspective;
        this.orderIndex = orderIndex;
        this.questionPool = questionPool;
    }

    public void assignQuestionSet(QuestionSet questionSet) {
        this.questionSet = questionSet;
    }

    /**
     * Resume Track용 stub — LLM 동적 생성 질문은 DB에 저장되지 않음.
     * RubricLoader.resolveFor()는 resumeTrack=true 우선으로 매핑하므로 questionText/feedbackPerspective만 참조.
     */
    public static Question stubForResumeTrack() {
        Question stub = new Question();
        stub.questionType = QuestionType.MAIN;
        stub.questionText = "";
        stub.feedbackPerspective = FeedbackPerspective.TECHNICAL;
        stub.orderIndex = 0;
        return stub;
    }
}
