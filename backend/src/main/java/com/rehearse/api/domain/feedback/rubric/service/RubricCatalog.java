package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricDimension;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;

import java.util.Map;
import java.util.Optional;

public interface RubricCatalog {

    Rubric resolveFor(Question question, QuestionSet questionSet, Interview interview);

    RubricDimension getDimension(String ref);

    Map<String, RubricDimension> getAllDimensions();

    Optional<String> findRefByName(String koreanLabel);
}
