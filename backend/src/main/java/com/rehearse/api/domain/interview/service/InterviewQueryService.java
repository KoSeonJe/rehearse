package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewQueryService {

    private final InterviewFinder interviewFinder;
    private final QuestionSetRepository questionSetRepository;

    public InterviewResponse getInterview(Long id, Long userId) {
        Interview interview = interviewFinder.findByIdAndValidateOwner(id, userId);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(id);
        return InterviewResponse.from(interview, questionSets);
    }

    public InterviewResponse getInterviewByPublicId(String publicId, Long userId) {
        Interview interview = interviewFinder.findByPublicIdAndValidateOwner(publicId, userId);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(interview.getId());
        return InterviewResponse.from(interview, questionSets);
    }
}
