package com.rehearse.api.domain.questionpool.repository;

import com.rehearse.api.domain.questionpool.entity.PreparedFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreparedFollowUpRepository extends JpaRepository<PreparedFollowUp, Long> {

    List<PreparedFollowUp> findByQuestionPoolIdOrderByDisplayOrderAsc(Long questionPoolId);
}
