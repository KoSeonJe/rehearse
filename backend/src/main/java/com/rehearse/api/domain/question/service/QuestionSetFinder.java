package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionSetFinder {

    private final QuestionSetRepository questionSetRepository;

    public List<QuestionSet> findByInterviewIdWithQuestions(Long interviewId) {
        return questionSetRepository.findByInterviewIdWithQuestions(interviewId);
    }
}
