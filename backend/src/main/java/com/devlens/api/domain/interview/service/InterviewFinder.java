package com.devlens.api.domain.interview.service;

import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.exception.InterviewErrorCode;
import com.devlens.api.domain.interview.repository.InterviewRepository;
import com.devlens.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewFinder {

    private final InterviewRepository interviewRepository;

    public Interview findById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException(InterviewErrorCode.NOT_FOUND));
    }

    public Interview findByIdWithQuestions(Long id) {
        return interviewRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new BusinessException(InterviewErrorCode.NOT_FOUND));
    }
}
