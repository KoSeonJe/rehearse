package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.question.entity.Question;

public record FollowUpSaveResult(Question question, int newFollowUpCount) {
}
