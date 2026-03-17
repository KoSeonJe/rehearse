package com.rehearse.api.domain.interview.repository;

import com.rehearse.api.domain.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query("SELECT i FROM Interview i JOIN FETCH i.questions WHERE i.id = :id")
    Optional<Interview> findByIdWithQuestions(@Param("id") Long id);
}
