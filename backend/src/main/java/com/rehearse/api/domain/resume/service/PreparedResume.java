package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;

public record PreparedResume(ResumeSkeleton skeleton, InterviewPlan plan) {
}
