package com.rehearse.api.domain.question.models.service;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;

public interface ResumeQuestionGenerator {

    GeneratedResumeQuestions generate(
            byte[] pdfBytes,
            int openerCount,
            int mainCount,
            Position position,
            TechStack techStack);
}
