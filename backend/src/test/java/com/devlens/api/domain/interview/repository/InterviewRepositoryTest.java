package com.devlens.api.domain.interview.repository;

import com.devlens.api.domain.interview.dto.InterviewResponse;
import com.devlens.api.domain.interview.entity.*;
import com.devlens.api.global.config.JpaAuditingConfig;
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
    @DisplayName("fetch join + EntityGraph 2단계 조회로 모든 컬렉션이 세션 종료 후에도 접근 가능하다")
    void findByIdWithQuestions_andElementCollections() {
        // given
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.JAVA_SPRING))
                .csSubTopics(List.of("자료구조", "운영체제"))
                .durationMinutes(30)
                .build();

        InterviewQuestion question = InterviewQuestion.builder()
                .questionOrder(1)
                .category("자료구조")
                .content("HashMap과 TreeMap의 차이점은?")
                .evaluationCriteria("시간 복잡도 이해")
                .build();
        interview.addQuestion(question);

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        // when — 1단계: questions fetch join, 2단계: element collections EntityGraph
        Interview found = interviewRepository.findByIdWithQuestions(interview.getId()).orElseThrow();
        interviewRepository.findByIdWithElementCollections(interview.getId());

        // 세션을 강제로 닫아 LazyInitializationException 가능성 테스트
        entityManager.clear();

        // then — 세션 없이도 모든 컬렉션 접근 가능
        assertThat(found.getInterviewTypes()).containsExactlyInAnyOrder(
                InterviewType.CS_FUNDAMENTAL, InterviewType.JAVA_SPRING);
        assertThat(found.getCsSubTopics()).containsExactlyInAnyOrder("자료구조", "운영체제");
        assertThat(found.getQuestions()).hasSize(1);
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

        InterviewQuestion question = InterviewQuestion.builder()
                .questionOrder(1)
                .category("React")
                .content("useEffect의 cleanup 함수는 언제 호출되나요?")
                .build();
        interview.addQuestion(question);

        interviewRepository.save(interview);
        entityManager.flush();
        entityManager.clear();

        // when — 2단계 조회
        Interview found = interviewRepository.findByIdWithQuestions(interview.getId()).orElseThrow();
        interviewRepository.findByIdWithElementCollections(interview.getId());
        entityManager.clear();

        // then — DTO 변환 + JSON 직렬화까지 예외 없이 성공
        assertThatNoException().isThrownBy(() -> {
            InterviewResponse response = InterviewResponse.from(found);
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            String json = mapper.writeValueAsString(response);
            assertThat(json).contains("REACT_COMPONENT");
            assertThat(json).contains("useEffect");
        });
    }
}
