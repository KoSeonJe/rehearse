package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuestionSetIdOrderByOrderIndex(Long questionSetId);
}
