package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.global.exception.BusinessException;
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
        Interview interview = interviewRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new BusinessException(InterviewErrorCode.NOT_FOUND));

        // ElementCollection은 별도 쿼리로 초기화 (MultipleBagFetchException 방지)
        interviewRepository.findByIdWithElementCollections(id);

        return interview;
    }
}
