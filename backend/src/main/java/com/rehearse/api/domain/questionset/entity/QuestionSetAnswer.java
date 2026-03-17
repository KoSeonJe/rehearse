package com.rehearse.api.domain.questionset.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_set_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionSetAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private long startMs;

    @Column(nullable = false)
    private long endMs;

    @Builder
    public QuestionSetAnswer(Question question, long startMs, long endMs) {
        this.question = question;
        this.startMs = startMs;
        this.endMs = endMs;
    }
}
