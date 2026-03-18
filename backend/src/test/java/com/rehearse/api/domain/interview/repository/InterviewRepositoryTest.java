package com.rehearse.api.domain.interview.repository;

import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.global.config.JpaAuditingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class InterviewRepositoryTest {

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("EntityGraph로 ElementCollection이 세션 종료 후에도 접근 가능하다")
    void findByIdWithElementCollections() {
        // given
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.JAVA_SPRING))
                .csSubTopics(List.of("자료구조", "운영체제"))
                .durationMinutes(30)
                .build();

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        // when
        Interview found = interviewRepository.findByIdWithElementCollections(interview.getId()).orElseThrow();
        entityManager.clear();

        // then
        assertThat(found.getInterviewTypes()).containsExactlyInAnyOrder(
                InterviewType.CS_FUNDAMENTAL, InterviewType.JAVA_SPRING);
        assertThat(found.getCsSubTopics()).containsExactlyInAnyOrder("자료구조", "운영체제");
        assertThat(found.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
    }

    @Test
    @DisplayName("InterviewResponse DTO 변환 + JSON 직렬화 시 LazyInitializationException이 발생하지 않는다")
    void interviewResponse_noLazyInitializationException() {
        // given
        Interview interview = Interview.builder()
                .position(Position.FRONTEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.REACT_COMPONENT))
                .csSubTopics(List.of())
                .durationMinutes(20)
                .build();

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        // when
        Interview found = interviewRepository.findByIdWithElementCollections(interview.getId()).orElseThrow();
        entityManager.clear();

        // then
        assertThatNoException().isThrownBy(() -> {
            InterviewResponse response = InterviewResponse.from(found);
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            String json = mapper.writeValueAsString(response);
            assertThat(json).contains("REACT_COMPONENT");
            assertThat(json).contains("PENDING");
        });
    }
}
