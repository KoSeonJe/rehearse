package com.rehearse.api.domain.servicefeedback.repository;

import com.rehearse.api.domain.servicefeedback.entity.ServiceFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceFeedbackRepository extends JpaRepository<ServiceFeedback, Long> {

    Optional<ServiceFeedback> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    Page<ServiceFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
